package io.quarkus.quickcli;

import io.quarkus.quickcli.model.CommandModel;
import io.quarkus.quickcli.model.CommandModelRegistry;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Main entry point for QuickCLI. Creates a command line parser from a
 * pre-generated command model (no runtime annotation scanning).
 *
 * <p>Usage:</p>
 * <pre>
 * int exitCode = new CommandLine(MyCommand.class).execute(args);
 * System.exit(exitCode);
 * </pre>
 */
public final class CommandLine {

    /** QuickCLI version string. */
    public static final String VERSION = "1.0.0";

    private final CommandSpec spec;
    private final CommandModel model;
    private final io.quarkus.quickcli.Factory factory;
    private final Map<String, CommandLine> subcommands = new LinkedHashMap<>();
    private PrintStream out = System.out;
    private PrintStream err = System.err;
    private PrintWriter outWriter;
    private PrintWriter errWriter;
    private ExecutionStrategy executionStrategy = new RunLastStrategy();
    private ParameterExceptionHandler parameterExceptionHandler;
    private ExitCodeExceptionMapper exitCodeExceptionMapper;
    private final Map<String, HelpSectionRenderer> helpSectionMap = new LinkedHashMap<>();
    private final Help.ColorScheme colorScheme = Help.ColorScheme.DEFAULT;
    private ParseResult lastParseResult;

    /**
     * Creates a CommandLine with the default factory.
     */
    public CommandLine(Class<?> commandClass) {
        this(commandClass, new io.quarkus.quickcli.Factory.DefaultFactory());
    }

    /**
     * Creates a CommandLine with a custom factory.
     */
    public CommandLine(Class<?> commandClass, io.quarkus.quickcli.Factory factory) {
        this.factory = factory;
        this.model = CommandModelRegistry.getModel(commandClass);
        this.spec = model.buildSpec();
        this.spec.setCommandLine(this);
        resolveSubcommandModels(this.spec);
        buildSubcommandMap();
    }

    /**
     * Creates a CommandLine from an existing command instance (for pre-created objects).
     */
    public CommandLine(Object commandInstance) {
        this(commandInstance, new io.quarkus.quickcli.Factory.DefaultFactory());
    }

    /**
     * Creates a CommandLine from an existing command instance with a custom factory.
     * Initializes the instance's mixins, arg groups, and spec so that error handlers
     * can access them even if parsing fails.
     */
    public CommandLine(Object commandInstance, io.quarkus.quickcli.Factory factory) {
        this(commandInstance.getClass(), factory);
        // Pre-initialize the instance so fields like @Mixin output are available
        // to error handlers even when parsing fails
        if (this.model != null) {
            try {
                this.model.initMixins(commandInstance, factory);
                this.model.initArgGroups(commandInstance, factory);
                this.model.injectSpec(commandInstance, this.spec);
                this.spec.setCommandInstance(commandInstance);
            } catch (Exception e) {
                throw new RuntimeException("QuickCLI: failed to pre-initialize command " + commandInstance.getClass().getName(), e);
            }
        }
    }

    /**
     * Creates a CommandLine from a pre-built CommandSpec (for dynamic commands
     * like plugins that are not annotation-processed).
     */
    public CommandLine(CommandSpec spec) {
        this(spec, new io.quarkus.quickcli.Factory.DefaultFactory());
    }

    /**
     * Creates a CommandLine from a pre-built CommandSpec with a custom factory.
     * Preserves the spec's existing parent chain (unlike the Class-based constructor
     * which builds a new spec hierarchy).
     */
    public CommandLine(CommandSpec spec, io.quarkus.quickcli.Factory factory) {
        this.factory = factory;
        // For pre-built specs (e.g., plugin commands), the model may not exist.
        // This is fine — the spec is already fully constructed.
        CommandModel m = null;
        try {
            m = CommandModelRegistry.getModel(spec.commandClass());
        } catch (IllegalStateException ignored) {
            // No model for dynamic/plugin commands — that's expected
        }
        this.model = m;
        this.spec = spec;
        this.spec.setCommandLine(this);
    }

