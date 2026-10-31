package io.github.whileaway;

/** Read-only presentation choices. Encounter completion does not establish observation. */
public final class ChaseKnowledge {
    private ChaseKnowledge() {}

    private static boolean observed(StoryActorRecord actor) {
        return actor != null && actor.facts.contains(StoryActors.CHASE_OBSERVED);
    }

    public static boolean shouldPresentResolution(boolean notice, boolean playerPresent, StoryActorRecord actor) {
        return notice && playerPresent && observed(actor);
    }

    public static String stationRevisitKey(StoryActorRecord actor) {
        return observed(actor) ? "story.whileaway.station_revisit" : "story.whileaway.station_revisit_unseen";
    }
}
