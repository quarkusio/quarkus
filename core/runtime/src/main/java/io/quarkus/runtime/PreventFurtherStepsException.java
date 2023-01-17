package io.quarkus.runtime;

/**
 * When this exception is thrown from a runtime init recorder method, then no other recorder steps
 * will be executed and the application will be terminated.
 * This is likely of very limited use, but it does allow us to boot the application up to a certain point
 * for example in AppCDS generation.
 * It can also be used for sidecar type scenarios where the sidecar is the same application but only performs
 * a small part of the bootstrap steps.
 */
public final class PreventFurtherStepsException extends RuntimeException {

    private final int exitCode;

    public PreventFurtherStepsException() {
        this(1);
    }

    public PreventFurtherStepsException(int exitCode) {
        this(null, exitCode);
    }

    public PreventFurtherStepsException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