    /**
     * Set the output stream for help and version output.
     */
    public CommandLine setOut(PrintStream out) {
        this.out = out;
        return this;
    }

    /**
     * Set the error stream.
     */
    public CommandLine setErr(PrintStream err) {
        this.err = err;
        return this;
    }

    /** Get the output writer. */
    public PrintWriter getOut() {
        if (outWriter == null) {
            outWriter = new PrintWriter(out, true);
        }
        return outWriter;
    }

    /** Get the error writer. */
    public PrintWriter getErr() {
        if (errWriter == null) {
            errWriter = new PrintWriter(err, true);
        }
        return errWriter;
    }

    /** Get the color scheme. */
    public Help.ColorScheme getColorScheme() {
        return colorScheme;
    }

    /** Get the last parse result from execute() or parseArgs(). */
    public ParseResult getParseResult() {
        return lastParseResult;
    }

    /** Get unmatched arguments from the last parse. */
    public List<String> getUnmatchedArguments() {
        return lastParseResult != null ? lastParseResult.unmatched() : Collections.emptyList();
    }

    /**
     * Set the execution strategy.
     */
    public CommandLine setExecutionStrategy(ExecutionStrategy strategy) {
        this.executionStrategy = strategy;
        return this;
    }

    /**
     * Get the command specification.
     */
    public CommandSpec getCommandSpec() {
        return spec;
    }

    /** Get the command name. */
    public String getCommandName() {
        return spec.name();
    }

    /** Get the subcommands map (name -> CommandLine). */
    public Map<String, CommandLine> getSubcommands() {
        return Collections.unmodifiableMap(subcommands);
    }

    /** Add a subcommand at runtime (e.g., for plugin systems). */
    public CommandLine addSubcommand(String name, CommandLine subcommandLine) {
        subcommands.put(name, subcommandLine);
        spec.addSubcommand(name, subcommandLine.getCommandSpec());
        return this;
    }

    /** Add a subcommand at runtime from a CommandSpec (e.g., for plugin systems). */
    public CommandLine addSubcommand(String name, CommandSpec subSpec) {
        CommandLine subCmdLine = new CommandLine(subSpec);
        return addSubcommand(name, subCmdLine);
    }

    /** Get the help section map for customization. */
    public Map<String, HelpSectionRenderer> getHelpSectionMap() {
        return helpSectionMap;
    }

    /** Set the parameter exception handler. */
    public CommandLine setParameterExceptionHandler(ParameterExceptionHandler handler) {
        this.parameterExceptionHandler = handler;
        return this;
    }

    /** Get the exit code exception mapper. */
    public ExitCodeExceptionMapper getExitCodeExceptionMapper() {
        return exitCodeExceptionMapper;
    }

    /** Set the exit code exception mapper. */
    public CommandLine setExitCodeExceptionMapper(ExitCodeExceptionMapper mapper) {
        this.exitCodeExceptionMapper = mapper;
        return this;
    }

    /** Get a Help object for this command. */
    public Help getHelp() {
        return new Help(spec);
    }

    /** Print usage/help to the given output stream. */
    public void usage(PrintStream out) {
        HelpFormatter.printHelp(spec, out);
    }

    /** Print usage/help to the given writer. */
    public void usage(PrintWriter out) {
        HelpFormatter.printHelp(spec, out);
    }

    /**
     * Parse and execute the command with the given arguments.
     *
     * @return exit code (0 for success)
     */
    public int execute(String... args) {
        try {
            ParseResult result = parse(args);
            return executionStrategy.execute(result, this);
        } catch (ParameterException e) {
            if (parameterExceptionHandler != null) {
                return parameterExceptionHandler.handleParseException(e, args);
            }
            err.println("Error: " + e.getMessage());
            err.println("Try '" + spec.qualifiedName() + " --help' for more information.");
            return ExitCode.USAGE;
        } catch (ExecutionException e) {
            err.println("Error executing command: " + e.getCause().getMessage());
            return ExitCode.SOFTWARE;
        } catch (Exception e) {
            err.println("Unexpected error: " + e.getMessage());
            return ExitCode.SOFTWARE;
        }
    }

