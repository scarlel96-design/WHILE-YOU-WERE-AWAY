package io.github.whileaway.core;

/** Discovery, not elapsed time, unlocks contact. Count is validated by the ledger. */
public final class ThreatPolicy {
    private ThreatPolicy() {}
    public static boolean mayManifest(int distinctClues, boolean signalRestored) {
        return distinctClues >= 3 && signalRestored;
    }
}
