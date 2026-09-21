package io.github.whileaway.support;

import java.util.Objects;

/**
 * Pure, read-only description of the load observations needed by the Return Network adapter.
 *
 * <p>This class is deliberately not wired into the runtime. Its result is a diagnostic desired
 * state, not {@code ReturnNetworkState.Wait}, and it never authorizes a story fact, identity,
 * block, chunk, or entity mutation. In particular, a missing live NPC means that the production
 * NPC-owned updater is not running; this model can describe the desired wait without claiming
 * that the production runtime wait field was refreshed.</p>
 */
public final class ReturnNetworkLoadAssessment {
    public enum Path {
        NOT_ASSESSED,
        AVAILABLE,
        ARRIVED,
        TEMPORARY_FAILURE,
        UNREACHABLE,
        OUT_OF_RANGE,
        CHUNK_UNAVAILABLE,
        DESTINATION_BLOCKED,
        DESTINATION_INVALID,
        DESTINATION_MISSING
    }

    public enum Desired {
        PLAYER_ABSENT,
        WRONG_DIMENSION,
        NPC_CHUNK_UNAVAILABLE,
        NPC_ENTITY_STORAGE_UNAVAILABLE,
        NPC_IDENTITY_UNCONFIRMED,
        RELAY_UNAVAILABLE,
        ENVIRONMENT_UNAVAILABLE,
        PATH_NOT_ASSESSED,
        PATH_IN_PROGRESS,
        PATH_RETRYABLE,
        PATH_BLOCKED,
        READY
    }

    /** A caller-supplied observation only; construction and assessment have no world access. */
    public record Snapshot(
        boolean eligiblePlayer,
        boolean correctDimension,
        boolean npcChunkLoaded,
        boolean npcEntityStorageReady,
        boolean canonicalNpcKnown,
        boolean relayLoaded,
        boolean environmentReady,
        Path path
    ) {
        public Snapshot {
            Objects.requireNonNull(path, "path");
        }
    }

    public record Result(Desired desired, String diagnostic, boolean factMutationAuthorized) {
        public Result {
            Objects.requireNonNull(desired, "desired");
            Objects.requireNonNull(diagnostic, "diagnostic");
            if (factMutationAuthorized) {
                throw new IllegalArgumentException("load assessment must remain read-only");
            }
        }

        public boolean ready() {
            return desired == Desired.READY;
        }
    }

    private ReturnNetworkLoadAssessment() {}

    /**
     * Applies a derived, fail-closed readiness ordering for support diagnostics: player,
     * dimension, live NPC load and identity, relay/environment, then path. This is not the full
     * production updater's literal precedence: the updater exists only on a live NPC and does not
     * compute a missing-NPC or wrong-dimension wait in that absence. Once a live NPC is present,
     * environment still wins over path here, matching the ordering that updater actually uses.
     */
    public static Result assess(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Desired desired;
        if (!snapshot.eligiblePlayer()) desired = Desired.PLAYER_ABSENT;
        else if (!snapshot.correctDimension()) desired = Desired.WRONG_DIMENSION;
        else if (!snapshot.npcChunkLoaded()) desired = Desired.NPC_CHUNK_UNAVAILABLE;
        else if (!snapshot.npcEntityStorageReady()) desired = Desired.NPC_ENTITY_STORAGE_UNAVAILABLE;
        else if (!snapshot.canonicalNpcKnown()) desired = Desired.NPC_IDENTITY_UNCONFIRMED;
        else if (!snapshot.relayLoaded()) desired = Desired.RELAY_UNAVAILABLE;
        else if (!snapshot.environmentReady()) desired = Desired.ENVIRONMENT_UNAVAILABLE;
        else desired = classify(snapshot.path());
        return new Result(desired, diagnostic(desired, snapshot.path()), false);
    }

    private static Desired classify(Path path) {
        return switch (path) {
            case ARRIVED -> Desired.READY;
            case AVAILABLE -> Desired.PATH_IN_PROGRESS;
            case NOT_ASSESSED -> Desired.PATH_NOT_ASSESSED;
            case TEMPORARY_FAILURE -> Desired.PATH_RETRYABLE;
            case UNREACHABLE, OUT_OF_RANGE, CHUNK_UNAVAILABLE, DESTINATION_BLOCKED,
                DESTINATION_INVALID, DESTINATION_MISSING -> Desired.PATH_BLOCKED;
        };
    }

    private static String diagnostic(Desired desired, Path path) {
        return "RN_LOAD desired=" + desired + " path=" + path
            + " source=observation-only mutation=false";
    }
}
