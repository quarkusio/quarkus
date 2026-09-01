package io.quarkus.test.aesh;

import java.time.Duration;
import java.util.List;

import org.aesh.command.CommandResult;

/**
 * Options for {@link AeshLauncher#execute(String, ExecuteOptions)}.
 * <p>
 * Immutable — fluent methods return new instances.
 *
 * <pre>
 * // Expected failure with custom timeout
 * ExecuteOptions.expecting(CommandResult.FAILURE).timeout(Duration.ofMinutes(2))
 *
 * // Interactive command with pre-canned input
 * ExecuteOptions.defaults().input("y")
 * </pre>
 */
public class ExecuteOptions {

    static final ExecuteOptions DEFAULT = new ExecuteOptions(
            CommandResult.SUCCESS, AeshLauncher.DEFAULT_TIMEOUT, List.of());

    private final CommandResult expectedResult;
    private final Duration timeout;
    private final List<String> input;

    private ExecuteOptions(CommandResult expectedResult, Duration timeout, List<String> input) {
        this.expectedResult = expectedResult;
        this.timeout = timeout;
        this.input = input;
    }

    /**
     * Returns the default options: expects {@link CommandResult#SUCCESS}
     * with the {@link AeshLauncher#DEFAULT_TIMEOUT default timeout} and
     * no pre-canned input.
     */
    public static ExecuteOptions defaults() {
        return DEFAULT;
    }

    /**
     * Returns options that expect the given {@link CommandResult}.
     *
     * @param result the expected command result
     */
    public static ExecuteOptions expecting(CommandResult result) {
        return new ExecuteOptions(result, AeshLauncher.DEFAULT_TIMEOUT, List.of());
    }

    /**
     * Returns a copy with the given timeout.
     *
     * @param timeout maximum time to wait for the command to complete
     */
    public ExecuteOptions timeout(Duration timeout) {
        return new ExecuteOptions(this.expectedResult, timeout, this.input);
    }

    /**
     * Returns a copy with pre-canned input for interactive commands.
     * <p>
     * The responses are loaded into aesh's input line queue and consumed
     * by {@code invocation.inputLine()} calls within the command. Each
     * response corresponds to one {@code inputLine()} call, in order.
     *
     * @param responses the input lines for each {@code inputLine()} call
     */
    public ExecuteOptions input(String... responses) {
        return new ExecuteOptions(this.expectedResult, this.timeout, List.of(responses));
    }

    public CommandResult expectedResult() {
        return expectedResult;
    }

    public Duration timeout() {
        return timeout;
    }

    public List<String> input() {
        return input;
    }
}
