package io.github.whileaway.core;

public final class CityAccess {
    private CityAccess() {}
    public static boolean canEnter(Progress p) {
        return p.has(Clue.STATION) && p.has(Clue.SIGNAL_RESTORED)
            && p.has(Clue.PERSONAL_READ) && p.encounterComplete();
    }
    public static int read(int current, int bit) {
        return (current | (bit & 7)) & 7;
    }
}
