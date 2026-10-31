package io.github.whileaway.support;

/** Current Returner exposure rules. Streak progress remains transient. */
public final class ReturnerObservationPolicy {
    private ReturnerObservationPolicy() {}

    public static boolean withinRange(double distanceSquared) {
        return distanceSquared <= 32 * 32 && distanceSquared > 0.01;
    }

    public static boolean facing(double lookDot) {
        return lookDot >= 0.35;
    }

    public static int nextStreak(int previous, boolean eligible) {
        return eligible ? previous + 1 : 0;
    }

    public static boolean confirmed(int streak) {
        return streak >= 8;
    }
}
