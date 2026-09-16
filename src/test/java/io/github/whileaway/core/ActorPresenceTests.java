package io.github.whileaway.core;

/** Independent policy oracle used unchanged by both local coding candidates. */
public final class ActorPresenceTests {
    private static int checks;
    private static void check(boolean expected, boolean actual, String context) {
        checks++;
        if (expected != actual) throw new AssertionError("ACTOR_PRESENCE_MISMATCH: " + context);
    }
    public static void main(String[] args) {
        double[] distances = {0, 1, 1763, 1764, Math.nextUp(1764d), 1765,
            -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (int flags = 0; flags < 32; flags++) {
            boolean required = (flags & 1) != 0, online = (flags & 2) != 0;
            boolean alive = (flags & 4) != 0, sameDimension = (flags & 8) != 0;
            boolean limited = (flags & 16) != 0;
            for (int d = 0; d < distances.length; d++) {
                boolean expected = !required || (online && alive && sameDimension && (!limited || d < 4));
                check(expected, ActorPresencePolicy.canRun(required, online, alive, sameDimension,
                    limited, distances[d], 42), "flags=" + flags + " distance=" + distances[d]);
            }
        }
        double[] invalidRadii = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double radius : invalidRadii) {
            check(false, ActorPresencePolicy.canRun(true,true,true,true,true,0,radius), "invalid radius=" + radius);
            check(true, ActorPresencePolicy.canRun(true,true,true,true,false,Double.NaN,radius), "unbounded ignores radius");
        }
        check(true, ActorPresencePolicy.canRun(true,true,true,true,true,0,0), "zero radius origin");
        check(false, ActorPresencePolicy.canRun(true,true,true,true,true,1,0), "zero radius outside");
        check(false, ActorPresencePolicy.canRun(true,true,true,true,true,Double.POSITIVE_INFINITY,Double.MAX_VALUE), "overflow must not admit infinity");
        check(true, ActorPresencePolicy.canRun(true,true,true,true,true,Double.MAX_VALUE,Double.MAX_VALUE), "finite enormous radius");
        check(true, ActorPresencePolicy.canRun(true,true,true,true,true,4,2), "non-default radius boundary");
        check(false, ActorPresencePolicy.canRun(true,true,true,true,true,Math.nextUp(4d),2), "non-default just outside");
        System.out.println("PASS actor presence policy checks=" + checks);
    }
}
