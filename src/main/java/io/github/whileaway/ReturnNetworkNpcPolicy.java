package io.github.whileaway;

/** Existing adapter precedence. These choices do not grant movement or story facts. */
final class ReturnNetworkNpcPolicy {
    enum Owner { RESIDENT, EVENT, AFTERMATH_LOOK }
    private ReturnNetworkNpcPolicy() {}

    static Owner owner(ReturnNetworkState.Stage stage, long gameTime) {
        if (stage == ReturnNetworkState.Stage.NOT_STARTED) return Owner.RESIDENT;
        if (stage == ReturnNetworkState.Stage.COMPLETED)
            return gameTime % 200 < 30 ? Owner.AFTERMATH_LOOK : Owner.RESIDENT;
        return Owner.EVENT;
    }

    static boolean eventInteractionAllowed(ReturnNetworkState.Stage stage) {
        return stage != ReturnNetworkState.Stage.COMPLETED;
    }

    static String residentDialogue(ReturnNetworkState.Stage stage, boolean crouching, boolean working) {
        if (stage == ReturnNetworkState.Stage.COMPLETED && crouching) return "story.whileaway.network.aftermath";
        return working ? "story.whileaway.yeoul.ordinary_work" : "story.whileaway.yeoul.ordinary_rest";
    }
}
