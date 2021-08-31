package io.quarkus.dev.testing;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

public class ContinuousTestingSharedStateManager {

    private static final CopyOnWriteArraySet<Consumer<State>> stateListeners = new CopyOnWriteArraySet<>();
    public static final State INITIAL_STATE = new State(-1, false, false, 0, 0, 0, 0, 0, 0, 0, false, false, false, true);
    private static volatile State lastState = INITIAL_STATE;

    public static void addStateListener(Consumer<State> stateListener) {
        stateListeners.add(stateListener);
        if (lastState != null) {
            stateListener.accept(lastState);
        }
    }

    public static void removeStateListener(Consumer<State> stateListener) {
        stateListeners.remove(stateListener);
    }

    public static void reset() {
        setLastState(INITIAL_STATE);
    }

    public static void setLastState(State state) {
        lastState = state;
        for (var sl : stateListeners) {
            sl.accept(state);
        }
    }

    public static State getLastState() {
        return lastState;
    }

    public static void setInProgress(boolean inProgress) {
        State state = lastState;
        if (state != null) {
            setLastState(
                    new State(state.lastRun, state.running, inProgress, state.run, state.passed, state.failed, state.skipped,
                            state.currentPassed, state.currentFailed, state.currentSkipped, state.isBrokenOnly,
                            state.isTestOutput, state.isInstrumentationBasedReload, state.isLiveReload));
        }
    }

    public static void setRunning(boolean running) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(state.lastRun, running, running && state.inProgress, state.run, state.passed, state.failed,
                    state.skipped,
                    state.currentPassed, state.currentFailed, state.currentSkipped, state.isBrokenOnly, state.isTestOutput,
                    state.isInstrumentationBasedReload, state.isLiveReload));
        }
    }

    public static void setBrokenOnly(boolean brokenOnly) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(state.lastRun, state.running, state.inProgress, state.run, state.passed, state.failed,
                    state.skipped,
                    state.currentPassed, state.currentFailed, state.currentSkipped, brokenOnly, state.isTestOutput,
                    state.isInstrumentationBasedReload, state.isLiveReload));
        }
    }

    public static void setTestOutput(boolean testOutput) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(state.lastRun, state.running, state.inProgress, state.run, state.passed, state.failed,
                    state.skipped,
                    state.currentPassed, state.currentFailed, state.currentSkipped, state.isBrokenOnly, testOutput,
                    state.isInstrumentationBasedReload, state.isLiveReload));
        }
    }

    public static void setInstrumentationBasedReload(boolean instrumentationBasedReload) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(state.lastRun, state.running, state.inProgress, state.run, state.passed, state.failed,
                    state.skipped,
                    state.currentPassed, state.currentFailed, state.currentSkipped, state.isBrokenOnly, state.isTestOutput,
                    instrumentationBasedReload, state.isLiveReload));
        }
    }

    public static void setLiveReloadEnabled(boolean liveReload) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(state.lastRun, state.running, state.inProgress, state.run, state.passed, state.failed,
                    state.skipped,
                    state.currentPassed, state.currentFailed, state.currentSkipped, state.isBrokenOnly, state.isTestOutput,
                    state.isInstrumentationBasedReload, liveReload));
        }
    }

    public static class State {
        public final long lastRun;
        public final boolean running;
        public final boolean inProgress;
        public final long run;
        public final long passed;
        public final long failed;
        public final long skipped;
        public final long currentPassed;
        public final long currentFailed;
        public final long currentSkipped;
        public final boolean isBrokenOnly;
        public final boolean isTestOutput;
        public final boolean isInstrumentationBasedReload;
        public final boolean isLiveReload;

        public State(long lastRun, boolean running, boolean inProgress, long run, long passed, long failed, long skipped,
                long currentPassed, long currentFailed, long currentSkipped, boolean isBrokenOnly, boolean isTestOutput,
                boolean isInstrumentationBasedReload, boolean isLiveReload) {
            this.lastRun = lastRun;
            this.running = running;
            this.inProgress = inProgress;
            this.run = run;
            this.passed = passed;
            this.failed = failed;
            this.skipped = skipped;
            this.currentPassed = currentPassed;
            this.currentFailed = currentFailed;
            this.currentSkipped = currentSkipped;
            this.isBrokenOnly = isBrokenOnly;
            this.isTestOutput = isTestOutput;
            this.isInstrumentationBasedReload = isInstrumentationBasedReload;
            this.isLiveReload = isLiveReload;
        }

        @Override
        public String toString() {
            return "State{" +
                    "lastRun=" + lastRun +
                    ", running=" + running +
                    ", inProgress=" + inProgress +
                    ", run=" + run +
                    ", passed=" + passed +
                    ", failed=" + failed +
                    ", skipped=" + skipped +
                    ", isBrokenOnly=" + isBrokenOnly +
                    ", isTestOutput=" + isTestOutput +
                    ", isInstrumentationBasedReload=" + isInstrumentationBasedReload +
                    ", isLiveReload=" + isLiveReload +
                    '}';
        }
    }
}
