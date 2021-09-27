package io.quarkus.dev.testing;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;

public class ContinuousTestingSharedStateManager {

    private static final CopyOnWriteArraySet<Consumer<State>> stateListeners = new CopyOnWriteArraySet<>();
    public static final State INITIAL_STATE = new StateBuilder()
            .setLastRun(-1).setIsLiveReload(true).build();
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
        setLastState((s) -> INITIAL_STATE);
    }

    public static void setLastState(Function<StateBuilder, State> modifier) {
        State state;
        synchronized (ContinuousTestingSharedStateManager.class) {
            StateBuilder builder = lastState.builder();
            state = lastState = modifier.apply(builder);
        }
        for (var sl : stateListeners) {
            sl.accept(state);
        }
    }

    public static State getLastState() {
        return lastState;
    }

    public static void setInProgress(boolean inProgress) {
        setLastState((s) -> s.setInProgress(inProgress).build());
    }

    public static void setRunning(boolean running) {
        setLastState((s) -> s.setRunning(running).build());
    }

    public static void setBrokenOnly(boolean brokenOnly) {
        setLastState((s) -> s.setIsBrokenOnly(brokenOnly).build());
    }

    public static void setTestOutput(boolean testOutput) {
        setLastState((s) -> s.setIsTestOutput(testOutput).build());
    }

    public static void setInstrumentationBasedReload(boolean instrumentationBasedReload) {
        setLastState((s) -> s.setIsInstrumentationBasedReload(instrumentationBasedReload).build());
    }

    public static void setLiveReloadEnabled(boolean liveReload) {
        setLastState((s) -> s.setIsLiveReload(liveReload).build());
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

        public State(StateBuilder builder) {
            this.lastRun = builder.lastRun;
            this.running = builder.running;
            this.inProgress = builder.inProgress;
            this.run = builder.run;
            this.passed = builder.passed;
            this.failed = builder.failed;
            this.skipped = builder.skipped;
            this.currentPassed = builder.currentPassed;
            this.currentFailed = builder.currentFailed;
            this.currentSkipped = builder.currentSkipped;
            this.isBrokenOnly = builder.isBrokenOnly;
            this.isTestOutput = builder.isTestOutput;
            this.isInstrumentationBasedReload = builder.isInstrumentationBasedReload;
            this.isLiveReload = builder.isLiveReload;
        }

        StateBuilder builder() {
            return new StateBuilder(this);
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

    public static class StateBuilder {

        public StateBuilder() {
        }

        public StateBuilder(State existing) {
            this.lastRun = existing.lastRun;
            this.running = existing.running;
            this.inProgress = existing.inProgress;
            this.run = existing.run;
            this.passed = existing.passed;
            this.failed = existing.failed;
            this.skipped = existing.skipped;
            this.currentPassed = existing.currentPassed;
            this.currentFailed = existing.currentFailed;
            this.currentSkipped = existing.currentSkipped;
            this.isBrokenOnly = existing.isBrokenOnly;
            this.isTestOutput = existing.isTestOutput;
            this.isInstrumentationBasedReload = existing.isInstrumentationBasedReload;
            this.isLiveReload = existing.isLiveReload;
        }

        long lastRun;
        boolean running;
        boolean inProgress;
        long run;
        long passed;
        long failed;
        long skipped;
        long currentPassed;
        long currentFailed;
        long currentSkipped;
        boolean isBrokenOnly;
        boolean isTestOutput;
        boolean isInstrumentationBasedReload;
        boolean isLiveReload;

        public StateBuilder setLastRun(long lastRun) {
            this.lastRun = lastRun;
            return this;
        }

        public StateBuilder setRunning(boolean running) {
            this.running = running;
            return this;
        }

        public StateBuilder setInProgress(boolean inProgress) {
            this.inProgress = inProgress;
            return this;
        }

        public StateBuilder setRun(long run) {
            this.run = run;
            return this;
        }

        public StateBuilder setPassed(long passed) {
            this.passed = passed;
            return this;
        }

        public StateBuilder setFailed(long failed) {
            this.failed = failed;
            return this;
        }

        public StateBuilder setSkipped(long skipped) {
            this.skipped = skipped;
            return this;
        }

        public StateBuilder setCurrentPassed(long currentPassed) {
            this.currentPassed = currentPassed;
            return this;
        }

        public StateBuilder setCurrentFailed(long currentFailed) {
            this.currentFailed = currentFailed;
            return this;
        }

        public StateBuilder setCurrentSkipped(long currentSkipped) {
            this.currentSkipped = currentSkipped;
            return this;
        }

        public StateBuilder setIsBrokenOnly(boolean isBrokenOnly) {
            this.isBrokenOnly = isBrokenOnly;
            return this;
        }

        public StateBuilder setIsTestOutput(boolean isTestOutput) {
            this.isTestOutput = isTestOutput;
            return this;
        }

        public StateBuilder setIsInstrumentationBasedReload(boolean isInstrumentationBasedReload) {
            this.isInstrumentationBasedReload = isInstrumentationBasedReload;
            return this;
        }

        public StateBuilder setIsLiveReload(boolean isLiveReload) {
            this.isLiveReload = isLiveReload;
            return this;
        }

        public long getLastRun() {
            return lastRun;
        }

        public boolean isRunning() {
            return running;
        }

        public boolean isInProgress() {
            return inProgress;
        }

        public long getRun() {
            return run;
        }

        public long getPassed() {
            return passed;
        }

        public long getFailed() {
            return failed;
        }

        public long getSkipped() {
            return skipped;
        }

        public long getCurrentPassed() {
            return currentPassed;
        }

        public long getCurrentFailed() {
            return currentFailed;
        }

        public long getCurrentSkipped() {
            return currentSkipped;
        }

        public boolean isBrokenOnly() {
            return isBrokenOnly;
        }

        public boolean isTestOutput() {
            return isTestOutput;
        }

        public boolean isInstrumentationBasedReload() {
            return isInstrumentationBasedReload;
        }

        public boolean isLiveReload() {
            return isLiveReload;
        }

        public ContinuousTestingSharedStateManager.State build() {
            return new ContinuousTestingSharedStateManager.State(this);
        }
    }
}
