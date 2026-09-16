package io.github.whileaway;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

/** Read-only evidence. A UUID binding is not identity and a snapshot is not an identity source. */
public final class ActorCandidates {
    public enum Outcome { NO_CANDIDATE, EXACT_SINGLE_CANDIDATE, AMBIGUOUS_MULTIPLE_CANDIDATES,
        STALE_GENERATION_ONLY, UNLOADED_OR_UNCONFIRMED, IDENTITY_CONFLICT }
    public record Result(Outcome outcome,Entity exact,int matches,int stale,String detail) {}
    private ActorCandidates() {}
    public static boolean identity(Entity entity,StoryActorRecord r) {
        var t=entity.getPersistentData().getCompound(StoryActors.TAG);
        return t.getString("storyId").equals(r.storyId)&&t.getString("ownerEventId").equals(r.eventId)
            &&t.getString("spawnRole").equals(r.role)&&t.hasUUID("instanceId")&&t.getUUID("instanceId").equals(r.instanceId)
            &&t.contains("generation",Tag.TAG_INT)&&t.getInt("generation")==r.generation;
    }
    public static boolean ready(ServerLevel level,StoryActorRecord r) {
        var pos=r.snapshot.getList("Pos",Tag.TAG_DOUBLE);
        if(pos.size()!=3)return false;
        for(int i=0;i<3;i++)if(!Double.isFinite(pos.getDouble(i)))return false;
        var snapshotPos=BlockPos.containing(pos.getDouble(0),pos.getDouble(1),pos.getDouble(2));
        return level.hasChunkAt(r.position)&&level.areEntitiesLoaded(new ChunkPos(r.position).toLong())
            &&level.hasChunkAt(snapshotPos)&&level.areEntitiesLoaded(new ChunkPos(snapshotPos).toLong());
    }
    public static Result inspect(MinecraftServer server,StoryActorRecord r) {
        int matches=0,stale=0;boolean conflict=false;Entity exact=null;ServerLevel bound=null;
        // Loaded entities only: never load chunks, query world files or choose the nearest candidate.
        for(var level:server.getAllLevels()) {
            if(level.dimension().location().toString().equals(r.dimension))bound=level;
            for(var entity:level.getAllEntities()) {
                var t=entity.getPersistentData().getCompound(StoryActors.TAG);
                if(!r.storyId.equals(t.getString("storyId")))continue;
                boolean family=t.getString("ownerEventId").equals(r.eventId)&&t.getString("spawnRole").equals(r.role)
                    &&t.hasUUID("instanceId")&&t.getUUID("instanceId").equals(r.instanceId)&&t.contains("generation",Tag.TAG_INT);
                if(!family){conflict=true;continue;}
                if(t.getInt("generation")<r.generation){stale++;continue;}
                if(!identity(entity,r)){conflict=true;continue;}
                matches++;exact=entity;
                if(!BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(r.entityType)
                    ||!level.dimension().location().toString().equals(r.dimension)
                    ||!Set.of("PREPARED","ACTIVE","SUSPENDED").contains(t.getString("lifecycleState"))
                    ||r.lifecycle==StoryActorRecord.Lifecycle.RETIRED)conflict=true;
                if(entity instanceof io.github.whileaway.entity.Wayfarer w&&!Objects.equals(w.witness(),r.owner))conflict=true;
            }
        }
        Outcome outcome=matches>1?Outcome.AMBIGUOUS_MULTIPLE_CANDIDATES:conflict?Outcome.IDENTITY_CONFLICT:
            bound==null||!ready(bound,r)?Outcome.UNLOADED_OR_UNCONFIRMED:
            matches==1?Outcome.EXACT_SINGLE_CANDIDATE:stale>0?Outcome.STALE_GENERATION_ONLY:Outcome.NO_CANDIDATE;
        return new Result(outcome,outcome==Outcome.EXACT_SINGLE_CANDIDATE?exact:null,matches,stale,
            "storyId="+r.storyId+" eventId="+r.eventId+" instanceId="+r.instanceId+" generation="+r.generation+" candidates="+matches+" stale="+stale);
    }
}