    /**
     * Parse arguments without executing the command.
     * Alias for {@link #parse(String...)} for picocli compatibility.
     */
    public ParseResult parseArgs(String... args) {
        return parse(args);
    }

    /**
     * Parse arguments without executing the command.
     */
    public ParseResult parse(String... args) {
        ParseResult result = parseArgsInternal(spec, Arrays.asList(args), 0);
        this.lastParseResult = result;
        return result;
    }

    /**
     * For negatable options, determine the value when the --no-xxx (negative) form is used.
     * Picocli behavior: negate the effective default value.
     * - defaultValue="false" → returns "true" (negate false)
     * - defaultValue="true" or null → returns "false" (negate true, or standard negative)
     */
    private static String negatableValueForNegativeForm(OptionSpec option) {
        String defaultValue = option.defaultValue();
        if ("false".equals(defaultValue)) {
            return "true"; // negate "false" → "true"
        }
        return "false"; // standard negative form
    }

    /**
     * For negatable options, determine the value when the --xxx (positive) form is used.
     * Picocli behavior: affirm the effective default value.
     * - defaultValue="false" → returns "false" (affirm false)
     * - defaultValue="true" or null → returns "true" (affirm true, or standard positive)
     */
    private static String negatableValueForPositiveForm(OptionSpec option) {
        String defaultValue = option.defaultValue();
        if ("false".equals(defaultValue)) {
            return "false"; // affirm "false"
        }
        return "true"; // standard positive form
    }

