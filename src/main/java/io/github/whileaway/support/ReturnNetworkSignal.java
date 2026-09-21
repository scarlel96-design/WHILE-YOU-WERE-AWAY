package io.github.whileaway.support;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A bounded, transient representation of the return-network signal.
 *
 * <p>The semantic pattern is always SHORT, SHORT, LONG. Tick values are runtime
 * tuning, not story facts. This type deliberately has no Minecraft, world,
 * network, save-data, checkpoint, or fact API.</p>
 */
public final class ReturnNetworkSignal {
    public static final List<PulseClass> SEMANTIC_PATTERN =
            List.of(PulseClass.SHORT, PulseClass.SHORT, PulseClass.LONG);

    private ReturnNetworkSignal() {
    }

    public enum PulseClass {
        SHORT,
        LONG
    }

    public enum SignalUse {
        CORE,
        AMBIENT
    }

    /**
     * The planner may be bound only to a transient cue that does not mutate the
     * carrier's activation or other world state. Carrier selection remains an
     * integration concern and is intentionally absent from this model.
     */
    public enum CarrierContract {
        NON_STATE_MUTATING_TRANSIENT_CUE
    }

    /** Observation means a technical sample only, never inferred human perception. */
    public enum ObserverContract {
        TECHNICAL_SAMPLE_ONLY
    }

    /** Controls whether a completed session may be explicitly replayed as ambience. */
    public enum CompletedReplayPolicy {
        NO_REPLAY,
        ALLOW_AMBIENT_ONLY
    }

    public enum SessionState {
        ACTIVE,
        RESTART_REQUIRED,
        COMPLETED
    }

    /** Runtime timing only; none of these values is narrative canon. */
    public record Timing(long shortTicks, long longTicks, long firstGapTicks, long secondGapTicks) {
        public Timing {
            requirePositive(shortTicks, "shortTicks");
            requirePositive(longTicks, "longTicks");
            requirePositive(firstGapTicks, "firstGapTicks");
            requirePositive(secondGapTicks, "secondGapTicks");
            if (shortTicks >= longTicks) {
                throw new IllegalArgumentException("shortTicks must be less than longTicks");
            }
        }
    }

    /** A planned pulse. endTickExclusive uses checked arithmetic. */
    public record Pulse(int identityIndex, PulseClass pulseClass, long startTick, long durationTicks) {
        public Pulse {
            if (identityIndex < 0) {
                throw new IllegalArgumentException("identityIndex must be non-negative");
            }
            Objects.requireNonNull(pulseClass, "pulseClass");
            requireNonNegative(startTick, "startTick");
            requirePositive(durationTicks, "durationTicks");
            Math.addExact(startTick, durationTicks);
        }

        public long endTickExclusive() {
            return Math.addExact(startTick, durationTicks);
        }
    }

    /** Immutable deterministic plan with explicit pulse identities and gap information. */
    public record Timeline(long originTick, Timing timing, List<Pulse> pulses, long completesAtTick) {
        public Timeline {
            requireNonNegative(originTick, "originTick");
            Objects.requireNonNull(timing, "timing");
            pulses = List.copyOf(Objects.requireNonNull(pulses, "pulses"));
            if (pulses.size() != SEMANTIC_PATTERN.size()) {
                throw new IllegalArgumentException("timeline must contain exactly three pulses");
            }
            for (int index = 0; index < pulses.size(); index++) {
                Pulse pulse = pulses.get(index);
                if (pulse.identityIndex() != index || pulse.pulseClass() != SEMANTIC_PATTERN.get(index)) {
                    throw new IllegalArgumentException("pulse identity/order does not match SHORT SHORT LONG");
                }
                long expectedDuration = pulse.pulseClass() == PulseClass.SHORT
                        ? timing.shortTicks() : timing.longTicks();
                if (pulse.durationTicks() != expectedDuration) {
                    throw new IllegalArgumentException("pulse duration does not match its class");
                }
            }
            if (pulses.get(0).startTick() != originTick) {
                throw new IllegalArgumentException("originTick must equal the first pulse start");
            }
            long expectedSecondStart = Math.addExact(
                    pulses.get(0).endTickExclusive(), timing.firstGapTicks());
            if (pulses.get(1).startTick() != expectedSecondStart) {
                throw new IllegalArgumentException("first actual gap must equal firstGapTicks");
            }
            long expectedThirdStart = Math.addExact(
                    pulses.get(1).endTickExclusive(), timing.secondGapTicks());
            if (pulses.get(2).startTick() != expectedThirdStart) {
                throw new IllegalArgumentException("second actual gap must equal secondGapTicks");
            }
            if (completesAtTick != pulses.get(pulses.size() - 1).endTickExclusive()) {
                throw new IllegalArgumentException("completesAtTick must equal the final pulse end");
            }
        }

