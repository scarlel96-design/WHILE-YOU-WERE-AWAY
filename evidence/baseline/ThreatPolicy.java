package io.github.whileaway.core;

/** Initial scaffold baseline: no encounter may manifest yet. */
public final class ThreatPolicy {
    private ThreatPolicy() {}
    public static boolean mayManifest(int distinctClues, boolean signalRestored) {
        return false;
    }
}