    private ParseResult parseArgsInternal(CommandSpec currentSpec, List<String> args, int startIndex) {
        ParseResult result = new ParseResult(currentSpec, args);
        boolean endOfOptions = false;
        boolean hasUnmatched = currentSpec.hasUnmatchedField();

        for (int i = startIndex; i < args.size(); i++) {
            String arg = args.get(i);

            if ("--".equals(arg)) {
                endOfOptions = true;
                continue;
            }

            // Check for help/version flags (always handle -h/--help)
            if (!endOfOptions) {
                if ("-h".equals(arg) || "--help".equals(arg)) {
                    result.setHelpRequested(true);
                    return result;
                }
                if (currentSpec.mixinStandardHelpOptions()
                        && ("-V".equals(arg) || "--version".equals(arg))) {
                    result.setVersionRequested(true);
                    return result;
                }
            }

            // Check for usageHelp / versionHelp options
            if (!endOfOptions) {
                OptionSpec specialOption = currentSpec.findOption(arg);
                if (specialOption != null && specialOption.isVersionHelp()) {
                    result.setVersionRequested(true);
                    result.addOptionValue(specialOption.longestName(), "true");
                    return result;
                }
                if (specialOption != null && specialOption.isUsageHelp()) {
                    result.setHelpRequested(true);
                    result.addOptionValue(specialOption.longestName(), "true");
                    return result;
                }
            }

            // Check for subcommand
            if (!endOfOptions && currentSpec.subcommands().containsKey(arg)) {
                CommandSpec subSpec = currentSpec.subcommands().get(arg);
                ParseResult subResult = parseArgsInternal(subSpec, args, i + 1);
                result.setSubcommandResult(subResult);
                // Store parse result on the subcommand's CommandLine
                if (subSpec.commandLine() != null) {
                    subSpec.commandLine().lastParseResult = subResult;
                }
                return result;
            }

            // Check for option
            if (!endOfOptions && arg.startsWith("-")) {
                String optionName = arg;
                String inlineValue = null;

                // Handle --option=value syntax
                int eqIndex = arg.indexOf('=');
                if (eqIndex > 0) {
                    optionName = arg.substring(0, eqIndex);
                    inlineValue = arg.substring(eqIndex + 1);
                }

                OptionSpec option = currentSpec.findOption(optionName);
                // Try prefix matching for attached-value options (e.g., -Dkey=value)
                if (option == null && optionName.length() > 2 && optionName.startsWith("-")
                        && !optionName.startsWith("--")) {
                    // Try the first two chars as the option name (e.g., "-D" from "-Dkey")
                    String prefix = optionName.substring(0, 2);
                    OptionSpec prefixOption = currentSpec.findOption(prefix);
                    if (prefixOption != null && !prefixOption.isBoolean()) {
                        option = prefixOption;
                        // The rest of the original arg (after the prefix) is the value
                        String attached = arg.substring(2);
                        inlineValue = attached;
                    }
                }
                // Try negatable option matching
                boolean negated = false;
                if (option == null && optionName.startsWith("--no-")) {
                    // --no-foo → try --foo (negatable=true)
                    String positiveForm = "--" + optionName.substring(5);
                    OptionSpec negatableOption = currentSpec.findOption(positiveForm);
                    if (negatableOption != null && negatableOption.isNegatable()) {
                        option = negatableOption;
                        negated = true;
                        inlineValue = negatableValueForNegativeForm(negatableOption);
                    }
                }
                if (option == null && optionName.startsWith("--") && !optionName.startsWith("--no-")) {
                    // --foo → try --no-foo (negatable=true, positive form of negated option)
                    String negativeForm = "--no-" + optionName.substring(2);
                    OptionSpec negatableOption = currentSpec.findOption(negativeForm);
                    if (negatableOption != null && negatableOption.isNegatable()) {
                        option = negatableOption;
                        negated = true;
                        inlineValue = negatableValueForPositiveForm(negatableOption);
                    }
                }
                if (option == null) {
                    if (hasUnmatched) {
                        result.addUnmatched(arg);
                        continue;
                    }
                    throw new ParameterException(
                            "Unknown option: '" + optionName + "'", this);
                }

                if (option.isBoolean() && !option.isVersionHelp()) {
                    if (inlineValue != null) {
                        result.addOptionValue(option.longestName(), inlineValue);
                    } else if (option.isNegatable() && optionName.startsWith("--no-")) {
                        // Negatable option with --no- prefix matched directly
                        result.addOptionValue(option.longestName(), negatableValueForNegativeForm(option));
                    } else {
                        result.addOptionValue(option.longestName(), "true");
                    }
                } else {
                    String value;
                    if (inlineValue != null) {
                        value = inlineValue;
                    } else if (i + 1 < args.size()) {
                        value = args.get(++i);
                    } else {
                        throw new ParameterException(
                                "Option " + optionName + " requires a value", this);
                    }
                    result.addOptionValue(option.longestName(), value);
                }
            } else {
                if (hasUnmatched && !endOfOptions) {
                    // For commands with @Unmatched, non-option args may also be unmatched
                    // But only if there are no positional parameters defined
                    if (currentSpec.parameters().isEmpty()) {
                        result.addUnmatched(arg);
                    } else {
                        result.addPositionalValue(arg);
                    }
                } else {
                    // Positional parameter
                    result.addPositionalValue(arg);
                }
            }
        }

        // Validate required options
        for (OptionSpec option : currentSpec.options()) {
            if (option.required() && !result.hasOptionBySpec(option)) {
                throw new ParameterException(
                        "Missing required option: " + option.longestName(), this);
            }
        }

        // Validate exclusive groups
        for (List<String> group : currentSpec.exclusiveGroups()) {
            List<String> matched = new java.util.ArrayList<>();
            for (String optName : group) {
                if (result.hasOption(optName)) {
                    matched.add(optName);
                }
            }
            if (matched.size() > 1) {
                throw new ParameterException(
                        "Error: " + matched.get(0) + " and " + matched.get(1)
                                + " are mutually exclusive (specify only one)", this);
            }
        }

        return result;
    }

    private void resolveSubcommandModels(CommandSpec parentSpec) {
        for (Map.Entry<String, CommandSpec> entry : parentSpec.subcommands().entrySet()) {
            resolveSubcommandModels(entry.getValue());
        }
    }

    private void buildSubcommandMap() {
        buildSubcommandMapForSpec(this);
    }

    private void buildSubcommandMapForSpec(CommandLine parentCmdLine) {
        for (Map.Entry<String, CommandSpec> entry : parentCmdLine.spec.subcommands().entrySet()) {
            String subName = entry.getKey();
            CommandSpec subSpec = entry.getValue();
            // Wrap the existing spec (preserving parent chain) instead of building a new one
            CommandLine subCmdLine = new CommandLine(subSpec, factory);
            parentCmdLine.subcommands.put(subName, subCmdLine);
            subSpec.setCommandLine(subCmdLine);
            // Recurse into sub-subcommands
            buildSubcommandMapForSpec(subCmdLine);
        }
    }

