package io.github.whileaway;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class CanonicalGameTests {
    private static Entity entity(GameTestHelper h) {
        var e=WhileAway.WAYFARER.get().create(h.getLevel());var p=h.absolutePos(new BlockPos(1,2,1));
        h.setBlock(new BlockPos(1,1,1),Blocks.STONE);h.setBlock(new BlockPos(1,2,1),Blocks.AIR);h.setBlock(new BlockPos(1,3,1),Blocks.AIR);h.setBlock(new BlockPos(1,4,1),Blocks.AIR);
        e.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);return e;
    }
    private static StoryActorRecord record(Entity a) {
        var r=new StoryActorRecord("candidate:"+UUID.randomUUID(),"candidate_event","pursuer","whileaway:wayfarer","minecraft:overworld",UUID.randomUUID(),null,a.getUUID(),a.blockPosition(),true);
        StoryActors.tag(a,r);StoryActors.capture(a,r);return r;
    }
    @GameTest(template="empty") public static void candidateMultiplicityNeverChoosesFirst(GameTestHelper h) {
        var a=entity(h);var r=record(a);h.getLevel().addFreshEntity(a);
        h.assertTrue(ActorCandidates.inspect(h.getLevel().getServer(),r).outcome()==ActorCandidates.Outcome.EXACT_SINGLE_CANDIDATE,"single live identity");
        var b=entity(h);StoryActors.tag(b,r);h.getLevel().addFreshEntity(b);
        h.assertTrue(ActorCandidates.inspect(h.getLevel().getServer(),r).outcome()==ActorCandidates.Outcome.AMBIGUOUS_MULTIPLE_CANDIDATES,"two candidates never distance/UUID sorted");
        a.discard();b.discard();h.succeed();
    }
    @GameTest(template="empty") public static void candidateStaleConflictAndUnloadedAreDistinct(GameTestHelper h) {
        var a=entity(h);var r=record(a);h.getLevel().addFreshEntity(a);r.generation=1;
        h.assertTrue(ActorCandidates.inspect(h.getLevel().getServer(),r).outcome()==ActorCandidates.Outcome.STALE_GENERATION_ONLY,"stale not canonical");
        r.generation=0;a.getPersistentData().getCompound(StoryActors.TAG).putString("spawnRole","other");
        h.assertTrue(ActorCandidates.inspect(h.getLevel().getServer(),r).outcome()==ActorCandidates.Outcome.IDENTITY_CONFLICT,"role conflict");
        a.discard();r.position=new BlockPos(12000000,80,12000000);
        h.assertTrue(ActorCandidates.inspect(h.getLevel().getServer(),r).outcome()==ActorCandidates.Outcome.UNLOADED_OR_UNCONFIRMED,"unknown is not absence");h.succeed();
    }
    @GameTest(template="empty") public static void preparedReservationRetriesSameEpochAndUuid(GameTestHelper h) {
        var a=entity(h);var r=StoryActors.prepare(a,"reservation:"+UUID.randomUUID(),"candidate_event","pursuer",UUID.randomUUID(),null,true);
        UUID reserved=r.entityId;int epoch=r.generation;
        StoryActors.reconcile(h.getLevel().getServer(),r);var live=StoryActors.reconcile(h.getLevel().getServer(),r);
        h.assertTrue(live!=null&&r.entityId.equals(reserved)&&r.generation==epoch,"persisted reservation retried without new epoch");
        StoryActors.abort(h.getLevel().getServer(),r);h.succeed();
    }
    @GameTest(template="empty") public static void missingGenerationTagIsNotEpochZero(GameTestHelper h) {
        var a=entity(h);var r=record(a);a.getPersistentData().getCompound(StoryActors.TAG).remove("generation");
        h.assertTrue(!ActorCandidates.identity(a,r),"missing int tag is not generation zero");h.succeed();
    }
}
