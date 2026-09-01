package io.quarkus.test.aesh;

import java.time.Duration;

import org.aesh.command.CommandResult;

import io.quarkus.test.junit.main.LaunchResult;

/**
 * Test utility for interacting with an Aesh REPL (console mode) application.
 * <p>
 * Injected as a method parameter in {@code @QuarkusMainTest} tests. Starts the
 * REPL on a background thread and allows sending commands and asserting on
 * their output.
 */
public interface AeshLauncher extends AutoCloseable {

    /**
     * Default timeout for {@link #execute(String)}.
     */
    Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Start the REPL session on a background thread.
     * Non-blocking -- returns immediately after the console is ready.
     * <p>
     * Calling this method is optional — the REPL is auto-launched on the
     * first {@link #execute} call if not already started. Use this
     * method only when you need to pass initial arguments.
     *
     * @param args optional initial arguments
     */
    void launch(String... args);

    /**
     * Send a command to the REPL, wait for it to complete, and assert that
     * the command succeeds ({@link CommandResult#SUCCESS}).
     * Uses the {@link #DEFAULT_TIMEOUT default timeout} of 30 seconds.
     * <p>
     * Arguments containing spaces or special characters should be quoted
     * the same way as in a terminal (single or double quotes).
     *
     * @param command the command string to execute
     * @return the raw command output (includes prompt and echo)
     * @throws AssertionError if the command does not return SUCCESS
     * @throws RuntimeException if the command does not complete within the timeout
     */
    default String execute(String command) {
        return execute(command, ExecuteOptions.defaults());
    }

    /**
     * Send a command to the REPL, wait for it to complete, and assert that
     * the command returns the expected {@link CommandResult}.
     * Uses the {@link #DEFAULT_TIMEOUT default timeout}.
     *
     * @param command the command string to execute
     * @param expectedResult the expected command result
     * @return the raw command output (includes prompt and echo)
     * @throws AssertionError if the exit code does not match
     * @throws RuntimeException if the command does not complete within the timeout
     */
    default String execute(String command, CommandResult expectedResult) {
        return execute(command, ExecuteOptions.expecting(expectedResult));
    }

    /**
     * Send a command to the REPL, wait for it to complete, and assert the
     * result according to the given {@link ExecuteOptions}.
     *
     * @param command the command string to execute
     * @param options execution options (expected result, timeout, pre-canned input)
     * @return the raw command output (includes prompt and echo)
     * @throws AssertionError if the exit code does not match the expected result
     * @throws RuntimeException if the command does not complete within the timeout
     */
    String execute(String command, ExecuteOptions options);

    /**
     * Send raw input to the REPL without waiting for command completion.
     * Use this for complex interactive scenarios where {@link ExecuteOptions#input}
     * is not sufficient. A newline is appended automatically.
     * <p>
     * For most interactive commands, prefer using
     * {@code ExecuteOptions.defaults().input("response")} which provides
     * pre-canned responses via aesh's input line queue.
     *
     * @param input the text to send
     */
    void sendInput(String input);

    /**
     * Returns the exit code from the last {@link #execute} call.
     * 0 indicates success.
     *
     * @return the last exit code
     */
    default int getLastExitCode() {
        return 0;
    }

    /**
     * @deprecated Use {@link #execute(String)} instead.
     */
    @Deprecated(since = "4.0", forRemoval = true)
    default String executeCommand(String command) {
        return execute(command);
    }

    /**
     * @deprecated Use {@link #execute(String, ExecuteOptions)} instead.
     */
    @Deprecated(since = "4.0", forRemoval = true)
    default String executeCommand(String command, Duration timeout) {
        return execute(command, ExecuteOptions.defaults().timeout(timeout));
    }

    /**
     * Returns the clean command output from the last {@link #execute} call.
     * Contains only what the command wrote via {@code invocation.println()} —
     * no prompt, no command echo, no readline chrome.
     *
     * @return the last command's output, or an empty string
     */
    default String getCommandOutput() {
        return "";
    }

    /**
     * Returns all accumulated clean command output since {@link #launch()} or
     * the last {@link #resetOutput()} call. Contains only command output,
     * no prompt or readline chrome.
     *
     * @return the accumulated output
     */
    default String getOutput() {
        return "";
    }

    /**
     * Clears the accumulated output buffer returned by {@link #getOutput()}.
     */
    default void resetOutput() {
    }

    /**
     * Returns the accumulated error output from the REPL session.
     *
     * @return the error output, or an empty string if none
     */
    String getErrorOutput();

    /**
     * Returns the exception from the last failed command, or {@code null}
     * if the last command succeeded.
     *
     * @return the last error, or null
     */
    default Throwable getLastError() {
        return null;
    }

    /**
     * Returns the launch result after the REPL exits, or {@code null} if
     * the REPL is still running.
     *
     * @return the launch result, or null
     */
    default LaunchResult getLaunchResult() {
        return null;
    }

    /**
     * Returns whether the REPL session is still running.
     *
     * @return true if the REPL thread is alive
     */
    default boolean isRunning() {
        return false;
    }

    /**
     * Wait for the REPL session to terminate, with a timeout.
     * Useful for tests that verify a command causes the REPL to exit.
     *
     * @param timeout maximum time to wait
     * @return true if the REPL exited within the timeout, false if it timed out
     */
    default boolean waitForExit(Duration timeout) {
        return true;
    }

    /**
     * Send the exit command and wait for the REPL session to shut down cleanly.
     */
    void exit();

    /**
     * Closes the launcher, exiting the REPL if still running.
     * Called automatically after each test method.
     */
    @Override
    default void close() {
        if (isRunning()) {
            exit();
        }
    }

}
