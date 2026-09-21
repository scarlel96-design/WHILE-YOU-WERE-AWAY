package io.github.whileaway.support;

import io.github.whileaway.support.ReturnNetworkSignal.CarrierContract;
import io.github.whileaway.support.ReturnNetworkSignal.CompletedReplayPolicy;
import io.github.whileaway.support.ReturnNetworkSignal.EmittedPulse;
import io.github.whileaway.support.ReturnNetworkSignal.MatchResult;
import io.github.whileaway.support.ReturnNetworkSignal.ObservedPulse;
import io.github.whileaway.support.ReturnNetworkSignal.ObserverContract;
import io.github.whileaway.support.ReturnNetworkSignal.Pulse;
import io.github.whileaway.support.ReturnNetworkSignal.PulseClass;
import io.github.whileaway.support.ReturnNetworkSignal.SessionState;
import io.github.whileaway.support.ReturnNetworkSignal.SignalUse;
import io.github.whileaway.support.ReturnNetworkSignal.Timeline;
import io.github.whileaway.support.ReturnNetworkSignal.Timing;
import io.github.whileaway.support.ReturnNetworkSignal.TransientSession;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReturnNetworkSignalTests {
    private static final Timing TIMING = new Timing(3, 8, 2, 4);
    private static int passed;

    private ReturnNetworkSignalTests() {
    }

    public static void main(String[] args) {
        run("semantic order and immutability", ReturnNetworkSignalTests::semanticOrderAndImmutability);
        run("timing validation and exact gaps", ReturnNetworkSignalTests::timingValidationAndExactGaps);
        run("timeline rejects origin mismatch", ReturnNetworkSignalTests::timelineRejectsOriginMismatch);
        run("timeline rejects gap mismatch", ReturnNetworkSignalTests::timelineRejectsGapMismatch);
        run("timeline rejects SHORT LONG SHORT", ReturnNetworkSignalTests::timelineRejectsShortLongShort);
        run("deterministic planning", ReturnNetworkSignalTests::deterministicPlanning);
        run("overflow rejection", ReturnNetworkSignalTests::overflowRejection);
        run("exact match and observer separation", ReturnNetworkSignalTests::exactMatchAndObserverSeparation);
        run("wrong order rejected", ReturnNetworkSignalTests::wrongOrderRejected);
        run("no partial observation match", ReturnNetworkSignalTests::partialRejected);
        run("duplicate identity rejected", ReturnNetworkSignalTests::duplicateRejected);
        run("wrong timing and gap rejected", ReturnNetworkSignalTests::wrongTimingRejected);
        run("mismatched observer and session rejected", ReturnNetworkSignalTests::mismatchedObserverAndSessionRejected);
        run("poll and dispatch deduplicate", ReturnNetworkSignalTests::pollAndDispatchDeduplicate);
        run("late poll first deadline requires restart", ReturnNetworkSignalTests::skippedFirstDeadline);
        run("late poll second deadline requires restart", ReturnNetworkSignalTests::skippedSecondDeadline);
        run("late poll long deadline requires restart", ReturnNetworkSignalTests::skippedLongDeadline);
        run("suspend before first requires restart", ReturnNetworkSignalTests::suspendBeforeFirst);
        run("suspend between pulses requires restart", ReturnNetworkSignalTests::suspendBetweenPulses);
        run("suspend inside pulse requires restart", ReturnNetworkSignalTests::suspendInsidePulse);
        run("final pulse interruption requires restart", ReturnNetworkSignalTests::finalPulseInterruption);
        run("repeated suspend is explicit exception", ReturnNetworkSignalTests::repeatedSuspendPolicy);
        run("readiness loss requires restart", ReturnNetworkSignalTests::readinessLossRequiresRestart);
        run("restart entire plan and exact recovery match", ReturnNetworkSignalTests::restartEntirePlanAndMatch);
        run("restart overflow leaves old state consistent", ReturnNetworkSignalTests::restartOverflowIsAtomic);
        run("completed replay policy", ReturnNetworkSignalTests::completedReplayPolicy);
        run("transient boundary has no persistence API", ReturnNetworkSignalTests::transientBoundary);
        System.out.println("RESULT PASS " + passed + "/27");
    }

    private static void semanticOrderAndImmutability() {
        assertEquals(List.of(PulseClass.SHORT, PulseClass.SHORT, PulseClass.LONG),
                ReturnNetworkSignal.SEMANTIC_PATTERN, "semantic pattern");
        expectThrows(UnsupportedOperationException.class,
                () -> ReturnNetworkSignal.SEMANTIC_PATTERN.add(PulseClass.SHORT));
        Timeline plan = ReturnNetworkSignal.plan(10, TIMING);
        expectThrows(UnsupportedOperationException.class,
                () -> plan.pulses().add(new Pulse(3, PulseClass.LONG, 99, 8)));
    }

    private static void timingValidationAndExactGaps() {
        expectThrows(IllegalArgumentException.class, () -> new Timing(0, 8, 2, 4));
        expectThrows(IllegalArgumentException.class, () -> new Timing(3, 3, 2, 4));
        expectThrows(IllegalArgumentException.class, () -> new Timing(3, 8, -1, 4));
        Timeline plan = ReturnNetworkSignal.plan(10, TIMING);
        assertEquals(2L, plan.gapAfter(0), "first gap");
        assertEquals(4L, plan.gapAfter(1), "second gap");
        assertEquals(30L, plan.completesAtTick(), "completion tick");
    }

    private static void timelineRejectsOriginMismatch() {
        Timeline valid = ReturnNetworkSignal.plan(10, TIMING);
        expectThrows(IllegalArgumentException.class,
                () -> new Timeline(9, TIMING, valid.pulses(), valid.completesAtTick()));
    }

    private static void timelineRejectsGapMismatch() {
        List<Pulse> wrongGap = List.of(
                new Pulse(0, PulseClass.SHORT, 10, 3),
                new Pulse(1, PulseClass.SHORT, 16, 3),
                new Pulse(2, PulseClass.LONG, 23, 8));
        expectThrows(IllegalArgumentException.class,
                () -> new Timeline(10, TIMING, wrongGap, 31));
    }

    private static void timelineRejectsShortLongShort() {
        List<Pulse> wrongOrder = List.of(
                new Pulse(0, PulseClass.SHORT, 10, 3),
                new Pulse(1, PulseClass.LONG, 15, 8),
                new Pulse(2, PulseClass.SHORT, 27, 3));
        expectThrows(IllegalArgumentException.class,
                () -> new Timeline(10, TIMING, wrongOrder, 30));
    }

    private static void deterministicPlanning() {
        Timeline first = ReturnNetworkSignal.plan(73, TIMING);
        Timeline second = ReturnNetworkSignal.plan(73, TIMING);
        assertEquals(first, second, "same inputs must produce same value plan");
        assertEquals(List.of(73L, 78L, 85L),
                first.pulses().stream().map(Pulse::startTick).toList(), "starts");
        assertEquals(List.of(3L, 3L, 8L),
                first.pulses().stream().map(Pulse::durationTicks).toList(), "durations");
    }

    private static void overflowRejection() {
        expectThrows(ArithmeticException.class,
                () -> ReturnNetworkSignal.plan(Long.MAX_VALUE - 2, TIMING));
        expectThrows(ArithmeticException.class,
                () -> new Pulse(0, PulseClass.SHORT, Long.MAX_VALUE, 1));
    }

    private static void exactMatchAndObserverSeparation() {
        Timeline timeline = ReturnNetworkSignal.plan(10, TIMING);
        List<EmittedPulse> emitted = exactEmissions("s1", SignalUse.CORE, timeline);
        List<ObservedPulse> observed = exactObservations("probe-A", emitted);
        assertTrue(!EmittedPulse.class.equals(ObservedPulse.class), "emission and observation types differ");
        MatchResult result = ReturnNetworkSignal.matchExact(timeline, emitted, observed);
        assertTrue(result.exactMatch(), result.detail());
        assertEquals(CarrierContract.NON_STATE_MUTATING_TRANSIENT_CUE,
                emitted.get(0).carrierContract(), "carrier contract");
        assertEquals(ObserverContract.TECHNICAL_SAMPLE_ONLY,
                observed.get(0).observerContract(), "observer contract");
    }

    private static void wrongOrderRejected() {
        Timeline timeline = ReturnNetworkSignal.plan(10, TIMING);
        List<EmittedPulse> emitted = exactEmissions("s2", SignalUse.CORE, timeline);
        List<ObservedPulse> observed = new ArrayList<>(exactObservations("probe", emitted));
        ObservedPulse first = observed.get(0);
        observed.set(0, observed.get(1));
        observed.set(1, first);
        assertFalse(ReturnNetworkSignal.matchExact(timeline, emitted, observed).exactMatch(), "wrong order");
    }

    private static void partialRejected() {
        Timeline timeline = ReturnNetworkSignal.plan(10, TIMING);
        List<EmittedPulse> emitted = exactEmissions("s3", SignalUse.CORE, timeline);
        assertFalse(ReturnNetworkSignal.matchExact(
                timeline, emitted, exactObservations("probe", emitted).subList(0, 2)).exactMatch(), "partial");
    }

    private static void duplicateRejected() {
        Timeline timeline = ReturnNetworkSignal.plan(10, TIMING);
        List<EmittedPulse> emitted = exactEmissions("s4", SignalUse.CORE, timeline);
        List<ObservedPulse> observed = new ArrayList<>(exactObservations("probe", emitted));
        observed.set(2, observed.get(1));
        MatchResult result = ReturnNetworkSignal.matchExact(timeline, emitted, observed);
        assertFalse(result.exactMatch(), "duplicate identity index");
        assertTrue(result.detail().contains("identity index 2"), "duplicate location reported");
    }

    private static void wrongTimingRejected() {
        Timeline timeline = ReturnNetworkSignal.plan(10, TIMING);
        List<EmittedPulse> emitted = exactEmissions("s5", SignalUse.CORE, timeline);
        List<ObservedPulse> observed = new ArrayList<>(exactObservations("probe", emitted));
        ObservedPulse middle = observed.get(1);
        observed.set(1, new ObservedPulse(
                middle.observerIdentity(), middle.sessionIdentity(), middle.identityIndex(),
                middle.pulseClass(), middle.observedStartTick() + 1, middle.observedDurationTicks()));
        assertFalse(ReturnNetworkSignal.matchExact(timeline, emitted, observed).exactMatch(), "wrong start/gap");
    }

    private static void mismatchedObserverAndSessionRejected() {
        Timeline timeline = ReturnNetworkSignal.plan(10, TIMING);
        List<EmittedPulse> emitted = exactEmissions("s6", SignalUse.CORE, timeline);
        List<ObservedPulse> observerMismatch = new ArrayList<>(exactObservations("probe-A", emitted));
        ObservedPulse third = observerMismatch.get(2);
        observerMismatch.set(2, new ObservedPulse(
                "probe-B", third.sessionIdentity(), third.identityIndex(), third.pulseClass(),
                third.observedStartTick(), third.observedDurationTicks()));
        assertFalse(ReturnNetworkSignal.matchExact(timeline, emitted, observerMismatch).exactMatch(),
                "mixed observers");

        List<ObservedPulse> sessionMismatch = new ArrayList<>(exactObservations("probe-A", emitted));
        ObservedPulse second = sessionMismatch.get(1);
        sessionMismatch.set(1, new ObservedPulse(
                second.observerIdentity(), "different-session", second.identityIndex(), second.pulseClass(),
                second.observedStartTick(), second.observedDurationTicks()));
        assertFalse(ReturnNetworkSignal.matchExact(timeline, emitted, sessionMismatch).exactMatch(),
                "mixed sessions");
    }

    private static void pollAndDispatchDeduplicate() {
        TransientSession session = session("dedupe", 10);
        assertEquals(1, session.poll(10).size(), "first dispatch");
        assertEquals(0, session.poll(10).size(), "same poll deduplicated");
        assertTrue(session.dispatchDuePulse(0, 10).isEmpty(), "direct duplicate deduplicated");
        assertTrue(session.dispatchDuePulse(1, 10).isEmpty(), "early out-of-order dispatch blocked");
        assertEquals(1, session.emissions().size(), "one unique dispatch");
        assertEquals(SessionState.ACTIVE, session.state(), "early request does not invalidate schedule");
    }

    private static void skippedFirstDeadline() {
        TransientSession session = session("skip-first", 10);
        assertEquals(0, session.poll(11).size(), "no catch-up emission");
        assertEquals(SessionState.RESTART_REQUIRED, session.state(), "missed first is explicit");
        assertEquals(0, session.poll(15).size(), "invalidated session emits no tail");
    }

    private static void skippedSecondDeadline() {
        TransientSession session = session("skip-second", 10);
        session.poll(10);
        assertEquals(0, session.poll(16).size(), "no catch-up second");
        assertEquals(SessionState.RESTART_REQUIRED, session.state(), "missed second is explicit");
        assertEquals(1, session.emissions().size(), "old prefix retained only in old transient session");
    }

    private static void skippedLongDeadline() {
        TransientSession session = session("skip-long", 10);
        session.poll(10);
        session.poll(15);
        assertEquals(0, session.poll(23).size(), "no catch-up long");
        assertEquals(SessionState.RESTART_REQUIRED, session.state(), "missed long is explicit");
    }

    private static void suspendBeforeFirst() {
        TransientSession session = session("suspend-before", 10);
        session.suspend(9);
        assertRestartRequiredAndNoTail(session, 10);
    }

    private static void suspendBetweenPulses() {
        TransientSession session = session("suspend-between", 10);
        session.poll(10);
        session.suspend(14);
        assertRestartRequiredAndNoTail(session, 15);
    }

    private static void suspendInsidePulse() {
        TransientSession session = session("suspend-inside", 10);
        session.poll(10);
        session.suspend(11);
        assertRestartRequiredAndNoTail(session, 15);
        assertEquals(1, session.emissions().size(), "interrupted prefix cannot become full match");
    }

    private static void finalPulseInterruption() {
        TransientSession session = session("suspend-final", 10);
        emitWhole(session);
        session.suspend(23);
        assertEquals(SessionState.RESTART_REQUIRED, session.state(), "long interrupted before end");
        assertEquals(0, session.poll(30).size(), "no completion or tail after interruption");
        assertFalse(session.completed(), "interrupted long is not complete");
        List<ObservedPulse> fullLooking = exactObservations("probe", session.emissions());
        assertTrue(ReturnNetworkSignal.matchExact(
                session.timeline(), session.emissions(), fullLooking).exactMatch(),
                "structural matcher has no session state");
        assertFalse(session.observeMatch(fullLooking).exactMatch(),
                "session-gated observation rejects interrupted full-looking samples");
    }

    private static void repeatedSuspendPolicy() {
        TransientSession session = session("repeat-suspend", 10);
        session.suspend(9);
        expectThrows(IllegalStateException.class, () -> session.suspend(9));
        assertEquals(SessionState.RESTART_REQUIRED, session.state(), "state remains restart required");
    }

    private static void readinessLossRequiresRestart() {
        TransientSession session = session("readiness", 10);
        session.readinessLost(9);
        assertRestartRequiredAndNoTail(session, 10);
    }

    private static void restartEntirePlanAndMatch() {
        TransientSession old = session("old", 10);
        old.poll(10);
        old.suspend(11);
        expectThrows(IllegalArgumentException.class, () -> old.restart("old", 100));
        expectThrows(IllegalArgumentException.class, () -> old.restart("too-early", 10));

        TransientSession fresh = old.restart("fresh", 100);
        assertEquals(SessionState.ACTIVE, fresh.state(), "fresh active state");
        assertEquals(0, fresh.emissions().size(), "old emissions are not concatenated");
        assertEquals(List.of(0, 1, 2),
                fresh.timeline().pulses().stream().map(Pulse::identityIndex).toList(), "full identities");
        emitWhole(fresh);
        fresh.poll(fresh.timeline().completesAtTick());
        assertEquals(SessionState.COMPLETED, fresh.state(), "fresh full replay completes");
        List<ObservedPulse> freshObservations = exactObservations("fresh-probe", fresh.emissions());
        assertTrue(fresh.observeMatch(freshObservations).exactMatch(),
                "fresh replay has exact full recovery match");
        assertEquals(1, old.emissions().size(), "old partial session remains separate");
    }

    private static void restartOverflowIsAtomic() {
        TransientSession old = session("overflow-old", 10);
        old.poll(10);
        old.suspend(11);
        List<EmittedPulse> before = old.emissions();
        expectThrows(ArithmeticException.class,
                () -> old.restart("overflow-new", Long.MAX_VALUE - 2));
        assertEquals(SessionState.RESTART_REQUIRED, old.state(), "old state unchanged");
        assertEquals(before, old.emissions(), "old emissions unchanged");
        TransientSession recovered = old.restart("overflow-recovered", 100);
        assertEquals(0, recovered.emissions().size(), "later valid restart still clean");
    }

    private static void completedReplayPolicy() {
        TransientSession core = TransientSession.start(
                "core", SignalUse.CORE, 10, TIMING, CompletedReplayPolicy.ALLOW_AMBIENT_ONLY);
        emitWhole(core);
        core.poll(core.timeline().completesAtTick());
        assertTrue(core.completed(), "core completes");
        assertEquals(0, core.poll(core.timeline().completesAtTick() + 1).size(), "core does not repeat");
        expectThrows(IllegalStateException.class, () -> core.restart("core-repeat", 100));
        TransientSession ambient = core.replayCompletedAsAmbient("ambient", 200);
        assertEquals(SignalUse.AMBIENT, ambient.use(), "replay is explicitly ambient");
        assertEquals(1, ambient.poll(200).size(), "ambient starts at identity zero");
        expectThrows(IllegalArgumentException.class,
                () -> core.replayCompletedAsAmbient("core", 300));

        TransientSession blocked = session("blocked", 10);
        emitWhole(blocked);
        blocked.poll(blocked.timeline().completesAtTick());
        expectThrows(IllegalStateException.class,
                () -> blocked.replayCompletedAsAmbient("blocked-ambient", 200));
    }

    private static void transientBoundary() {
        assertFalse(Serializable.class.isAssignableFrom(TransientSession.class), "not Serializable");
        assertEquals(CarrierContract.NON_STATE_MUTATING_TRANSIENT_CUE,
                CarrierContract.values()[0], "carrier contract excludes state mutation");
        assertEquals(ObserverContract.TECHNICAL_SAMPLE_ONLY,
                ObserverContract.values()[0], "observer contract excludes perception claims");
        List<String> forbidden = List.of("save", "load", "persist", "checkpoint", "fact", "snapshot", "commit");
        for (Method method : TransientSession.class.getDeclaredMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            for (String token : forbidden) {
                assertFalse(name.contains(token), "no persistence-like method: " + method.getName());
            }
            assertFalse(name.equals("resume"), "no tail-resume API");
        }
        TransientSession original = session("original", 10);
        original.poll(10);
        TransientSession fresh = session("independent", 10);
        assertEquals(0, fresh.emissions().size(), "independent session inherits no state");
    }

    private static TransientSession session(String identity, long startTick) {
        return TransientSession.start(
                identity, SignalUse.CORE, startTick, TIMING, CompletedReplayPolicy.NO_REPLAY);
    }

    private static void assertRestartRequiredAndNoTail(TransientSession session, long nextDueTick) {
        assertEquals(SessionState.RESTART_REQUIRED, session.state(), "restart required");
        assertEquals(0, session.poll(nextDueTick).size(), "old session has no tail or catch-up");
    }

    private static List<EmittedPulse> exactEmissions(String session, SignalUse use, Timeline timeline) {
        return timeline.pulses().stream()
                .map(pulse -> new EmittedPulse(
                        session, use, pulse.identityIndex(), pulse.pulseClass(),
                        pulse.startTick(), pulse.startTick(), pulse.durationTicks()))
                .toList();
    }

    private static List<ObservedPulse> exactObservations(String observer, List<EmittedPulse> emitted) {
        return emitted.stream()
                .map(pulse -> new ObservedPulse(
                        observer, pulse.sessionIdentity(), pulse.identityIndex(), pulse.pulseClass(),
                        pulse.dispatchedAtTick(), pulse.durationTicks()))
                .toList();
    }

    private static void emitWhole(TransientSession session) {
        for (Pulse pulse : session.timeline().pulses()) {
            assertEquals(1, session.poll(pulse.startTick()).size(), "emit identity " + pulse.identityIndex());
        }
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("PASS " + name);
        } catch (Throwable error) {
            System.err.println("FAIL " + name + ": " + error);
            error.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void expectThrows(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("expected " + type.getName() + " but got " + error, error);
        }
        throw new AssertionError("expected " + type.getName());
    }
}
