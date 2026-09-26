package io.github.whileaway;

import java.util.*;
import io.github.whileaway.support.ReturnNetworkPlayback;
import net.minecraft.core.particles.ParticleTypes;
import io.github.whileaway.content.RelayBlock;
import io.github.whileaway.entity.StoryNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** First compound campaign adapter. Reuses the revision-2 tram, never rebuilds it.
 * World writes require an explicit player interaction. Missing/changed equipment waits,
 * including the crash gap between the durable intervention and its physical application. */
public final class ReturnNetworkEvents {
    public static final BlockPos RELAY=new BlockPos(42,68,46), STAND=new BlockPos(44,65,47);
    public static final String LOCATION="quiet_city:tram_relay";
    private static final Map<ServerLevel,Runtime> RUNTIME=new WeakHashMap<>();
    private static final class Runtime { ReturnNetworkPlayback playback; UUID observer; boolean prompted; ReturnNetworkState.Wait wait=ReturnNetworkState.Wait.NONE; }
    private static Runtime runtime(ServerLevel l){return RUNTIME.computeIfAbsent(l,k->new Runtime());}
    private static void say(ServerPlayer p,String key){p.displayClientMessage(Component.translatable("story.whileaway.network."+key),false);}
    public static boolean originalEquipment(ServerLevel l){return l.getBlockState(RELAY).is(Blocks.OXIDIZED_CUT_COPPER);}
    public static boolean activatedEquipment(ServerLevel l){var b=l.getBlockState(RELAY);return b.is(WhileAway.RELAY.get())&&b.getValue(RelayBlock.LIT);}
    public static boolean environmentReady(ServerLevel l,ReturnNetworkState s) {
        if(!l.hasChunkAt(RELAY)||!l.hasChunkAt(STAND))return false;
        if(!l.getBlockState(STAND).isAir()||!l.getBlockState(STAND.above()).isAir()
            ||!l.getFluidState(STAND).isEmpty()||!l.getBlockState(STAND.below()).isCollisionShapeFullBlock(l,STAND.below()))return false;
        return s.stage.ordinal()<4?originalEquipment(l):activatedEquipment(l);
    }
    public static ReturnNetworkState.Wait waitState(ServerLevel l){return runtime(l).wait;}
    private static boolean campaignLocation(ServerLevel l) {
        return l.dimension().equals(CityDistrict.KEY)&&CityDistrict.get(l).ready()&&CityDistrict.get(l).layoutVersion()==2;
    }
    private static boolean exactLocation(ReturnNetworkState s,ServerLevel l) {
        return s.location!=null&&LOCATION.equals(s.location.id())&&s.location.position()==RELAY.asLong()
            &&s.location.dimension().equals(l.dimension().location().toString());
    }
    private static boolean commit(ServerPlayer p,ReturnNetworkState next,String boundary) {
        var server=p.getServer();if(!StoryStorage.available(server))return false;
        try {
            var d=NarrativeData.get(server);var result=d.commitReturnNetwork(d.returnNetwork(),next,server.registryAccess());
            if(result.wrote())RecoveryBoundaryEvent.emit(p,ReturnNetworkState.ID,boundary,next.stage.ordinal());
            return true;
        }catch(RuntimeException e){
            StoryStorage.block(server,"RN_COMMIT_FAILED "+e.getMessage());
            com.mojang.logging.LogUtils.getLogger().error("RN_COMMIT_FAILED event={} boundary={}",ReturnNetworkState.ID,boundary,e);
            return false;
        }
    }
    @SubscribeEvent public void tick(ServerTickEvent.Post e) {
        var server=e.getServer();if(!StoryStorage.available(server))return;
        var l=server.getLevel(CityDistrict.KEY);if(l==null||!campaignLocation(l))return;
        var d=NarrativeData.get(server);var s=d.returnNetwork();
        signalTick(l,s);
        if(server.getTickCount()%20!=0)return;
        if(s.stage!=ReturnNetworkState.Stage.NOT_STARTED||!ReturnNetworkIntegrity.prerequisite(d.actors.get(NpcEvents.STORY)))return;
        if(!environmentReady(l,s))return;
        var p=l.players().stream().filter(v->v.isAlive()&&!v.isSpectator()&&v.blockPosition().distSqr(RELAY)<=36).findFirst().orElse(null);
        if(p==null)return;
        var next=s.discover(new ReturnNetworkState.Prerequisite(4,true,true,true),UUID.randomUUID(),
            new ReturnNetworkState.Location(LOCATION,l.dimension().location().toString(),RELAY.asLong()));
        if(commit(p,next,"RN_A_DISCOVERED"))say(p,"discovered");
    }
    /** Returns true only when this adapter owns navigation. Existing resident identity is untouched. */
    public static boolean update(StoryNpc npc,StoryActorRecord r) {
        var l=(ServerLevel)npc.level();var d=NarrativeData.get(l.getServer());var s=d.returnNetwork();
        if(s.stage==ReturnNetworkState.Stage.NOT_STARTED)return false;
        if(!exactLocation(s,l)){StoryStorage.block(l.getServer(),"RN_LOCATION_IDENTITY_MISMATCH");npc.getNavigation().stop();return true;}
        var issues=ReturnNetworkIntegrity.issues(s,d.actors);
        if(!issues.isEmpty()){StoryStorage.block(l.getServer(),String.join(";",issues));npc.getNavigation().stop();return true;}
        if(s.stage==ReturnNetworkState.Stage.COMPLETED) {
            // A small new resident habit, not a replay of the event or a new work identity.
            if(l.getGameTime()%200<30){npc.getLookControl().setLookAt(RELAY.getX()+.5,RELAY.getY()+.5,RELAY.getZ()+.5);return true;}
            return false;
        }
        var rt=runtime(l);var p=NpcEvents.witness(l,RELAY);
        if(p==null){rt.wait=ReturnNetworkState.Wait.PLAYER_ABSENT;rt.playback=null;npc.storyNavigation().externalPause(npc);return true;}
        if(!environmentReady(l,s)){rt.wait=ReturnNetworkState.Wait.WAITING_ENVIRONMENT;rt.playback=null;npc.storyNavigation().externalPause(npc);return true;}
        var path=npc.storyNavigation().step(npc,STAND);
        if(path!=NpcEnvironment.Path.ARRIVED){rt.wait=path==NpcEnvironment.Path.AVAILABLE?ReturnNetworkState.Wait.WAITING_NPC:ReturnNetworkState.Wait.WAITING_ENVIRONMENT;rt.playback=null;return true;}
        rt.wait=ReturnNetworkState.Wait.NONE;
        npc.getLookControl().setLookAt(RELAY.getX()+.5,RELAY.getY()+.5,RELAY.getZ()+.5);
        if(s.stage==ReturnNetworkState.Stage.DISCOVERED) {
            if(commit(p,s.join(new ReturnNetworkState.Participant(r.storyId,r.instanceId,r.entityId,r.generation)),"RN_B_NPC_JOINED"))say(p,"joined");
        }else if(s.stage==ReturnNetworkState.Stage.INTERVENTION_COMMITTED) {
            if(s.presentation==ReturnNetworkState.Presentation.NONE){commit(p,s.beginPresentation(),"RN_PRESENTING");rt.playback=null;return true;}
            // A full response presentation remains transient; only an explicit equipment interaction confirms observation.
            if(rt.playback==null)startSignal(l,p,ReturnNetworkPlayback.Purpose.RESPONSE);
        }else if(s.stage==ReturnNetworkState.Stage.RESPONSE_OBSERVED&&s.facts.contains(ReturnNetworkState.Fact.SHARED_EXPERIENCE)) {
            RecoveryBoundaryEvent.emit(p,ReturnNetworkState.ID,"RN_G_BEFORE_COMPLETE",5);
            if(commit(p,s.complete(),"RN_G_COMPLETED"))say(p,"complete");
        }
        return true;
    }
    public static boolean interactNpc(StoryNpc npc,ServerPlayer p) {
        if(!p.isAlive()||p.isSpectator()||npc.level()!=p.level()||npc.distanceToSqr(p)>36||!StoryStorage.available(p.getServer()))return false;
        var d=NarrativeData.get(p.getServer());var s=d.returnNetwork();var r=d.actors.get(NpcEvents.STORY);
        if(r==null||!StoryActors.canonical(npc,r)||!exactLocation(s,p.serverLevel()))return false;
        if(s.stage==ReturnNetworkState.Stage.COMPLETED){say(p,"aftermath");return true;}
        if(s.stage!=ReturnNetworkState.Stage.RESPONSE_OBSERVED||!environmentReady(p.serverLevel(),s)
            ||npc.distanceToSqr(STAND.getX()+.5,STAND.getY(),STAND.getZ()+.5)>1.4)return false;
        if(!s.facts.contains(ReturnNetworkState.Fact.SHARED_EXPERIENCE)&&commit(p,s.shareExperience(),"RN_F_SHARED"))say(p,"shared");
        return true;
    }
    /** Public interaction seam used by the block event and scripted clients, never by timers. */
    public static boolean interactEquipment(ServerPlayer p,BlockPos pos) {
        var l=p.serverLevel();if(!pos.equals(RELAY)||!campaignLocation(l)||!p.isAlive()||p.isSpectator()
            ||p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>25||!StoryStorage.available(p.getServer()))return false;
        var d=NarrativeData.get(p.getServer());var s=d.returnNetwork();
        if(!exactLocation(s,l))return false;
        if(s.stage.ordinal()>=4 && originalEquipment(l) && p.isShiftKeyDown()) {
            // A committed operation can be explicitly reapplied even while the NPC route is blocked.
            l.setBlock(RELAY,WhileAway.RELAY.get().defaultBlockState().setValue(RelayBlock.LIT,true),3);
            return true;
        }
        if(s.stage.ordinal()>=5) {
            say(p,activatedEquipment(l)?"evidence":"waiting_environment");return true;
        }
        var r=d.actors.get(NpcEvents.STORY);var e=r==null?null:l.getEntity(r.entityId);
        if(!(e instanceof StoryNpc npc)||!StoryActors.canonical(npc,r)||npc.distanceToSqr(STAND.getX()+.5,STAND.getY(),STAND.getZ()+.5)>1.4){say(p,"waiting_npc");return true;}
        if(s.stage==ReturnNetworkState.Stage.NPC_JOINED&&environmentReady(l,s)) {
            var rt=runtime(l);
            if(confirmedSignal(l,p,ReturnNetworkPlayback.Purpose.OLD_PATTERN)) {
                if(commit(p,s.observePattern(),"RN_C_PATTERN")){rt.playback=null;say(p,"pattern");}
            }else if(rt.playback==null)startSignal(l,p,ReturnNetworkPlayback.Purpose.OLD_PATTERN);
            return true;
        }
        if(s.stage==ReturnNetworkState.Stage.INTERVENTION_COMMITTED && s.presentation==ReturnNetworkState.Presentation.PRESENTING
            && confirmedSignal(l,p,ReturnNetworkPlayback.Purpose.RESPONSE)) {
            if(commit(p,s.observeResponse(),"RN_E_RESPONSE")){runtime(l).playback=null;say(p,"response");}
            return true;
        }
        if(s.stage==ReturnNetworkState.Stage.SIGNAL_OBSERVED) {
            if(!environmentReady(l,s)){say(p,"waiting_environment");return true;}
            if(!p.isShiftKeyDown()){say(p,"interact_hint");return true;}
            if(!commit(p,s.intervene(),"RN_D_INTERVENTION"))return true;
            s=d.returnNetwork();
        }
        if(s.stage.ordinal()>=4&&originalEquipment(l)&&p.isShiftKeyDown()) {
            // Explicit, repeatable physical application after durable intent. Never replace an unknown block.
            l.setBlock(RELAY,WhileAway.RELAY.get().defaultBlockState().setValue(RelayBlock.LIT,true),3);
        }
        if(s.stage.ordinal()>=4&&!activatedEquipment(l)){say(p,"waiting_environment");return true;}
        if(s.stage.ordinal()>=5)say(p,"evidence");
        return true;
    }
    /** Runtime diagnostic only: no snapshot/save API, no persistent signal tick. */
    public static ReturnNetworkPlayback signal(ServerLevel l){return runtime(l).playback;}
    private static void startSignal(ServerLevel l,ServerPlayer p,ReturnNetworkPlayback.Purpose purpose) {
        var rt=runtime(l);rt.prompted=false;rt.observer=p.getUUID();rt.playback=new ReturnNetworkPlayback(purpose,l.getGameTime());
        say(p,"listening");
        RecoveryBoundaryEvent.emit(p,ReturnNetworkState.ID,"RN_SIGNAL_PREPARED_"+purpose, NarrativeData.get(l.getServer()).returnNetwork().stage.ordinal());
    }
    private static boolean signalReady(ServerLevel l,ReturnNetworkState s,ServerPlayer p) {
        if(p==null||p.serverLevel()!=l||!p.isAlive()||p.isSpectator()||p.distanceToSqr(RELAY.getX()+.5,RELAY.getY()+.5,RELAY.getZ()+.5)>36
            ||!environmentReady(l,s))return false;
        var r=NarrativeData.get(l.getServer()).actors.get(NpcEvents.STORY);
        var e=r==null?null:l.getEntity(r.entityId);
        return e instanceof StoryNpc npc&&StoryActors.canonical(npc,r)&&npc.distanceToSqr(STAND.getX()+.5,STAND.getY(),STAND.getZ()+.5)<=1.4;
    }
    private static boolean confirmedSignal(ServerLevel l,ServerPlayer p,ReturnNetworkPlayback.Purpose purpose) {
        var rt=runtime(l);
        return rt.playback!=null&&rt.playback.purpose()==purpose&&rt.playback.complete()&&p.getUUID().equals(rt.observer)
            &&signalReady(l,NarrativeData.get(l.getServer()).returnNetwork(),p);
    }
    private static void signalTick(ServerLevel l,ReturnNetworkState s) {
        var rt=runtime(l);var play=rt.playback;if(play==null)return;
        var p=l.getServer().getPlayerList().getPlayer(rt.observer);
        boolean stage=play.purpose()==ReturnNetworkPlayback.Purpose.OLD_PATTERN?s.stage==ReturnNetworkState.Stage.NPC_JOINED
            :s.stage==ReturnNetworkState.Stage.INTERVENTION_COMMITTED&&s.presentation==ReturnNetworkState.Presentation.PRESENTING;
        var frame=play.tick(l.getGameTime(),stage&&signalReady(l,s,p));
        if(play.interrupted()){rt.playback=null;return;}
        if(frame.pulse()>=0) {
            // Spark lifetime in Minecraft1.21.1 is 2-3 ticks, below the 16-tick gap. No block state changes.
            boolean sent=l.sendParticles(p,ParticleTypes.ELECTRIC_SPARK,true,RELAY.getX()+.5,RELAY.getY()+1.15,RELAY.getZ()+.5,24,.28,.12,.28,0);
            if(!sent){play.carrierFailed();rt.playback=null;return;}
            // Quiet repeated carrier notes sustain a pulse for its full short/long duration.
            if((l.getGameTime()-play.origin())%4==0)l.playSound(null,RELAY,SoundEvents.NOTE_BLOCK_BELL.value(),SoundSource.BLOCKS,.12f,1.5f);
        }
        if(frame.onset())RecoveryBoundaryEvent.emit(p,ReturnNetworkState.ID,"RN_SIGNAL_ON_"+play.purpose()+"_"+frame.pulse(),s.stage.ordinal());
        if(frame.offset())RecoveryBoundaryEvent.emit(p,ReturnNetworkState.ID,"RN_SIGNAL_OFF_"+play.purpose()+"_"+play.emitted(),s.stage.ordinal());
        if(frame.complete()&&!rt.prompted){rt.prompted=true;say(p,"signal_ready");}
        // No fact commit here. Emission/technical delivery != player observation. Confirmation is an interaction.
    }
    @SubscribeEvent public void blockUse(PlayerInteractEvent.RightClickBlock e) {
        if(e.getHand()!=InteractionHand.MAIN_HAND||!e.getPos().equals(RELAY)||!e.getLevel().dimension().equals(CityDistrict.KEY))return;
        if(e.getEntity() instanceof ServerPlayer p&&interactEquipment(p,e.getPos())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}
    }
    public static List<String> integrity(net.minecraft.server.MinecraftServer server) {
        if(!StoryStorage.available(server))return List.of("RN_WRITE_GUARD_ACTIVE");
        var d=NarrativeData.get(server);var s=d.returnNetwork();var out=new ArrayList<>(ReturnNetworkIntegrity.issues(s,d.actors));
        if(s.stage==ReturnNetworkState.Stage.NOT_STARTED)return out;
        var l=server.getLevel(CityDistrict.KEY);
        if(l==null||!exactLocation(s,l))out.add("RN_LOCATION_IDENTITY_MISMATCH");
        else if(!l.hasChunkAt(RELAY)||!l.hasChunkAt(STAND))out.add("RN_ENVIRONMENT_UNLOADED");
        else if(!environmentReady(l,s))out.add("RN_STRUCTURE_CONTRADICTION checkpoint="+s.stage.ordinal());
        return out;
    }
}
