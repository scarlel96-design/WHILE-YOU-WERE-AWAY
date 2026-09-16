package io.github.whileaway;

import io.github.whileaway.core.EventCheckpoint;
import io.github.whileaway.entity.Wayfarer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Bounded world-local actor reconciliation. Never loads a chunk or edits a block to make an actor fit. */
public final class StoryActors {
    public static final String TAG="whileaway_actor";
    public static final String CHASE="wayfarer_first";
    private static final Map<MinecraftServer,Map<String,Integer>> readyPasses=new WeakHashMap<>();
    public static String chaseId(UUID owner){return CHASE+":"+owner;}
    public static void persist(MinecraftServer server){var d=NarrativeData.get(server);d.setDirty();server.overworld().getDataStorage().save();}
    public static StoryActorRecord prepare(Entity actor,String id,String event,String role,UUID instance,UUID owner,boolean temporary) {
        var level=(ServerLevel)actor.level();var d=NarrativeData.get(level.getServer());
        var previous=d.actors.get(id);if(previous!=null)return previous;
        if(d.actors.size()>=256)throw new IllegalStateException("MANUAL_DIAGNOSTIC Actor capacity reached");
        var r=new StoryActorRecord(id,event,role,BuiltInRegistries.ENTITY_TYPE.getKey(actor.getType()).toString(),
            level.dimension().location().toString(),instance,owner,actor.getUUID(),actor.blockPosition(),temporary);
        d.actors.put(id,r);tag(actor,r);capture(actor,r);persist(level.getServer());emit(level.getServer(),r,"PREPARED");return r;
    }
    public static boolean beginChase(ServerPlayer player,Wayfarer actor) {
        var d=NarrativeData.get(player.getServer());String id=chaseId(player.getUUID());
        if(d.actors.containsKey(id)||d.entry(player.getUUID()).progress.encounterComplete())return false;
        // The reservation and encounter ticket share one durable record BEFORE any world entity exists.
        d.entry(player.getUUID()).encounter=actor.getUUID();
        var r=prepare(actor,id,CHASE,"pursuer",UUID.randomUUID(),player.getUUID(),true);
        return spawnPrepared(player.serverLevel(),r,actor);
    }
    private static void emit(MinecraftServer server,StoryActorRecord r,String boundary) {
        var p=r.owner==null?null:server.getPlayerList().getPlayer(r.owner);
        if(p!=null)RecoveryBoundaryEvent.emit(p,r.eventId,boundary,r.checkpoint.checkpoint);
    }
    public static void tag(Entity actor,StoryActorRecord r) {
        var t=new CompoundTag();t.putString("storyId",r.storyId);t.putString("ownerEventId",r.eventId);
        t.putString("spawnRole",r.role);t.putUUID("instanceId",r.instanceId);t.putInt("generation",r.generation);
        t.putString("lifecycleState",r.lifecycle.name());actor.getPersistentData().put(TAG,t);
    }
    public static boolean canonical(Entity actor,StoryActorRecord r) {
        var t=actor.getPersistentData().getCompound(TAG);
        return actor.getUUID().equals(r.entityId)&&t.hasUUID("instanceId")&&t.getUUID("instanceId").equals(r.instanceId)
            &&t.getInt("generation")==r.generation&&t.getString("storyId").equals(r.storyId)
            &&t.getString("ownerEventId").equals(r.eventId)&&t.getString("spawnRole").equals(r.role)
            &&BuiltInRegistries.ENTITY_TYPE.getKey(actor.getType()).toString().equals(r.entityType);
    }
    public static void capture(Entity actor,StoryActorRecord r) {
        r.position=actor.blockPosition();r.snapshot=actor.saveWithoutId(new CompoundTag());
        // Runtime IDs, passengers and leash references are not a durable actor identity.
        r.snapshot.remove("UUID");r.snapshot.remove("Passengers");r.snapshot.remove("Leash");
    }
    public static boolean spawnPrepared(ServerLevel level,StoryActorRecord r,Entity actor) {
        if(r.lifecycle==StoryActorRecord.Lifecycle.RETIRED||!canonical(actor,r))return false;
        if(!level.addFreshEntity(actor))return false;
        r.lifecycle=StoryActorRecord.Lifecycle.ACTIVE;tag(actor,r);capture(actor,r);
        if(r.checkpoint.checkpoint==0)r.checkpoint.advance(1);
        persist(level.getServer());emit(level.getServer(),r,"FACT_COMMITTED");return true;
    }
    /** False means unknown/unloaded, not proof of absence. Two separate loaded passes fence join queues. */
    public static boolean storageReady(ServerLevel level,StoryActorRecord r) {
        return level.hasChunkAt(r.position)&&level.areEntitiesLoaded(new ChunkPos(r.position).toLong());
    }
    public static Entity find(MinecraftServer server,UUID id) {
        for(var level:server.getAllLevels()){var entity=level.getEntity(id);if(entity!=null)return entity;}return null;
    }
    /** Presence is a context gate, not an encounter outcome. */
    public static boolean ownerPresent(MinecraftServer server,StoryActorRecord r) {
        if(r.owner==null)return true;
        var p=server.getPlayerList().getPlayer(r.owner);
        if(p==null||!p.isAlive()||!p.level().dimension().location().toString().equals(r.dimension))return false;
        var anchor=r.snapshot.contains("Home")?net.minecraft.core.BlockPos.of(r.snapshot.getLong("Home")):r.position;
        return !r.eventId.equals(CHASE)||p.blockPosition().distSqr(anchor)<=42*42;
    }
    public static Entity reconcile(MinecraftServer server,StoryActorRecord r) {
        var current=find(server,r.entityId);
        if(r.eventId.equals(CHASE)&&r.owner!=null&&r.lifecycle!=StoryActorRecord.Lifecycle.RETIRED) {
            var e=NarrativeData.get(server).entry(r.owner);
            // Contradictory permanent facts require diagnosis, not an endless spawn/discard loop.
            if(e.progress.encounterComplete()||!r.entityId.equals(e.encounter))return null;
        }
        if(r.lifecycle==StoryActorRecord.Lifecycle.RETIRED) {
            if(current!=null&&canonical(current,r))current.discard();
            if(r.eventId.equals(CHASE)&&r.checkpoint.checkpoint==3&&server.getPlayerList().getPlayer(r.owner)!=null)confirmChase(server,r);
            return null;
        }
        if(current!=null) {
            if(!canonical(current,r)||!current.level().dimension().location().toString().equals(r.dimension))return null;
            readyPasses.computeIfAbsent(server,s->new HashMap<>()).remove(r.storyId);
            if(!ownerPresent(server,r)){suspend(current,r,"owner_absent");return current;}
            if(r.lifecycle==StoryActorRecord.Lifecycle.PREPARED) {
                r.lifecycle=StoryActorRecord.Lifecycle.ACTIVE;tag(current,r);
                if(r.checkpoint.checkpoint==0)r.checkpoint.advance(1);
                persist(server);emit(server,r,"FACT_COMMITTED");
            }
            return current;
        }
        var passes=readyPasses.computeIfAbsent(server,s->new HashMap<>());
        if(!ownerPresent(server,r)) {
            passes.remove(r.storyId);
            if(r.lifecycle!=StoryActorRecord.Lifecycle.SUSPENDED) {
                r.lifecycle=StoryActorRecord.Lifecycle.SUSPENDED;r.checkpoint.suspend("owner_absent");persist(server);
            }return null;
        }
        var level=server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(r.dimension)));
        if(level==null||!storageReady(level,r)){passes.remove(r.storyId);return null;}
        if(passes.merge(r.storyId,1,Integer::sum)<2)return null;
        // Only known registry types; unsupported types remain diagnosable, never substitute a different NPC.
        var type=BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(r.entityType)).orElse(null);
        if(type==null)return null;
        Entity actor=type.create(level);if(actor==null)return null;
        actor.load(r.snapshot.copy());
        boolean clear=false;
        for(int[] off:new int[][]{{0,0},{1,0},{-1,0},{0,1},{0,-1},{2,0},{-2,0},{0,2},{0,-2}}) {
            var p=r.position.offset(off[0],0,off[1]);
            if(!level.hasChunkAt(p)||!level.getFluidState(p).isEmpty()||!level.getBlockState(p.below()).isCollisionShapeFullBlock(level,p.below()))continue;
            actor.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,actor.getYRot(),actor.getXRot());
            if(level.noCollision(actor)&&level.isUnobstructed(actor)){clear=true;break;}
        }
        if(!clear)return null;
        if(r.generation==Integer.MAX_VALUE)throw new IllegalStateException("MANUAL_DIAGNOSTIC Actor generation exhausted");
        var previous=r.lifecycle;r.generation++;r.entityId=UUID.randomUUID();actor.setUUID(r.entityId);
        r.lifecycle=StoryActorRecord.Lifecycle.PREPARED;tag(actor,r);capture(actor,r);
        if(r.eventId.equals(CHASE)&&r.owner!=null)NarrativeData.get(server).entry(r.owner).encounter=r.entityId;
        persist(server); // Fence a late entity from an OLD, unloaded chunk before introducing the replacement.
        if(!spawnPrepared(level,r,actor))return null;
        passes.remove(r.storyId);
        com.mojang.logging.LogUtils.getLogger().info("EVENT_RECOVERY eventId={} previousState={} restoredCheckpoint={} reason=missing_loaded_actor entitiesRemoved=0 entitiesRecreated=1 generation={}",r.eventId,previous,r.checkpoint.checkpoint,r.generation);
        return actor;
    }
    /** Legacy actor adoption happens in its tick, not while a chunk is joining. */
    public static StoryActorRecord attach(Wayfarer actor) {
        var level=(ServerLevel)actor.level();var server=level.getServer();var d=NarrativeData.get(server);
        var r=d.actors.get(chaseId(actor.witness()));
        if(r==null) {
            var e=d.entry(actor.witness());if(e.progress.encounterComplete()||!actor.getUUID().equals(e.encounter))return null;
            var instance=UUID.nameUUIDFromBytes(("whileaway:legacy:"+chaseId(actor.witness())).getBytes(StandardCharsets.UTF_8));
            r=prepare(actor,chaseId(actor.witness()),CHASE,"pursuer",instance,actor.witness(),true);
            r.lifecycle=StoryActorRecord.Lifecycle.ACTIVE;r.checkpoint.advance(1);capture(actor,r);persist(server);
        }
        return canonical(actor,r)&&r.lifecycle!=StoryActorRecord.Lifecycle.RETIRED?r:null;
    }
    public static void checkpoint(Entity actor,StoryActorRecord r,int phase,String fact) {
        if(r.lifecycle==StoryActorRecord.Lifecycle.RETIRED||!canonical(actor,r))return;
        if(fact!=null)r.facts.add(fact);
        if(phase==r.checkpoint.checkpoint&&r.checkpoint.state==EventCheckpoint.State.SUSPENDED) {
            r.checkpoint.state=switch(phase){case 0->EventCheckpoint.State.PREPARING;case 1->EventCheckpoint.State.PHASE_1;case 2->EventCheckpoint.State.PHASE_2;default->EventCheckpoint.State.RESOLVING;};
        }
        if(phase==r.checkpoint.checkpoint+1)r.checkpoint.advance(phase);
        capture(actor,r);persist(((ServerLevel)actor.level()).getServer());
        emit(((ServerLevel)actor.level()).getServer(),r,"CHECKPOINT_COMMITTED");
    }
    public static void suspend(Entity actor,StoryActorRecord r,String reason) {
        if(r.lifecycle==StoryActorRecord.Lifecycle.SUSPENDED)return;
        r.lifecycle=StoryActorRecord.Lifecycle.SUSPENDED;r.checkpoint.suspend(reason);capture(actor,r);persist(((ServerLevel)actor.level()).getServer());
    }
    public static void completeChase(Wayfarer actor) {
        var server=((ServerLevel)actor.level()).getServer();var r=attach(actor);
        if(r==null){actor.discard();return;}
        if(r.checkpoint.terminal())return;
        var e=NarrativeData.get(server).entry(r.owner);
        // Completion facts, retirement and old ticket removal are one atomic story-file write.
        r.facts.add("encounter_complete");r.lifecycle=StoryActorRecord.Lifecycle.RETIRED;
        while(r.checkpoint.checkpoint<3)r.checkpoint.advance(r.checkpoint.checkpoint+1);
        e.progress=e.progress.complete();e.encounter=null;persist(server);emit(server,r,"BEFORE_COMPLETE");
        confirmChase(server,r);actor.discard();
    }
    private static void confirmChase(MinecraftServer server,StoryActorRecord r) {
        if(r.checkpoint.checkpoint!=3||!r.facts.contains("encounter_complete"))return;
        var e=NarrativeData.get(server).entry(r.owner);e.progress=e.progress.complete();e.encounter=null;
        r.checkpoint.advance(4);boolean notice=!r.noticeClaimed;r.noticeClaimed=true;persist(server);emit(server,r,"AFTER_COMPLETE");
        var p=server.getPlayerList().getPlayer(r.owner);
        if(notice&&p!=null){p.displayClientMessage(net.minecraft.network.chat.Component.translatable("story.whileaway.after"),false);
            if(StoryConfig.MUSIC.get())StoryEvents.music(p,WhileAway.RESOLVE.get(),.45f);}
    }
    public static void abort(MinecraftServer server,StoryActorRecord r) {
        r.lifecycle=StoryActorRecord.Lifecycle.RETIRED;r.checkpoint.state=EventCheckpoint.State.ABORTED;r.checkpoint.reason="operator_recovery";
        if(r.owner!=null)NarrativeData.get(server).entry(r.owner).encounter=null;
        persist(server);var actor=find(server,r.entityId);if(actor!=null&&canonical(actor,r))actor.discard();
    }
    @SubscribeEvent public void joining(EntityJoinLevelEvent event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var actor=event.getEntity();if(!actor.getPersistentData().contains(TAG))return;
        var r=NarrativeData.get(level.getServer()).actors.get(actor.getPersistentData().getCompound(TAG).getString("storyId"));
        // Pure metadata admission: no chunk loading, spawning or saving from a join callback.
        if(r!=null&&(r.lifecycle==StoryActorRecord.Lifecycle.RETIRED||!canonical(actor,r)))event.setCanceled(true);
    }
    @SubscribeEvent public void tick(ServerTickEvent.Post event) {
        var server=event.getServer();if(server.getTickCount()%20!=0)return;
        for(var r:NarrativeData.get(server).actors.values())reconcile(server,r);
    }
    @SubscribeEvent public void stopped(ServerStoppedEvent event){readyPasses.remove(event.getServer());}
    public static List<String> integrity(MinecraftServer server) {
        var result=new ArrayList<String>();var ids=new HashSet<UUID>();var d=NarrativeData.get(server);
        for(var r:d.actors.values()) {
            if(!ids.add(r.entityId))result.add("MANUAL_DIAGNOSTIC duplicate canonical UUID "+r.storyId);
            var actor=find(server,r.entityId);
            if(actor!=null&&!canonical(actor,r))result.add("MANUAL_DIAGNOSTIC actor ownership mismatch "+r.storyId);
            if(actor!=null&&!actor.level().dimension().location().toString().equals(r.dimension))result.add("MANUAL_DIAGNOSTIC actor dimension mismatch "+r.storyId);
            if(r.lifecycle==StoryActorRecord.Lifecycle.RETIRED&&actor!=null)result.add("SAFE_AUTO_RECOVER retired actor present "+r.storyId);
            if(r.eventId.equals(CHASE)&&r.owner!=null) {
                var e=d.entry(r.owner);
                if(r.lifecycle!=StoryActorRecord.Lifecycle.RETIRED&&!r.entityId.equals(e.encounter))result.add("MANUAL_DIAGNOSTIC chase ticket mismatch "+r.storyId);
                if(r.checkpoint.state==EventCheckpoint.State.COMPLETED&&!e.progress.encounterComplete())result.add("MANUAL_DIAGNOSTIC completion fact missing "+r.storyId);
            }
            if(actor==null&&r.lifecycle!=StoryActorRecord.Lifecycle.RETIRED)result.add("RECOVER_WITH_WARNING actor absent or unloaded "+r.storyId);
        }return result;
    }
}
