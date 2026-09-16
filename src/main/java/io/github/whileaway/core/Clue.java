package io.github.whileaway.core;

public enum Clue {
    STATION, WARNING_READ, EVACUATION_READ, PERSONAL_READ, SIGNAL_RESTORED;
    public int bit() { return 1 << ordinal(); }
}
