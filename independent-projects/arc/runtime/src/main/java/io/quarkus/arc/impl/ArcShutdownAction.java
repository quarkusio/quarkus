package io.quarkus.arc.impl;

/**
 * Represents a type of action executed during the ArC container shutdown.
 * Each action is traced with a JFR {@link ArcShutdownActionEvent}.
 *
 * @see ArcShutdownActionEvent
 */
enum ArcShutdownAction {

    CDI_BEFORE_DESTROYED_APPLICATION_CONTEXT,
    CDI_DESTROYED_APPLICATION_CONTEXT,
    CDI_DESTROY_APPLICATION_BEAN,
    CDI_DESTROY_SINGLETON_BEAN;

    /**
     * Runs the given action and emits a JFR event with this action type and the provided info.
     *
     * @param info additional information about the action, e.g. the bean class name
     * @param action the action to execute
     */
    void run(Object info, Runnable action) {
        ArcShutdownActionEvent event = new ArcShutdownActionEvent();
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