    // --- Inner classes and interfaces ---

    /**
     * Strategy for executing parsed commands.
     */
    @FunctionalInterface
    public interface ExecutionStrategy {
        int execute(ParseResult parseResult, CommandLine commandLine) throws Exception;
    }

    /**
     * Handler for parameter parsing exceptions.
     */
    @FunctionalInterface
    public interface ParameterExceptionHandler {
        int handleParseException(ParameterException ex, String[] args);
    }

    /**
     * Renders a section of the help message.
     */
    @FunctionalInterface
    public interface HelpSectionRenderer {
        String render(Help help);
    }

    /**
     * Maps exceptions to exit codes.
     */
    @FunctionalInterface
    public interface ExitCodeExceptionMapper {
        int getExitCode(Throwable exception);
    }

    /**
     * Version provider interface (re-export for convenience).
     */
    public interface VersionProvider extends io.quarkus.quickcli.VersionProvider {
    }

    /**
     * Factory interface (re-export for convenience).
     */
    public interface Factory extends io.quarkus.quickcli.Factory {
    }

    /**
     * Default strategy: executes the last (most specific) subcommand.
     */
    public static class RunLastStrategy implements ExecutionStrategy {
        @Override
        public int execute(ParseResult parseResult, CommandLine commandLine) throws Exception {
            // Walk to the deepest subcommand
            ParseResult current = parseResult;
            while (current.subcommandResult() != null) {
                current = current.subcommandResult();
            }

            return executeCommand(current, parseResult, commandLine);
        }

        private int executeCommand(ParseResult target, ParseResult root,
                                    CommandLine commandLine) throws Exception {
            CommandSpec spec = target.commandSpec();

            // Handle help
            if (target.isHelpRequested()) {
                HelpFormatter.printHelp(spec, commandLine.out);
                return 0;
            }

            // Handle version
            if (target.isVersionRequested()) {
                String[] versionStrings = spec.version();
                if (versionStrings.length == 0 && spec.versionProviderClass() != null
                        && spec.versionProviderClass() != io.quarkus.quickcli.VersionProvider.NoVersionProvider.class) {
                    try {
                        @SuppressWarnings("unchecked")
                        io.quarkus.quickcli.VersionProvider provider = (io.quarkus.quickcli.VersionProvider) commandLine.factory
                                .create((Class<Object>) (Class<?>) spec.versionProviderClass());
                        versionStrings = provider.getVersion();
                    } catch (Exception e) {
                        commandLine.err.println("Error getting version: " + e.getMessage());
                    }
                }
                for (String v : versionStrings) {
                    commandLine.out.println(v);
                }
                return 0;
            }

            // If this command has subcommands and none was selected, show help
            if (!spec.subcommands().isEmpty() && target.subcommandResult() == null
                    && !isExecutable(spec.commandClass())) {
                HelpFormatter.printHelp(spec, commandLine.out);
                return 0;
            }

            // Create and execute the command
            CommandModel model = null;
            try {
                model = CommandModelRegistry.getModel(spec.commandClass());
            } catch (IllegalStateException e) {
                // Dynamic commands (plugins) may not have a generated model — that's OK
            }

            // Dynamic commands (plugins) may have a pre-set instance and no model
            if (model == null) {
                Object presetInstance = spec.commandInstance();
                if (presetInstance != null) {
                    // Pass unmatched args to the instance if it supports them
                    List<String> unmatched = target.unmatched();
                    if (!unmatched.isEmpty() && presetInstance instanceof UsesArguments ua) {
                        ua.useArguments(unmatched);
                    }
                    return executeInstance(presetInstance);
                }
                throw new ExecutionException(
                        new IllegalStateException("No model for " + spec.commandClass().getName()));
            }

            // Ensure the spec has a CommandLine reference (subcommand specs
            // created during buildSpec() may not have one yet)
            if (spec.commandLine() == null) {
                spec.setCommandLine(commandLine);
            }

            // Reuse pre-initialized instance (set during constructor) or create new
            Object instance = spec.commandInstance();
            if (instance == null) {
                instance = commandLine.factory.create(spec.commandClass());
                model.initMixins(instance, commandLine.factory);
                model.initArgGroups(instance, commandLine.factory);
                model.injectSpec(instance, spec);
            }

            // Set parent command if applicable
            setParentChain(instance, model, root, target, commandLine);

            // Apply parsed values
            model.applyValues(instance, target);

            // Set @Unmatched field
            List<String> unmatched = target.unmatched();
            model.setUnmatched(instance, unmatched);

            // Pass unmatched args to UsesArguments instances (e.g. plugin commands)
            if (!unmatched.isEmpty() && instance instanceof UsesArguments ua) {
                ua.useArguments(unmatched);
            }

            // Execute
            return executeInstance(instance);
        }

