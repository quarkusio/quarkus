package io.quarkus.quickcli;

/**
 * Standard exit codes for CLI applications.
 */
public final class ExitCode {
    /** Successful execution. */
    public static final int OK = 0;
    /** Execution error in user code. */
    public static final int SOFTWARE = 1;
    /** Invalid command-line usage (bad options, missing arguments). */
    public static final int USAGE = 2;

    private ExitCode() {
    }
}
