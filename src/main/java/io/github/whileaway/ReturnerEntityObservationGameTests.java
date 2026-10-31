package io.github.whileaway;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

/** Headless entity integration with Minecraft's supported mock ServerPlayer API. */
@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class ReturnerEntityObservationGameTests {
    @GameTest(template="empty",batch="support_observation_entity",timeoutTicks=60)
    public static void creativeMockNeverGrantsObservation(GameTestHelper h) {
        var level=h.getLevel();var server=level.getServer();var data=NarrativeData.get(server);
        // This version's supported mock overrides isCreative() to true, regardless of game mode.
        var player=h.makeMockServerPlayerInLevel();player.setNoGravity(true);
        var start=h.absolutePos(new BlockPos(2,3,1));player.setPos(start.getX()+.5,start.getY(),start.getZ()+.5);
        player.setYRot(0);player.setXRot(0);
        var entry=data.entry(player.getUUID());entry.openingStage=OpeningStage.COMPLETED;
        var actor=WhileAway.WAYFARER.get().create(level);actor.setNoAi(true);actor.setNoGravity(true);
        var pos=start.south(7);actor.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        actor.bind(player.getUUID(),start.south(20));entry.encounter=actor.getUUID();
        var record=StoryActors.prepare(actor,StoryActors.chaseId(player.getUUID()),StoryActors.CHASE,"pursuer",UUID.randomUUID(),player.getUUID(),true);
        h.assertTrue(StoryActors.spawnPrepared(level,record,actor),"production entity spawned through the existing reservation");
        h.runAtTickTime(12,()->{
            try {
                h.assertTrue(player.isCreative()&&actor.tickCount>=8,"supported creative mock and actual entity ticks exercised");
                h.assertTrue(!record.facts.contains(StoryActors.CHASE_OBSERVED),"creative demonstration never grants encounter knowledge");
                h.assertTrue(!record.facts.contains("encounter_complete"),"creative presence does not terminate the encounter");
                var restored=StoryActorRecord.load(record.save());
                h.assertTrue(!restored.facts.contains(StoryActors.CHASE_OBSERVED)&&restored.entityId.equals(actor.getUUID()),"NBT round trip preserves absence and binding");
                var before=record.save();
                h.assertTrue(!ChaseKnowledge.shouldPresentResolution(true,true,record)&&record.save().equals(before),"unobserved eligibility remains read-only");
                h.succeed();
            } finally {
                StoryActors.abort(server,record);data.actors.remove(record.storyId);StoryActors.persist(server);
                server.getPlayerList().remove(player);
            }
        });
    }
}
