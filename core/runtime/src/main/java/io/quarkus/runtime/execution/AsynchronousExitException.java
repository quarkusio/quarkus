package io.quarkus.runtime.execution;

/**
 * An exception which indicates that the application is exiting asynchronously due to {@link System#exit} being
 * called.
 */
public class AsynchronousExitException extends Exception {
    private static final long serialVersionUID = -2567887171725334485L;

    /**
     * Constructs a new {@code AsynchronousExitException} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public AsynchronousExitException() {
    }

    /**
     * Constructs a new {@code AsynchronousExitException} instance with an initial message. No
     * cause is specified.
     *
     * @param msg the message
     */
    public AsynchronousExitException(final String msg) {
        super(msg);
    }
}
