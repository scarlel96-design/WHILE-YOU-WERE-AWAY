package io.github.whileaway;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class NpcExceptionGameTests {
    @GameTest(template="empty") public static void absentWitnessDoesNotMaterializePreparedResident(GameTestHelper h) {
        var l=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));
        var npc=WhileAway.STORY_NPC.get().create(l);npc.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var r=StoryActors.prepare(npc,"npc-reserved-test:"+UUID.randomUUID(),NpcEvents.ID,"resident",UUID.randomUUID(),null,false,x->NpcState.initialize(x,pos));
        var uuid=r.entityId;
        h.assertTrue(!StoryActors.ownerPresent(l.getServer(),r),"unowned NPC is not proof of a present witness");
        for(int i=0;i<3;i++)StoryActors.reconcile(l.getServer(),r);
        h.assertTrue(r.checkpoint.checkpoint==0&&r.lifecycle==StoryActorRecord.Lifecycle.PREPARED&&r.generation==0&&r.entityId.equals(uuid)&&StoryActors.find(l.getServer(),uuid)==null,"keep initial reservation intact while absent");
        StoryActors.abort(l.getServer(),r);h.succeed();
    }
    @GameTest(template="empty") public static void activeResidentPausesWithoutAdvancingFacts(GameTestHelper h) {
        var l=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));
        var npc=WhileAway.STORY_NPC.get().create(l);npc.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        var r=StoryActors.prepare(npc,"npc-absent-test:"+UUID.randomUUID(),NpcEvents.ID,"resident",UUID.randomUUID(),null,false,x->NpcState.initialize(x,pos));
        h.assertTrue(StoryActors.spawnPrepared(l,r,npc),"fixture actual entity exists");
        var facts=java.util.Set.copyOf(r.facts);var uuid=r.entityId;
        StoryActors.reconcile(l.getServer(),r);
        h.assertTrue(r.lifecycle==StoryActorRecord.Lifecycle.SUSPENDED&&r.checkpoint.checkpoint==1&&r.generation==0&&r.entityId.equals(uuid)&&r.facts.equals(facts),"absence suspends without progression or ownership change");
        StoryActors.abort(l.getServer(),r);h.succeed();
    }
}
