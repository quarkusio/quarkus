package io.quarkus.aesh.runtime;

/**
 * Execution mode for the Aesh extension.
 */
public enum AeshMode {
    /**
     * Automatically detect the mode based on the number and type of commands.
     * <ul>
     * <li>Single command or @GroupCommandDefinition: uses runtime mode</li>
     * <li>Multiple independent commands: uses console mode</li>
     * </ul>
     */
    auto,

    /**
     * Use AeshRuntimeRunner for single command execution (like picocli).
     * The application executes one command and exits.
     * This mode is ideal for CLI tools that perform a single operation.
     */
    runtime,

    /**
     * Use AeshConsoleRunner for interactive shell mode.
     * Starts a REPL (Read-Eval-Print Loop) where users can type multiple commands.
     * This mode is ideal for interactive CLI applications with multiple commands.
     */
    console
}
