package io.github.whileaway;

import java.util.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

/** Read-only production selectors and ownership policy, not live dialogue or navigation. */
@GameTestHolder(WhileAway.ID)
@PrefixGameTestTemplate(false)
public final class KnowledgeAndResidentGameTests {
    private static StoryActorRecord record() {
        return ReturnNetworkFixtures.resident(new NarrativeData());
    }
    @GameTest(template="empty") public static void resolutionRequiresKnowledgeAndUnclaimedNotice(GameTestHelper h) {
        var r=record();r.facts.add("encounter_complete");var before=r.save();
        h.assertTrue(!ChaseKnowledge.shouldPresentResolution(true,true,r),"completion alone never authorizes notice/music");
        h.assertTrue(r.save().equals(before),"eligibility never adds observation");
        r.facts.add(StoryActors.CHASE_OBSERVED);
        h.assertTrue(ChaseKnowledge.shouldPresentResolution(true,true,r),"observed encounter allows the existing presentation");
        h.assertTrue(!ChaseKnowledge.shouldPresentResolution(false,true,r),"claimed notice stays claimed");
        h.assertTrue(!ChaseKnowledge.shouldPresentResolution(true,false,r),"absent player receives no presentation");h.succeed();
    }
    @GameTest(template="empty") public static void observedFactRemainsSingleAfterRoundTrip(GameTestHelper h) {
        var r=record();r.facts.add(StoryActors.CHASE_OBSERVED);var before=r.save();
        h.assertTrue(!r.facts.add(StoryActors.CHASE_OBSERVED),"existing set prevents duplicate knowledge");
        var restored=StoryActorRecord.load(r.save());
        h.assertTrue(restored.save().equals(before)&&ChaseKnowledge.shouldPresentResolution(true,true,restored),"fact persists independently of presentation");h.succeed();
    }
    @GameTest(template="empty") public static void stationRevisitOldMissingAndRepeatedSelectionIsReadOnly(GameTestHelper h) {
        var r=record();r.facts.add("encounter_complete");var old=StoryActorRecord.load(r.save());var before=old.save();
        String neutral="story.whileaway.station_revisit_unseen";
        h.assertTrue(ChaseKnowledge.stationRevisitKey(null).equals(neutral),"absent actor has neutral text");
        for(int visit=0;visit<3;visit++)h.assertTrue(ChaseKnowledge.stationRevisitKey(old).equals(neutral),"old save never infers knowledge");
        h.assertTrue(before.equals(old.save()),"revisits preserve every saved field");
        old.facts.add(StoryActors.CHASE_OBSERVED);before=old.save();
        h.assertTrue(ChaseKnowledge.stationRevisitKey(old).equals("story.whileaway.station_revisit"),"observation alone selects observed text");
        h.assertTrue(before.equals(old.save()),"observed selection also remains read-only");h.succeed();
    }
    @GameTest(template="empty") public static void completedNetworkDelegatesExceptExistingAftermathLook(GameTestHelper h) {
        h.assertTrue(ReturnNetworkNpcPolicy.owner(ReturnNetworkState.Stage.NOT_STARTED,0)==ReturnNetworkNpcPolicy.Owner.RESIDENT,"unstarted adapter delegates");
        h.assertTrue(ReturnNetworkNpcPolicy.owner(ReturnNetworkState.Stage.COMPLETED,30)==ReturnNetworkNpcPolicy.Owner.RESIDENT,"resident route resumes after glance");
        h.assertTrue(ReturnNetworkNpcPolicy.owner(ReturnNetworkState.Stage.COMPLETED,199)==ReturnNetworkNpcPolicy.Owner.RESIDENT,"resident keeps normal route");
        h.assertTrue(ReturnNetworkNpcPolicy.owner(ReturnNetworkState.Stage.COMPLETED,200)==ReturnNetworkNpcPolicy.Owner.AFTERMATH_LOOK,"existing periodic look remains unchanged");
        h.assertTrue(!ReturnNetworkNpcPolicy.eventInteractionAllowed(ReturnNetworkState.Stage.COMPLETED),"completed adapter releases interaction");h.succeed();
    }
    @GameTest(template="empty") public static void completedDialogueAndRoutineDoNotChangeFacts(GameTestHelper h) {
        var r=record();var before=r.save();var home=NpcState.home(r);
        h.assertTrue(ReturnNetworkNpcPolicy.residentDialogue(ReturnNetworkState.Stage.COMPLETED,false,true).endsWith("ordinary_work"),"completed network permits ordinary work dialogue");
        h.assertTrue(ReturnNetworkNpcPolicy.residentDialogue(ReturnNetworkState.Stage.COMPLETED,false,false).endsWith("ordinary_rest"),"completed network permits rest dialogue");
        h.assertTrue(ReturnNetworkNpcPolicy.residentDialogue(ReturnNetworkState.Stage.COMPLETED,true,true).endsWith("aftermath"),"crouching retains context dialogue");
        NpcEvents.residenceDestination(home,6000);NpcEvents.residenceDestination(home,8000);NpcEvents.residenceDestination(home,12000);
        h.assertTrue(before.equals(r.save()),"dialogue selection and resident destinations never create facts or replace identity");h.succeed();
    }
    @GameTest(template="empty") public static void activeNetworkOwnsAdapterWithoutChangingParticipant(GameTestHelper h) {
        var r=record();var state=ReturnNetworkState.notStarted().discover(new ReturnNetworkState.Prerequisite(4,true,true,true),UUID.randomUUID(),
            new ReturnNetworkState.Location(ReturnNetworkEvents.LOCATION,r.dimension,ReturnNetworkEvents.RELAY.asLong()))
            .join(new ReturnNetworkState.Participant(r.storyId,r.instanceId,r.entityId,r.generation));
        var before=state.save();var actorBefore=r.save();
        for(var stage:ReturnNetworkState.Stage.values())if(stage!=ReturnNetworkState.Stage.NOT_STARTED&&stage!=ReturnNetworkState.Stage.COMPLETED) {
            h.assertTrue(ReturnNetworkNpcPolicy.owner(stage,100)==ReturnNetworkNpcPolicy.Owner.EVENT,"active adapter retains navigation/wait responsibility");
            h.assertTrue(ReturnNetworkNpcPolicy.residentDialogue(stage,true,true).endsWith("ordinary_work"),"ordinary selector does not fabricate completion");
        }
        NpcEvents.residenceDestination(NpcState.home(r),6000);
        h.assertTrue(state.save().equals(before)&&r.save().equals(actorBefore),"ownership/destination reads preserve participant and facts");h.succeed();
    }
}
