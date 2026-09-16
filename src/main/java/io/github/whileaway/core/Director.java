package io.github.whileaway.core;

/** Pure decision engine. The server adapter owns visibility, spawning and persistence. */
public final class Director {
    private Director() {}
    public enum Cue { NONE, DISTANT_KNOCK, RETURNING_STEPS, MANIFEST }
    public record Context(long tick, boolean inStation, boolean active, boolean eligible,
                          boolean reading, boolean hostileNearby, boolean encounterAlive) {}
    public static Cue choose(Progress p, Context c) {
        if (!c.inStation || !c.active || !c.eligible || c.reading || c.hostileNearby
                || c.encounterAlive || p.encounterComplete() || c.tick < p.nextEventTick()) return Cue.NONE;
        return switch (p.stage()) {
            case 1 -> p.eventsPlayed() < 1 ? Cue.DISTANT_KNOCK : Cue.NONE;
            case 2 -> p.eventsPlayed() < 2 ? Cue.RETURNING_STEPS : Cue.NONE;
            case 3 -> Cue.MANIFEST;
            default -> Cue.NONE;
        };
    }
    public static boolean canRestore(Progress p) { return p.has(Clue.STATION) && p.has(Clue.WARNING_READ); }
}