        private void setParentChain(Object instance, CommandModel model,
                                     ParseResult root, ParseResult target,
                                     CommandLine commandLine) throws Exception {
            if (root == target) return;

            // Find the parent parse result
            ParseResult parent = root;
            ParseResult child = root;
            while (child != null && child != target) {
                parent = child;
                child = child.subcommandResult();
            }

            if (parent != null) {
                CommandModel parentModel = CommandModelRegistry.getModel(
                        parent.commandSpec().commandClass());
                Object parentInstance = commandLine.factory.create(
                        parent.commandSpec().commandClass());
                parentModel.initMixins(parentInstance, commandLine.factory);
                parentModel.initArgGroups(parentInstance, commandLine.factory);
                parentModel.applyValues(parentInstance, parent);
                model.setParentCommand(instance, parentInstance);
            }
        }

        private boolean isExecutable(Class<?> cls) {
            return Runnable.class.isAssignableFrom(cls)
                    || Callable.class.isAssignableFrom(cls);
        }

        private int executeInstance(Object instance) throws Exception {
            if (instance instanceof Callable<?> callable) {
                Object result = callable.call();
                if (result instanceof Integer exitCode) {
                    return exitCode;
                }
                return 0;
            } else if (instance instanceof Runnable runnable) {
                runnable.run();
                return 0;
            }
            return 0;
        }
    }

    /**
     * Thrown when there's a problem with command-line parameters.
     */
    public static class ParameterException extends RuntimeException {
        private CommandLine commandLine;

        public ParameterException(String message) {
            super(message);
        }

        public ParameterException(String message, CommandLine commandLine) {
            super(message);
            this.commandLine = commandLine;
        }

        public CommandLine getCommandLine() {
            return commandLine;
        }
    }

    /**
     * Thrown when an unmatched argument is encountered (and no @Unmatched field exists).
     */
    public static class UnmatchedArgumentException extends ParameterException {
        public UnmatchedArgumentException(String message) {
            super(message);
        }

        public UnmatchedArgumentException(String message, CommandLine commandLine) {
            super(message, commandLine);
        }

        /** Print suggestions for unmatched arguments. */
        public static void printSuggestions(ParameterException ex, PrintStream stream) {
            // Simple implementation — could be enhanced with fuzzy matching
            if (ex.getCommandLine() != null) {
                CommandSpec spec = ex.getCommandLine().getCommandSpec();
                stream.println("Possible solutions:");
                for (String name : spec.subcommands().keySet()) {
                    stream.println("  " + name);
                }
            }
        }
    }

    /**
     * Thrown when mutually exclusive arguments are used together.
     */
    public static class MutuallyExclusiveArgsException extends ParameterException {
        public MutuallyExclusiveArgsException(String message) {
            super(message);
        }

        public MutuallyExclusiveArgsException(String message, CommandLine commandLine) {
            super(message, commandLine);
        }
    }

    /**
     * Thrown for type conversion errors.
     */
    public static class TypeConversionException extends ParameterException {
        public TypeConversionException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when there's a problem executing a command.
     */
    public static class ExecutionException extends RuntimeException {
        public ExecutionException(Throwable cause) {
            super(cause);
        }
    }
}
