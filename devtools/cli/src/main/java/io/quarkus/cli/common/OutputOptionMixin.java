package io.quarkus.cli.common;

import static io.quarkus.devtools.messagewriter.MessageIcons.ERROR_ICON;
import static io.quarkus.devtools.messagewriter.MessageIcons.WARN_ICON;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.quarkus.devtools.messagewriter.MessageWriter;
import picocli.CommandLine;
import picocli.CommandLine.Help.ColorScheme;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

public class OutputOptionMixin implements MessageWriter {

    static final boolean picocliDebugEnabled = "DEBUG".equalsIgnoreCase(System.getProperty("picocli.trace"));

    @CommandLine.Option(names = { "-e", "--errors" }, description = "Display error messages.")
    boolean showErrors;

    @CommandLine.Option(names = { "--verbose" }, description = "Verbose mode.")
    boolean verbose;

    @CommandLine.Option(names = {
            "--cli-test" }, hidden = true, description = "Manually set output streams for unit test purposes.")
    boolean cliTestMode;

    Path testProjectRoot;

    @CommandLine.Option(names = { "--cli-test-dir" }, hidden = true)
    void setTestProjectRoot(String path) {
        // Allow the starting/project directory to be specified. Used during test.
        testProjectRoot = Paths.get(path).toAbsolutePath();
    }

    @Spec(Spec.Target.MIXEE)
    CommandSpec mixee;

    ColorScheme scheme;
    PrintWriter out;
    PrintWriter err;

    ColorScheme colorScheme() {
        ColorScheme colors = scheme;
        if (colors == null) {
            colors = scheme = mixee.commandLine().getColorScheme();
        }
        return colors;
    }

    public PrintWriter out() {
        PrintWriter o = out;
        if (o == null) {
            o = out = mixee.commandLine().getOut();
        }
        return o;
    }

    public PrintWriter err() {
        PrintWriter e = err;
        if (e == null) {
            e = err = mixee.commandLine().getErr();
        }
        return e;
    }

    public boolean isShowErrors() {
        return showErrors || picocliDebugEnabled;
    }

    public boolean isVerbose() {
        return verbose || picocliDebugEnabled;
    }

    public boolean isCliTest() {
        return cliTestMode;
    }

    public boolean isAnsiEnabled() {
        return CommandLine.Help.Ansi.AUTO.enabled();
    }

    public void printText(String[] text) {
        for (String line : text) {
            out().println(colorScheme().ansi().new Text(line, colorScheme()));
        }
    }

    public void printErrorText(String[] text) {
        for (String line : text) {
            err().println(colorScheme().errorText(line));
        }
    }

    public void printStackTrace(Exception ex) {
        if (isShowErrors()) {
            err().println(colorScheme().stackTraceText(ex));
        }
    }

    public Path getTestDirectory() {
        if (isCliTest()) {
            return testProjectRoot;
        }
        return null;
    }

    @Override
    public void info(String msg) {
        out().println(colorScheme().ansi().new Text(msg, colorScheme()));
    }

    @Override
    public void error(String msg) {
        out().println(colorScheme().errorText(ERROR_ICON + " " + msg));
    }

    @Override
    public boolean isDebugEnabled() {
        return isVerbose();
    }

    @Override
    public void debug(String msg) {
        if (isVerbose()) {
            out().println(colorScheme().ansi().new Text("[DEBUG] " + msg, colorScheme()));
        }
    }

    @Override
    public void warn(String msg) {
        out().println(colorScheme().ansi().new Text(WARN_ICON + " " + msg, colorScheme()));
    }

    // CommandLine must be passed in (forwarded commands)
    public void throwIfUnmatchedArguments(CommandLine cmd) {
        List<String> unmatchedArguments = cmd.getUnmatchedArguments();
        if (!unmatchedArguments.isEmpty()) {
            throw new CommandLine.UnmatchedArgumentException(cmd, unmatchedArguments);
        }
    }

    public int handleCommandException(Exception ex, String message) {
        CommandLine cmd = mixee.commandLine();
        printStackTrace(ex);
        if (ex instanceof CommandLine.ParameterException) {
            CommandLine.UnmatchedArgumentException.printSuggestions((CommandLine.ParameterException) ex, out());
        }
        error(message);
        return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : mixee.exitCodeOnInvalidInput();
    }

    @Override
    public String toString() {
        return "OutputOptions [testMode=" + cliTestMode
                + ", showErrors=" + showErrors
                + ", verbose=" + verbose + "]";
    }

}
