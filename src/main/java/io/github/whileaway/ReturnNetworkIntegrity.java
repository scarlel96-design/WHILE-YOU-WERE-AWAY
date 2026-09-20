package io.github.whileaway;

import java.util.*;
import io.github.whileaway.core.EventCheckpoint;

/** Persistent cross-system proof. Never uses position or a snapshot as identity. */
public final class ReturnNetworkIntegrity {
    public static final String SHARED="npc.shared=return_network_first_response";
    public static final String AFTERMATH="npc.routine=check_old_signal";
    private ReturnNetworkIntegrity() {}
    public static boolean prerequisite(StoryActorRecord r) {
        return r!=null && NpcEvents.STORY.equals(r.storyId) && NpcEvents.ID.equals(r.eventId)
            && r.entityType.equals("whileaway:story_npc") && r.role.equals("resident")
            && r.lifecycle!=StoryActorRecord.Lifecycle.RETIRED && r.checkpoint.checkpoint==4
            && r.checkpoint.state==EventCheckpoint.State.COMPLETED && NpcState.integrity(r).isEmpty()
            && r.facts.stream().anyMatch(f->f.startsWith("npc.relationship=") && f.endsWith("/shared_return_light"));
    }
    public static List<String> issues(ReturnNetworkState s,Map<String,StoryActorRecord> actors) {
        var issues=new ArrayList<String>();var r=actors.get(NpcEvents.STORY);
        if(s.stage==ReturnNetworkState.Stage.NOT_STARTED) {
            if(r!=null&&(r.facts.contains(SHARED)||r.facts.contains(AFTERMATH)))issues.add("RN_PREMATURE_NPC_FACT");
            return issues;
        }
        if(!prerequisite(r)){issues.add("RN_PREREQUISITE_MISSING");return issues;}
        if(!r.dimension.equals(s.location.dimension()))issues.add("RN_PARTICIPANT_DIMENSION");
        if(s.participant!=null) {
            var p=s.participant;
            if(!p.storyId().equals(r.storyId)||!p.instance().equals(r.instanceId)||!p.entity().equals(r.entityId)||p.generation()!=r.generation)
                issues.add("RN_PARTICIPANT_IDENTITY_MISMATCH");
        }
        if(r.facts.contains(SHARED)!=s.facts.contains(ReturnNetworkState.Fact.SHARED_EXPERIENCE))issues.add("RN_SHARED_EXPERIENCE_MISMATCH");
        if(r.facts.contains(AFTERMATH)!=s.facts.contains(ReturnNetworkState.Fact.AFTERMATH))issues.add("RN_AFTERMATH_MISMATCH");
        return issues;
    }
    public static void requireValid(ReturnNetworkState s,Map<String,StoryActorRecord> actors) {
        var issues=issues(s,actors);if(!issues.isEmpty())throw new IllegalStateException(String.join(";",issues));
    }
}