        public long gapAfter(int identityIndex) {
            if (identityIndex < 0 || identityIndex >= pulses.size() - 1) {
                throw new IllegalArgumentException("gap identity must be 0 or 1");
            }
            return Math.subtractExact(pulses.get(identityIndex + 1).startTick(),
                    pulses.get(identityIndex).endTickExclusive());
        }
    }

    /** Creates the exact SHORT, SHORT, LONG timeline. */
    public static Timeline plan(long originTick, Timing timing) {
        requireNonNegative(originTick, "originTick");
        Objects.requireNonNull(timing, "timing");

        Pulse first = new Pulse(0, PulseClass.SHORT, originTick, timing.shortTicks());
        long secondStart = Math.addExact(first.endTickExclusive(), timing.firstGapTicks());
        Pulse second = new Pulse(1, PulseClass.SHORT, secondStart, timing.shortTicks());
        long thirdStart = Math.addExact(second.endTickExclusive(), timing.secondGapTicks());
        Pulse third = new Pulse(2, PulseClass.LONG, thirdStart, timing.longTicks());
        return new Timeline(originTick, timing, List.of(first, second, third), third.endTickExclusive());
    }

    /** Actual transient dispatch. This is not an observation receipt. */
    public record EmittedPulse(
            String sessionIdentity,
            SignalUse use,
            int identityIndex,
            PulseClass pulseClass,
            long plannedStartTick,
            long dispatchedAtTick,
            long durationTicks) {
        public EmittedPulse {
            requireText(sessionIdentity, "sessionIdentity");
            Objects.requireNonNull(use, "use");
            if (identityIndex < 0) {
                throw new IllegalArgumentException("identityIndex must be non-negative");
            }
            Objects.requireNonNull(pulseClass, "pulseClass");
            requireNonNegative(plannedStartTick, "plannedStartTick");
            requireNonNegative(dispatchedAtTick, "dispatchedAtTick");
            requirePositive(durationTicks, "durationTicks");
        }

        public CarrierContract carrierContract() {
            return CarrierContract.NON_STATE_MUTATING_TRANSIENT_CUE;
        }
    }

    /**
     * A report from an explicitly named observer. It records an observation;
     * it does not assert that a player or human perceived the pulse.
     */
    public record ObservedPulse(
            String observerIdentity,
            String sessionIdentity,
            int identityIndex,
            PulseClass pulseClass,
            long observedStartTick,
            long observedDurationTicks) {
        public ObservedPulse {
            requireText(observerIdentity, "observerIdentity");
            requireText(sessionIdentity, "sessionIdentity");
            if (identityIndex < 0) {
                throw new IllegalArgumentException("identityIndex must be non-negative");
            }
            Objects.requireNonNull(pulseClass, "pulseClass");
            requireNonNegative(observedStartTick, "observedStartTick");
            requirePositive(observedDurationTicks, "observedDurationTicks");
        }

        public ObserverContract observerContract() {
            return ObserverContract.TECHNICAL_SAMPLE_ONLY;
        }
    }

    public record MatchResult(boolean exactMatch, String detail) {
        public MatchResult {
            Objects.requireNonNull(detail, "detail");
        }

        private static MatchResult exact() {
            return new MatchResult(true, "exact full sequence");
        }

        private static MatchResult mismatch(String detail) {
            return new MatchResult(false, detail);
        }
    }

