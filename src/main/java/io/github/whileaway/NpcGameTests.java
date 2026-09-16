package io.github.whileaway;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class NpcGameTests {
    private static StoryActorRecord record() {
        var r=new StoryActorRecord("npc-test:"+UUID.randomUUID(),NpcEvents.ID,"resident","whileaway:story_npc","minecraft:overworld",UUID.randomUUID(),null,UUID.randomUUID(),new BlockPos(0,2,0),false);
        NpcState.initialize(r,new BlockPos(4,2,0));return r;
    }
    @GameTest(template="empty") public static void npcFactsRoundTripIndependentCounterparts(GameTestHelper h) {
        var r=record();var player=UUID.randomUUID();r.checkpoint.advance(1);
        r.facts.add(NpcState.MET+player);r.facts.add(NpcState.DIALOGUE);r.checkpoint.advance(2);
        r.facts.add(NpcState.relationship("player:"+player,"first_conversation"));
        r.facts.add(NpcState.relationship("npc:doyun","shared_work"));
        var loaded=StoryActorRecord.load(r.save());
        h.assertTrue(loaded.facts.equals(r.facts)&&NpcState.integrity(loaded).isEmpty(),"facts survive without player-only relationship model");
        h.assertTrue(NpcState.home(loaded).equals(new BlockPos(4,2,0))&&loaded.owner==null&&!loaded.temporary,"residence independent of player lifetime");h.succeed();
    }
    @GameTest(template="empty") public static void npcConflictingResidenceAndPrematureLinkAreDetected(GameTestHelper h) {
        var r=record();r.facts.add("npc.home=123");
        h.assertTrue(!NpcState.integrity(r).isEmpty(),"two residences must not choose first");
        r=record();r.facts.add(NpcState.LINK);
        h.assertTrue(NpcState.integrity(r).stream().anyMatch(s->s.contains("NPC_PREMATURE_LIFE_COMMIT")),"link cannot predate completion");h.succeed();
    }
    @GameTest(template="empty") public static void npcInitializerFailureDoesNotRegisterPartialActor(GameTestHelper h) {
        var npc=WhileAway.STORY_NPC.get().create(h.getLevel());String id="npc-initializer:"+UUID.randomUUID();boolean rejected=false;
        try{StoryActors.prepare(npc,id,NpcEvents.ID,"resident",UUID.randomUUID(),null,false,r->{throw new IllegalArgumentException("bad facts");});}
        catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected&&!NarrativeData.get(h.getLevel().getServer()).actors.containsKey(id),"initializer must finish before durable reservation");h.succeed();
    }
    @GameTest(template="empty") public static void completedNpcRemainsCanonicalLivingEntity(GameTestHelper h) {
        h.setBlock(new BlockPos(1,1,1),Blocks.STONE);var pos=h.absolutePos(new BlockPos(1,2,1));
        var npc=WhileAway.STORY_NPC.get().create(h.getLevel());npc.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var r=StoryActors.prepare(npc,"npc-living:"+UUID.randomUUID(),NpcEvents.ID,"resident",UUID.randomUUID(),null,false,x->NpcState.initialize(x,pos));
        h.assertTrue(StoryActors.spawnPrepared(h.getLevel(),r,npc),"real custom NPC created");
        r.facts.add(NpcState.MET+UUID.randomUUID());r.facts.add(NpcState.DIALOGUE);r.checkpoint.advance(2);
        r.facts.add(NpcState.ARRIVED);r.facts.add(NpcState.OBSERVED);r.checkpoint.advance(3);NpcState.settle(r);r.facts.add(NpcState.LINK);r.checkpoint.advance(4);
        StoryActors.persist(h.getLevel().getServer());
        h.assertTrue(StoryActors.reconcile(h.getLevel().getServer(),r)==npc&&r.generation==0&&NpcState.integrity(r).isEmpty(),"completed event is not retired resident");
        StoryActors.abort(h.getLevel().getServer(),r);h.succeed();
    }
}
