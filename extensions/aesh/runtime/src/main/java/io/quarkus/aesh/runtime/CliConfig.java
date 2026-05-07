package io.quarkus.aesh.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * CLI configuration for the Aesh extension.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.aesh")
public interface CliConfig {

    /**
     * Name of bean annotated with {@link io.quarkus.aesh.runtime.annotations.TopCommand}
     * or FQCN of class which will be used as entry point for Aesh CommandRunner instance.
     * This class needs to be annotated with {@link org.aesh.command.Command}.
     */
    Optional<String> topCommand();

    /**
     * The prompt to display in console mode.
     * Only used when mode is set to 'console' or auto-detected as console.
     */
    @WithDefault("[quarkus]$ ")
    String prompt();

    /**
     * Whether to add a built-in 'exit' command in console mode.
     * Only used when mode is set to 'console' or auto-detected as console.
     */
    @WithDefault("true")
    boolean addExitCommand();

    /**
     * Enable command aliasing.
     * When enabled, users can create aliases for commands.
     */
    @WithDefault("false")
    boolean enableAlias();

    /**
     * Enable the export command.
     * When enabled, the 'export' command is available for setting environment variables.
     */
    @WithDefault("false")
    boolean enableExport();

    /**
     * Enable man page support.
     * When enabled, the 'man' command is available for viewing command documentation.
     */
    @WithDefault("false")
    boolean enableMan();

    /**
     * Enable command history persistence.
     * When enabled, command history is saved to a file between sessions.
     */
    @WithDefault("false")
    boolean persistHistory();

    /**
     * Path to the history file.
     * Only used when persistHistory is enabled.
     * If not specified, defaults to ".aesh_history" in the user's home directory.
     */
    Optional<String> historyFile();

    /**
     * Maximum number of history entries to keep.
     */
    @WithDefault("500")
    int historySize();

    /**
     * Enable logging of aesh internal operations.
     */
    @WithDefault("false")
    boolean logging();

    /**
     * Sub-command mode configuration.
     * Sub-command mode allows users to enter an interactive context for group commands.
     */
    SubCommandModeConfig subCommandMode();

    /**
     * Configuration for sub-command mode.
     */
    interface SubCommandModeConfig {

        /**
         * Enable sub-command mode.
         * When enabled, typing a group command without a subcommand enters an interactive context.
         * <p>
         * Example:
         *
         * <pre>
         * [quarkus]$ module --name mymodule
         * Entering module mode.
         * module[mymodule]> tag add v1.0
         * module[mymodule]> exit
         * [quarkus]$
         * </pre>
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Command to exit sub-command mode and return to the parent context.
         */
        @WithDefault("exit")
        String exitCommand();

        /**
         * Alternative command to exit sub-command mode (e.g., "..").
         * Set to empty string to disable.
         */
        @WithDefault("..")
        String alternativeExitCommand();

        /**
         * Separator used for nested context paths in the prompt.
         * For example, with separator ":", nested contexts appear as "module:project>"
         */
        @WithDefault(":")
        String contextSeparator();

        /**
         * Show option/argument values when entering sub-command mode.
         */
        @WithDefault("true")
        boolean showContextOnEntry();

        /**
         * Show the primary argument value in the prompt.
         * For example, "module[myapp]>" vs "module>"
         */
        @WithDefault("true")
        boolean showArgumentInPrompt();
    }
}
