package io.github.whileaway.core;

/** Immutable, serializable story state. No dependency on renderer or Minecraft clock. */
public record Progress(int clues, long nextEventTick, int eventsPlayed, boolean encounterComplete) {
    private static final int VALID_MASK = (1 << Clue.values().length) - 1;
    public Progress {
        clues &= VALID_MASK;
        nextEventTick = Math.max(0, nextEventTick);
        eventsPlayed = Math.max(0, eventsPlayed);
    }
    public static Progress empty() { return new Progress(0, 0, 0, false); }
    public boolean has(Clue clue) { return (clues & clue.bit()) != 0; }
    public int distinctClues() { return Integer.bitCount(clues); }
    public Progress discover(Clue clue) {
        return new Progress(clues | clue.bit(), nextEventTick, eventsPlayed, encounterComplete);
    }
    public int stage() {
        if (!has(Clue.STATION)) return 0;
        if (ThreatPolicy.mayManifest(distinctClues(), has(Clue.SIGNAL_RESTORED))) return 3;
        return has(Clue.WARNING_READ) ? 2 : 1;
    }
    public Progress defer(long tick) {
        return new Progress(clues, Math.max(nextEventTick, tick), eventsPlayed, encounterComplete);
    }
    public Progress eventPlayed(long now, long cooldown) {
        return new Progress(clues, now + Math.max(1, cooldown), eventsPlayed + 1, encounterComplete);
    }
    public Progress complete() { return new Progress(clues, nextEventTick, eventsPlayed, true); }
}
