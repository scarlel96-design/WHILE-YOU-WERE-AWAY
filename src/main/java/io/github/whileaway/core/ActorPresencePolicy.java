package io.github.whileaway.core;

/** Side-effect-free actor context gate. A missing owner pauses rather than completes an event. */
public final class ActorPresencePolicy {
    private ActorPresencePolicy() {}

    public static boolean canRun(boolean ownerRequired, boolean online, boolean alive,
            boolean sameDimension, boolean rangeLimited, double distanceSquared, double radius) {
        if (!ownerRequired) return true;
        if (!online || !alive || !sameDimension) return false;
        if (!rangeLimited) return true;
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0) return false;
        if (!Double.isFinite(radius) || radius < 0) return false;
        // Validate finite inputs before squaring: a large finite radius can overflow to infinity.
        return distanceSquared <= radius * radius;
    }
}
