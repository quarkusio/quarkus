package io.quarkus.runtime.init;

public class InitializeClassPreInitRunnable implements Runnable {

    private static final boolean PRE_INIT_RAISE_ERRORS = Boolean.getBoolean("quarkus-pre-init-raise-errors");

    private final String className;

    public InitializeClassPreInitRunnable(String className) {
        this.className = className;
    }

    @Override
    public void run() {
        try {
            Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (Throwable e) {
            // when testing Quarkus itself, we raise the errors, so that we can catch them
            if (PRE_INIT_RAISE_ERRORS) {
                throw new IllegalStateException(
                        "Unable to preload class: " + className + ". Pre-init instructions need to be adjusted.", e);
            }

            // otherwise we simply ignore the errors
        }
    }
}
