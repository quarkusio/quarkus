package io.quarkus.runtime.shutdown;

/**
 * Represents a type of action executed during the Quarkus shutdown sequence.
 * Each action is traced with a JFR {@link ShutdownActionEvent}.
 *
 * @see ShutdownActionEvent
 */
public enum ShutdownAction {

    SHUTDOWN_LISTENER_PRESHUTDOWN,
    SHUTDOWN_LISTENER_SHUTDOWN,
    SHUTDOWN_TASK,
    LAST_SHUTDOWN_TASK;

    /**
     * Runs the given action and emits a JFR event with this action type and the provided info.
     *
     * @param info additional information about the action, e.g. the class name of the shutdown handler
     * @param action the action to execute
     */
    public void run(Object info, Runnable action) {
        ShutdownActionEvent event = new ShutdownActionEvent();
        event.begin();
        try {
            action.run();
        } finally {
            event.end();
            if (event.shouldCommit()) {
                event.actionType = name();
                event.info = info != null ? info.toString() : null;
                event.commit();
            }
        }
    }

}