    /**
     * Performs structural comparison only: a complete, ordered, duplicate-free
     * emission and observation of the supplied plan. Call
     * {@link TransientSession#observeMatch(List)} when session completion state
     * must also be enforced.
     */
    public static MatchResult matchExact(
            Timeline timeline,
            List<EmittedPulse> emissions,
            List<ObservedPulse> observations) {
        Objects.requireNonNull(timeline, "timeline");
        emissions = List.copyOf(Objects.requireNonNull(emissions, "emissions"));
        observations = List.copyOf(Objects.requireNonNull(observations, "observations"));
        int expectedCount = timeline.pulses().size();
        if (emissions.size() != expectedCount) {
            return MatchResult.mismatch("emission count must be exactly " + expectedCount);
        }
        if (observations.size() != expectedCount) {
            return MatchResult.mismatch("observation count must be exactly " + expectedCount);
        }

        String sessionIdentity = emissions.get(0).sessionIdentity();
        SignalUse use = emissions.get(0).use();
        String observerIdentity = observations.get(0).observerIdentity();
        for (int index = 0; index < expectedCount; index++) {
            Pulse planned = timeline.pulses().get(index);
            EmittedPulse emitted = emissions.get(index);
            ObservedPulse observed = observations.get(index);
            if (emitted.identityIndex() != index
                    || emitted.pulseClass() != planned.pulseClass()
                    || emitted.plannedStartTick() != planned.startTick()
                    || emitted.dispatchedAtTick() != planned.startTick()
                    || emitted.durationTicks() != planned.durationTicks()
                    || emitted.use() != use
                    || !emitted.sessionIdentity().equals(sessionIdentity)) {
                return MatchResult.mismatch("emission mismatch at identity index " + index);
            }
            if (observed.identityIndex() != index
                    || observed.pulseClass() != emitted.pulseClass()
                    || observed.observedStartTick() != emitted.dispatchedAtTick()
                    || observed.observedDurationTicks() != emitted.durationTicks()
                    || !observed.observerIdentity().equals(observerIdentity)
                    || !observed.sessionIdentity().equals(sessionIdentity)) {
                return MatchResult.mismatch("observation mismatch at identity index " + index);
            }
        }
        return MatchResult.exact();
    }

    /**
     * In-memory dispatch state for one session. It is intentionally not
     * serializable and exposes no snapshot, checkpoint, fact, save, or load API.
     */
    public static final class TransientSession {
        private final String sessionIdentity;
        private final SignalUse use;
        private final CompletedReplayPolicy replayPolicy;
        private Timeline timeline;
        private final BitSet emittedIdentities = new BitSet(SEMANTIC_PATTERN.size());
        private final List<EmittedPulse> emissions = new ArrayList<>(SEMANTIC_PATTERN.size());
        private long lastInteractionTick = -1;
        private SessionState state = SessionState.ACTIVE;

        private TransientSession(
                String sessionIdentity,
                SignalUse use,
                Timeline timeline,
                CompletedReplayPolicy replayPolicy) {
            this.sessionIdentity = requireText(sessionIdentity, "sessionIdentity");
            this.use = Objects.requireNonNull(use, "use");
            this.timeline = Objects.requireNonNull(timeline, "timeline");
            this.replayPolicy = Objects.requireNonNull(replayPolicy, "replayPolicy");
        }

        public static TransientSession start(
                String sessionIdentity,
                SignalUse use,
                long startTick,
                Timing timing,
                CompletedReplayPolicy replayPolicy) {
            return new TransientSession(sessionIdentity, use, plan(startTick, timing), replayPolicy);
        }

        public synchronized List<EmittedPulse> poll(long nowTick) {
            acceptMonotonicTick(nowTick);
            if (state != SessionState.ACTIVE) {
                return List.of();
            }
            if (missedDeadline(nowTick)) {
                state = SessionState.RESTART_REQUIRED;
                return List.of();
            }
            List<EmittedPulse> newlyEmitted = new ArrayList<>(1);
            for (Pulse pulse : timeline.pulses()) {
                if (pulse.startTick() == nowTick) {
                    dispatchInternal(pulse.identityIndex(), nowTick).ifPresent(newlyEmitted::add);
                }
            }
            updateCompletion(nowTick);
            return List.copyOf(newlyEmitted);
        }

        public synchronized Optional<EmittedPulse> dispatchDuePulse(int identityIndex, long nowTick) {
            acceptMonotonicTick(nowTick);
            if (state != SessionState.ACTIVE) {
                return Optional.empty();
            }
            if (missedDeadline(nowTick)) {
                state = SessionState.RESTART_REQUIRED;
                return Optional.empty();
            }
            Optional<EmittedPulse> emitted = dispatchInternal(identityIndex, nowTick);
            updateCompletion(nowTick);
            return emitted;
        }

