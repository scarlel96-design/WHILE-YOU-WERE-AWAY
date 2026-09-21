package io.github.whileaway.support;

import static io.github.whileaway.support.ReturnNetworkLoadAssessment.*;

/** Standalone pure-Java checks. These do not claim actual chunk unload or real-client evidence. */
public final class ReturnNetworkLoadTests {
    private static int assertions;

    private static Snapshot snapshot(
        boolean player, boolean dimension, boolean npcChunk, boolean npcStorage,
        boolean canonical, boolean relay, boolean environment, Path path
    ) {
        return new Snapshot(player, dimension, npcChunk, npcStorage, canonical, relay, environment, path);
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static Result expect(Desired desired, Snapshot snapshot, String label) {
        Result result = assess(snapshot);
        check(result.desired() == desired, label + " expected=" + desired + " actual=" + result.desired());
        check(!result.factMutationAuthorized(), label + " must never authorize durable mutation");
        check(result.diagnostic().contains("source=observation-only mutation=false"), label + " diagnostic provenance");
        return result;
    }

    private static void testAPlayerAbsent() {
        expect(Desired.PLAYER_ABSENT,
            snapshot(false, false, false, false, false, false, false, Path.UNREACHABLE),
            "A player absence");
        System.out.println("PASS A player absence precedes dimension, NPC, relay, environment and path");
    }

    private static void testBWrongDimension() {
        expect(Desired.WRONG_DIMENSION,
            snapshot(true, false, false, false, false, false, false, Path.UNREACHABLE),
            "B wrong dimension");
        System.out.println("PASS B wrong dimension precedes NPC and relay observations");
    }

    private static void testCNpcAndRelayReload() {
        expect(Desired.NPC_CHUNK_UNAVAILABLE,
            snapshot(true, true, false, false, false, false, false, Path.NOT_ASSESSED),
            "C NPC chunk first");
        expect(Desired.NPC_ENTITY_STORAGE_UNAVAILABLE,
            snapshot(true, true, true, false, false, false, false, Path.NOT_ASSESSED),
            "C entity storage second");
        expect(Desired.NPC_IDENTITY_UNCONFIRMED,
            snapshot(true, true, true, true, false, false, false, Path.NOT_ASSESSED),
            "C identity unconfirmed");
        expect(Desired.RELAY_UNAVAILABLE,
            snapshot(true, true, true, true, true, false, false, Path.NOT_ASSESSED),
            "C relay unavailable");
        System.out.println("PASS C NPC chunk, entity storage, canonical identity and relay reload remain distinct");
    }

    private static void testDEnvironmentBeforePath() {
        expect(Desired.ENVIRONMENT_UNAVAILABLE,
            snapshot(true, true, true, true, true, true, false, Path.UNREACHABLE),
            "D environment precedence");
        expect(Desired.PATH_NOT_ASSESSED,
            snapshot(true, true, true, true, true, true, true, Path.NOT_ASSESSED),
            "D path not assessed");
        expect(Desired.PATH_IN_PROGRESS,
            snapshot(true, true, true, true, true, true, true, Path.AVAILABLE),
            "D path in progress");
        expect(Desired.PATH_RETRYABLE,
            snapshot(true, true, true, true, true, true, true, Path.TEMPORARY_FAILURE),
            "D path retryable");
        expect(Desired.PATH_BLOCKED,
            snapshot(true, true, true, true, true, true, true, Path.DESTINATION_BLOCKED),
            "D path blocked");
        Result ready = expect(Desired.READY,
            snapshot(true, true, true, true, true, true, true, Path.ARRIVED),
            "D arrived");
        check(ready.ready(), "arrived observation is diagnostically ready");
        System.out.println("PASS D environment precedes path and ARRIVED is the only ready classification");
    }

    private static void testPlayerPrecedenceDuringReload() {
        expect(Desired.PLAYER_ABSENT,
            snapshot(false, true, false, false, false, false, false, Path.CHUNK_UNAVAILABLE),
            "player before NPC and relay reload");
        expect(Desired.NPC_CHUNK_UNAVAILABLE,
            snapshot(true, true, false, false, false, false, false, Path.CHUNK_UNAVAILABLE),
            "NPC after player resumes");
        System.out.println("PASS RELOAD player precedence is reevaluated before NPC and relay readiness");
    }

    private static void testEveryPathIsReadOnly() {
        for (Path path : Path.values()) {
            Result result = assess(snapshot(true, true, true, true, true, true, true, path));
            check(!result.factMutationAuthorized(), "path " + path + " authorized mutation");
        }
        System.out.println("PASS READ_ONLY every path classification denies fact mutation");
    }

    public static void main(String[] args) {
        testAPlayerAbsent();
        testBWrongDimension();
        testCNpcAndRelayReload();
        testDEnvironmentBeforePath();
        testPlayerPrecedenceDuringReload();
        testEveryPathIsReadOnly();
        System.out.println("PASS ReturnNetworkLoadTests assertions=" + assertions
            + " scope=pure-diagnostic no-world-side-effects no-runtime-wiring");
    }
}
