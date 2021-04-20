package io.quarkus.dev.testing;

import java.util.function.Consumer;

//TODO: this is pretty horrible
public class ContinuousTestingWebsocketListener {

    private static Consumer<State> stateListener;
    private static volatile State lastState = new State(false, false, 0, 0, 0, 0);

    public static Consumer<State> getStateListener() {
        return stateListener;
    }

    public static void setStateListener(Consumer<State> stateListener) {
        ContinuousTestingWebsocketListener.stateListener = stateListener;
        if (lastState != null) {
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

    public static void setInProgress(boolean inProgress) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(state.running, inProgress, state.run, state.passed, state.failed, state.skipped));
        }
    }

    public static void setRunning(boolean running) {
        State state = lastState;
        if (state != null) {
            setLastState(new State(running, running && state.inProgress, state.run, state.passed, state.failed, state.skipped));
        }
    }

    public static class State {
        public final boolean running;
        public final boolean inProgress;
        public final long run;
        public final long passed;
        public final long failed;
        public final long skipped;

        public State(boolean running, boolean inProgress, long run, long passed, long failed, long skipped) {
            this.running = running;
            this.inProgress = inProgress;
            this.run = run;
            this.passed = passed;
            this.failed = failed;
            this.skipped = skipped;
        }
    }
}
