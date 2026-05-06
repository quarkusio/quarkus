package io.quarkus.quickcli.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.quickcli.ScopeType;

/**
 * Marks a class as a CLI command. The annotation processor will generate
 * a CommandModel at compile time, eliminating the need for runtime reflection.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {

    /** The name of this command. Defaults to the class name in lowercase. */
    String name() default "";

    /** Description shown in help output. */
    String[] description() default {};

    /** Subcommand classes. */
    Class<?>[] subcommands() default {};

    /** Version strings shown when --version is requested. */
    String[] version() default {};

    /** If true, adds --help and --version options automatically. */
    boolean mixinStandardHelpOptions() default false;

    /** Custom version provider class. */
    Class<? extends io.quarkus.quickcli.VersionProvider> versionProvider() default io.quarkus.quickcli.VersionProvider.NoVersionProvider.class;

    /** Header lines shown before the description in help output. */
    String[] header() default {};

    /** Footer lines shown after the description in help output. */
    String[] footer() default {};

    /** Custom name for the synopsis in help output. */
    String customSynopsis() default "";

    /** Sort options alphabetically in help output. */
    boolean sortOptions() default true;

    /** Scope type for options. INHERIT means options propagate to subcommands. */
    ScopeType scope() default ScopeType.LOCAL;

    /** Whether to show default values in help output. */
    boolean showDefaultValues() default false;

    /** Heading for the command list section in help. */
    String commandListHeading() default "Commands:%n";

    /** Heading for the synopsis section in help. */
    String synopsisHeading() default "Usage: ";

    /** Heading for the option list section in help. */
    String optionListHeading() default "Options:%n";

    /** Heading before the header section. */
    String headerHeading() default "";

    /** Heading for the parameter list section in help. */
    String parameterListHeading() default "%n";

    /** Whether subcommands can be repeated. */
    boolean subcommandsRepeatable() default false;

    /** Aliases for this command. */
    String[] aliases() default {};

    /** Whether to show the end-of-options delimiter (--) in usage help. */
    boolean showEndOfOptionsDelimiterInUsageHelp() default false;

    /** Whether this command is hidden from help output. */
    boolean hidden() default false;

    /** Whether this is a help command (like 'help' or 'completion'). */
    boolean helpCommand() default false;
}