        private Optional<EmittedPulse> dispatchInternal(int identityIndex, long nowTick) {
            if (identityIndex < 0 || identityIndex >= timeline.pulses().size()) {
                throw new IllegalArgumentException("identityIndex must be 0, 1, or 2");
            }
            if (emittedIdentities.get(identityIndex)) {
                return Optional.empty();
            }
            if (identityIndex > 0 && !emittedIdentities.get(identityIndex - 1)) {
                return Optional.empty();
            }
            Pulse pulse = timeline.pulses().get(identityIndex);
            if (pulse.startTick() != nowTick) {
                return Optional.empty();
            }
            EmittedPulse emitted = new EmittedPulse(
                    sessionIdentity,
                    use,
                    pulse.identityIndex(),
                    pulse.pulseClass(),
                    pulse.startTick(),
                    nowTick,
                    pulse.durationTicks());
            emittedIdentities.set(identityIndex);
            emissions.add(emitted);
            return Optional.of(emitted);
        }

        public synchronized void suspend(long nowTick) {
            acceptMonotonicTick(nowTick);
            if (state != SessionState.ACTIVE) {
                throw new IllegalStateException("only an active session can be suspended");
            }
            state = SessionState.RESTART_REQUIRED;
        }

        /** Readiness loss follows the same fail-closed interruption policy. */
        public synchronized void readinessLost(long nowTick) {
            suspend(nowTick);
        }

        /**
         * Starts the complete plan at identity zero under a distinct transient
         * session identity. No prior emission or observation is copied.
         */
        public synchronized TransientSession restart(String newSessionIdentity, long startTick) {
            if (state != SessionState.RESTART_REQUIRED) {
                throw new IllegalStateException("restart requires RESTART_REQUIRED state");
            }
            if (sessionIdentity.equals(newSessionIdentity)) {
                throw new IllegalArgumentException("restart requires a new session identity");
            }
            requireNonNegative(startTick, "startTick");
            if (startTick < lastInteractionTick) {
                throw new IllegalArgumentException("restart startTick must not precede the old session");
            }
            return start(newSessionIdentity, use, startTick, timeline.timing(), replayPolicy);
        }

        /**
         * Completed replay is explicit and ambient-only. Core repetition in the
         * same narrative session is rejected; a genuinely new core session must
         * be created with {@link #start} and a new session identity.
         */
        public synchronized TransientSession replayCompletedAsAmbient(String newSessionIdentity, long startTick) {
            if (state != SessionState.COMPLETED) {
                throw new IllegalStateException("only a completed session can be replayed");
            }
            if (replayPolicy != CompletedReplayPolicy.ALLOW_AMBIENT_ONLY) {
                throw new IllegalStateException("completed replay is disabled");
            }
            if (sessionIdentity.equals(newSessionIdentity)) {
                throw new IllegalArgumentException("replay requires a new session identity");
            }
            requireNonNegative(startTick, "startTick");
            if (startTick < lastInteractionTick) {
                throw new IllegalArgumentException("replay startTick must not precede the completed session");
            }
            return start(newSessionIdentity, SignalUse.AMBIENT, startTick, timeline.timing(), replayPolicy);
        }

        /** Requires COMPLETED state before applying the structural comparison. */
        public synchronized MatchResult observeMatch(List<ObservedPulse> observations) {
            if (state != SessionState.COMPLETED) {
                return MatchResult.mismatch("session must be COMPLETED before observation matching");
            }
            return matchExact(timeline, emissions, observations);
        }

        public synchronized boolean completed() {
            return state == SessionState.COMPLETED;
        }

        public synchronized SessionState state() {
            return state;
        }

        public String sessionIdentity() {
            return sessionIdentity;
        }

        public SignalUse use() {
            return use;
        }

        public synchronized Timeline timeline() {
            return timeline;
        }

        public synchronized List<EmittedPulse> emissions() {
            return List.copyOf(emissions);
        }

        private void acceptMonotonicTick(long tick) {
            requireNonNegative(tick, "tick");
            if (tick < lastInteractionTick) {
                throw new IllegalArgumentException("session ticks must be monotonic");
            }
            lastInteractionTick = tick;
        }

        private void updateCompletion(long nowTick) {
            if (emittedIdentities.cardinality() == timeline.pulses().size()
                    && nowTick >= timeline.completesAtTick()) {
                state = SessionState.COMPLETED;
            }
        }

        private boolean missedDeadline(long nowTick) {
            for (Pulse pulse : timeline.pulses()) {
                if (!emittedIdentities.get(pulse.identityIndex()) && pulse.startTick() < nowTick) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

}
