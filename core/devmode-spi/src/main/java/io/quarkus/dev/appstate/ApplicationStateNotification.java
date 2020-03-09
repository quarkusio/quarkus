package io.quarkus.dev.appstate;

/**
 * A class that allows for access to the application state, even from outside the runtime class loader.
 *
 * This should generally only be used by dev mode internals that need information about the current
 * application state.
 *
 * This class makes not attempt to verify that an application is starting/stopping when the
 * wait methods are called, this should only be called by a client that is controlling the Quarkus
 * lifecycle, and so knows what the current lifecycle state is.
 */
public class ApplicationStateNotification {

    private static boolean started = false;
    private static Throwable startupProblem;

    public static synchronized void notifyStartupComplete(Throwable sp) {
        started = startupProblem == null;
        startupProblem = sp;
        ApplicationStateNotification.class.notifyAll();
    }

    public static synchronized void notifyApplicationStopped() {
        started = false;
        startupProblem = null;
        ApplicationStateNotification.class.notifyAll();
    }

    public static synchronized void waitForApplicationStart() {
        while (!started && startupProblem == null) {
            try {
                ApplicationStateNotification.class.wait();
            } catch (InterruptedException e) {
                //ignore
            }
        }
        if (startupProblem != null) {
            throw new RuntimeException(startupProblem);
        }
    }

    public static synchronized void waitForApplicationStop() {
        while (started) {
            try {
                ApplicationStateNotification.class.wait();
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }
}
