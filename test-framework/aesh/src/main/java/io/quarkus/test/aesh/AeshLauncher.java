package io.quarkus.test.aesh;

import java.time.Duration;

/**
 * Test utility for interacting with an Aesh REPL (console mode) application.
 * <p>
 * Injected as a method parameter in {@code @QuarkusMainTest} tests. Starts the
 * REPL on a background thread and allows sending commands and asserting on
 * their output.
 */
public interface AeshLauncher {

    /**
     * Default timeout for {@link #executeCommand(String)}.
     */
    Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Start the REPL session on a background thread.
     * Non-blocking -- returns immediately after the console is ready.
     *
     * @param args optional initial arguments
     */
    void launch(String... args);

    /**
     * Send a command to the REPL and wait for it to complete.
     * Uses the {@link #DEFAULT_TIMEOUT default timeout} of 30 seconds.
     * <p>
     * The output buffer is cleared before each command, so the returned
     * string contains only the output from this specific command.
     *
     * @param command the command string to execute
     * @return the command output
     * @throws RuntimeException if the command does not complete within the timeout
     */
    String executeCommand(String command);

    /**
     * Send a command to the REPL and wait for it to complete with a custom timeout.
     * <p>
     * The output buffer is cleared before each command, so the returned
     * string contains only the output from this specific command.
     *
     * @param command the command string to execute
     * @param timeout maximum time to wait for the command to complete
     * @return the command output
     * @throws RuntimeException if the command does not complete within the timeout
     */
    String executeCommand(String command, Duration timeout);

    /**
     * Returns the accumulated error output from the REPL session.
     *
     * @return the error output, or an empty string if none
     */
    String getErrorOutput();

    /**
     * Send the exit command and wait for the REPL session to shut down cleanly.
     */
    void exit();
}
