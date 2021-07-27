package io.quarkus.dev.testing;

import java.util.function.Consumer;

//TODO: this is pretty horrible
public class ContinuousTestingWebsocketListener {

    private static Consumer<State> stateListener;
    private static volatile State lastState = new State(false, false, 0, 0, 0, 0, false, false, false, true);

    public static Consumer<State> getStateListener() {
        return stateListener;
    }

    public static void setStateListener(Consumer<State> stateListener) {
        ContinuousTestingWebsocketListener.stateListener = stateListener;
        if (lastState != null && stateListener != null) {
            stateListener.accept(lastState);
        }
    }

    public static void setLastState(State state) {
        lastState = state;
        Consumer<State> sl = stateListener;
        if (sl != null) {
            sl.accept(state);
        }
    }

    public static State getLastState() {
        return lastState;
    }

    public static void setInProgress(boolean inProgress) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(state.running, inProgress, state.run, state.passed, state.failed, state.skipped,
                    state.isBrokenOnly, state.isTestOutput, state.isInstrumentationBasedReload, state.isLiveReload));
        }
    }

    public static void setRunning(boolean running) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(running, running && state.inProgress, state.run, state.passed, state.failed, state.skipped,
                    state.isBrokenOnly, state.isTestOutput, state.isInstrumentationBasedReload, state.isLiveReload));
        }
    }

    public static void setBrokenOnly(boolean brokenOnly) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(state.running, state.inProgress, state.run, state.passed, state.failed, state.skipped,
                    brokenOnly, state.isTestOutput, state.isInstrumentationBasedReload, state.isLiveReload));
        }
    }

    public static void setTestOutput(boolean testOutput) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(state.running, state.inProgress, state.run, state.passed, state.failed, state.skipped,
                    state.isBrokenOnly, testOutput, state.isInstrumentationBasedReload, state.isLiveReload));
        }
    }

    public static void setInstrumentationBasedReload(boolean instrumentationBasedReload) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(state.running, state.inProgress, state.run, state.passed, state.failed, state.skipped,
                    state.isBrokenOnly, state.isTestOutput, instrumentationBasedReload, state.isLiveReload));
        }
    }

    public static void setLiveReloadEnabled(boolean liveReload) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(state.running, state.inProgress, state.run, state.passed, state.failed, state.skipped,
                    state.isBrokenOnly, state.isTestOutput, state.isInstrumentationBasedReload, liveReload));
        }
    }

    public static class State {
        public final boolean running;
        public final boolean inProgress;
        public final long run;
        public final long passed;
        public final long failed;
        public final long skipped;
        public final boolean isBrokenOnly;
        public final boolean isTestOutput;
        public final boolean isInstrumentationBasedReload;
        public final boolean isLiveReload;

        public State(boolean running, boolean inProgress, long run, long passed, long failed, long skipped,
                boolean isBrokenOnly, boolean isTestOutput, boolean isInstrumentationBasedReload, boolean isLiveReload) {
            this.running = running;
            this.inProgress = inProgress;
            this.run = run;
            this.passed = passed;
            this.failed = failed;
            this.skipped = skipped;
            this.isBrokenOnly = isBrokenOnly;
            this.isTestOutput = isTestOutput;
            this.isInstrumentationBasedReload = isInstrumentationBasedReload;
            this.isLiveReload = isLiveReload;
        }
    }
}
