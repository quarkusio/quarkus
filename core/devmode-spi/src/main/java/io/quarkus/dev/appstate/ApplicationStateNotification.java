package io.quarkus.dev.appstate;

/**
 * A class that allows for access to the application state, even from outside the runtime class loader.
 *
 * This should generally only be used by dev mode internals that need information about the current
 * application state.
 *
 * This class makes no attempt to verify that an application is starting/stopping when the
 * wait methods are called, this should only be called by a client that is controlling the Quarkus
 * lifecycle, and so knows what the current lifecycle state is.
 */
public class ApplicationStateNotification {

    private static State state = State.INITIAL;
    private static Throwable startupProblem;

    public static synchronized void reset() {
        if (state == State.STARTED) {
            throw new IllegalStateException("Cannot reset a started application");
        }
        state = State.INITIAL;
        startupProblem = null;
    }

    public static synchronized void notifyStartupComplete() {
        state = State.STARTED;
        ApplicationStateNotification.class.notifyAll();
    }

    public static synchronized void notifyApplicationStopped() {
        state = State.STOPPED;
        ApplicationStateNotification.class.notifyAll();
    }

    /**
     * Notify of startup failure.
     *
     * Before this method is called the exception should be logged.
     *
     * @param t The exception
     */
    public static synchronized void notifyStartupFailed(Throwable t) {
        startupProblem = t;
        state = State.STOPPED;
        ApplicationStateNotification.class.notifyAll();
    }

    public static synchronized void waitForApplicationStart() {
        while (state == State.INITIAL) {
            try {
                ApplicationStateNotification.class.wait();
            } catch (InterruptedException e) {
                //ignore
            }
        }
        if (startupProblem != null) {
            // let's not keep the startupProblem around in the static field
            // as it keeps a reference to the Application in the backtrace
            Throwable localStartupProblem = startupProblem;
            startupProblem = null;

            throw new ApplicationStartException(localStartupProblem);
        }
    }

    public static synchronized void waitForApplicationStop() {
        while (state != State.STOPPED) {
            try {
                ApplicationStateNotification.class.wait();
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    public static State getState() {
        return state;
    }

    public enum State {
        INITIAL,
        STARTED,
        STOPPED
    }
}
