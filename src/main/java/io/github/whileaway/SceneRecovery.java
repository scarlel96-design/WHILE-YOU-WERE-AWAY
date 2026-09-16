package io.github.whileaway;

import java.util.*;
import io.github.whileaway.core.EventCheckpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.logging.LogUtils;

/** All permanent records are world SavedData. Leases only identify the current connection's cosmetic playback. */
public final class SceneRecovery {
    private record Lease(UUID event,UUID token,long sentAt){}
    private static final Map<MinecraftServer,Map<UUID,Lease>> sessions=Collections.synchronizedMap(new WeakHashMap<>());
    private static Map<UUID,Lease> sessions(MinecraftServer s){return sessions.computeIfAbsent(s,k->new HashMap<>());}
    public static boolean begin(ServerPlayer p,int kind,BlockPos origin) {
        var d=NarrativeData.get(p.getServer());var e=d.entry(p.getUUID());
        if(e.scenes.containsKey(kind))return false;
        var r=new SceneRecord(kind,origin);e.scenes.put(kind,r);persist(p.getServer(),d);
        trace(p,r,"prepared");RecoveryBoundaryEvent.emit(p,r.eventId(),"PREPARED",0);return true;
    }
    public static void persist(MinecraftServer server,NarrativeData data) {
        data.setDirty();server.overworld().getDataStorage().save();
    }
    public static boolean busy(ServerPlayer p){return sessions(p.getServer()).containsKey(p.getUUID());}
    private static boolean inContext(ServerPlayer p,SceneRecord r) {
        return p.isAlive()&&!p.isSleeping()&&p.level().dimension().location().toString().equals(r.dimension())
            &&p.blockPosition().distSqr(r.origin)<=40*40&&p.serverLevel().hasChunkAt(r.origin);
    }
    public static void acknowledge(ServerPlayer p,SceneAck ack) {
        var lease=sessions(p.getServer()).get(p.getUUID());
        if(lease==null||!lease.event.equals(ack.eventId())||!lease.token.equals(ack.lease()))return;
        var d=NarrativeData.get(p.getServer());var e=d.entry(p.getUUID());
        var r=e.scenes.values().stream().filter(s->s.id.equals(ack.eventId())).findFirst().orElse(null);
        if(r==null||r.progress.terminal())return;
        if(ack.suspended()){suspend(p,"client_context_closed");return;}
        if(!inContext(p,r)) {suspend(p,"context_changed");return;}
        // The client can acknowledge only the same or next cosmetic checkpoint, never inventory or story choices.
        if(ack.checkpoint()<r.progress.checkpoint||ack.checkpoint()>r.progress.checkpoint+1||ack.checkpoint()>4)return;
        if(ack.checkpoint()==4)RecoveryBoundaryEvent.emit(p,r.eventId(),"BEFORE_COMPLETE",r.progress.checkpoint);
        boolean changed=false;
        int mask=ack.shown()&127;
        if((r.progress.shown|mask)!=r.progress.shown){r.progress.shown|=mask;changed=true;}
        if(r.progress.advance(ack.checkpoint())){changed=true;trace(p,r,"checkpoint_committed");}
        if(r.progress.terminal()) {
            if(r.kind==3)e.cityShockTriggered=true;
            sessions(p.getServer()).remove(p.getUUID());
        }
        if(changed) {
            persist(p.getServer(),d);
            RecoveryBoundaryEvent.emit(p,r.eventId(),r.progress.terminal()?"AFTER_COMPLETE":"FACT_COMMITTED",r.progress.checkpoint);
        }
    }
    public static void suspend(ServerPlayer p,String reason) {
        var lease=sessions(p.getServer()).remove(p.getUUID());if(lease==null)return;
        var d=NarrativeData.get(p.getServer());
        for(var r:d.entry(p.getUUID()).scenes.values())if(r.id.equals(lease.event)) {
            r.progress.suspend(reason);persist(p.getServer(),d);trace(p,r,"suspended");
        }
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if(event.getEntity() instanceof ServerPlayer p)suspend(p,"disconnect");
    }
    @SubscribeEvent public void stopped(ServerStoppedEvent event){sessions.remove(event.getServer());}
    @SubscribeEvent public void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer p)||p.tickCount%20!=0)return;
        if(!StoryStorage.available(p.getServer()))return;
        CityInvestigation.finish(p);
        var s=p.getServer();var d=NarrativeData.get(s);var e=d.entry(p.getUUID());var active=sessions(s).get(p.getUUID());
        if(active!=null) {
            var r=e.scenes.values().stream().filter(v->v.id.equals(active.event)).findFirst().orElse(null);
            if(r==null||!inContext(p,r)||s.overworld().getGameTime()-active.sentAt>600)suspend(p,"context_or_lease_expired");
            return;
        }
        // Fixed ordering makes recovery deterministic when more than one scene is pending.
        for(int kind=1;kind<=3;kind++) {
            var r=e.scenes.get(kind);if(r==null||r.progress.terminal()||!inContext(p,r))continue;
            if(kind==3&&(p.isCreative()||p.isSpectator()||p.getHealth()<8||!StoryConfig.ENCOUNTERS.get()
                ||p.serverLevel().getDifficulty()==net.minecraft.world.Difficulty.PEACEFUL||s.overworld().getGameTime()<e.readingUntil))continue;
            UUID token=UUID.randomUUID();sessions(s).put(p.getUUID(),new Lease(r.id,token,s.overworld().getGameTime()));
            var previous=r.progress.state;
            if(previous==EventCheckpoint.State.SUSPENDED||previous==EventCheckpoint.State.FAILED_RECOVERABLE)
                LogUtils.getLogger().debug("EVENT_RECOVERY eventId={} previousState={} restoredCheckpoint={} reason={} effects=cosmetic_reconstruction",r.eventId(),previous,r.progress.checkpoint,r.progress.reason);
            r.progress.state=EventCheckpoint.State.ACTIVE;persist(s,d);
            PacketDistributor.sendToPlayer(p,new ScenePayload(kind,r.origin,r.id,token,r.progress.checkpoint,r.progress.shown));
            trace(p,r,"dispatched");break;
        }
    }
    public static List<String> integrity(NarrativeData.Entry e) {
        var issues=new ArrayList<String>();var ids=new HashSet<UUID>();
        if(e.investigation.evidence()!=e.cityClues)issues.add("investigation_evidence_mismatch");
        if(e.investigation.progress.checkpoint==4&&e.investigation.evidence()!=7)issues.add("investigation_completed_without_evidence");
        e.scenes.forEach((kind,r)->{
            if(kind!=r.kind)issues.add("scene_key_mismatch:"+kind);
            if(!ids.add(r.id))issues.add("duplicate_scene_id:"+r.id);
            if(r.progress.checkpoint<0||r.progress.checkpoint>4)issues.add("invalid_checkpoint:"+kind);
            if(r.progress.state==EventCheckpoint.State.COMPLETED&&r.progress.checkpoint!=4)issues.add("completed_checkpoint_mismatch:"+kind);
            if(r.progress.state==EventCheckpoint.State.FAILED_RECOVERABLE)issues.add("recovered_invalid_state:"+kind+":"+r.progress.reason);
        });
        var seven=e.scenes.get(3);
        if(seven!=null&&(seven.progress.state==EventCheckpoint.State.COMPLETED)!=e.cityShockTriggered)issues.add("seven_completion_fact_mismatch");
        return issues;
    }
    private static void trace(ServerPlayer p,SceneRecord r,String action) {
        LogUtils.getLogger().info("WHILEAWAY_EVENT player={} event={} instance={} action={} state={} checkpoint={} reason={}",
            p.getUUID(),r.eventId(),r.id,action,r.progress.state,r.progress.checkpoint,r.progress.reason);
    }
}
