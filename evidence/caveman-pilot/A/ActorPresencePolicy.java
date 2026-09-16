package io.github.whileaway.core;

/** Pure context gate. Absence pauses an actor; it never decides an event outcome. */
public final class ActorPresencePolicy {
    private ActorPresencePolicy() {}

    public static boolean canRun(boolean ownerRequired, boolean online, boolean alive,
            boolean sameDimension, boolean rangeLimited, double distanceSquared, double radius) {
        if (!ownerRequired) return true;
        if (!online || !alive || !sameDimension) return false;
        if (!rangeLimited) return true;
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0
                || !Double.isFinite(radius) || radius < 0) return false;
        // An overflowing radius square admits all finite distances, never infinity or NaN.
        return distanceSquared <= radius * radius;
    }
}
