package io.github.whileaway;

/** Durable steps for the playable memory route. Presentation timing is transient. */
public enum OpeningStage {
    NOT_STARTED,
    STARTED,
    TRANSITION,
    OVERWORLD_COMMITTED,
    COMPLETED;

    public OpeningStage next() {
        return switch (this) {
            case NOT_STARTED -> STARTED;
            case STARTED -> TRANSITION;
            case TRANSITION -> OVERWORLD_COMMITTED;
            case OVERWORLD_COMMITTED -> COMPLETED;
            case COMPLETED -> throw new IllegalStateException("opening_already_completed");
        };
    }
}
