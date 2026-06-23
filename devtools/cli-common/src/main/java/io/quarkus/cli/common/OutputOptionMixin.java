package io.quarkus.cli.common;

import static io.quarkus.devtools.messagewriter.MessageIcons.ERROR_ICON;
import static io.quarkus.devtools.messagewriter.MessageIcons.WARN_ICON;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.quickcli.Ansi;
import io.quarkus.quickcli.CommandLine;
import io.quarkus.quickcli.CommandSpec;
import io.quarkus.quickcli.Help;
import io.quarkus.quickcli.annotations.Option;
import io.quarkus.quickcli.annotations.Spec;

public class OutputOptionMixin implements MessageWriter {

    static final boolean quickcliDebugEnabled = "DEBUG".equalsIgnoreCase(System.getProperty("quickcli.trace"));

    boolean verbose = false;

    @Option(names = { "-e", "--errors" }, description = "Print more context on errors and exceptions.")
    boolean showErrors;

    @Option(names = {
            "--cli-test" }, hidden = true, description = "Manually set output streams for unit test purposes.")
    boolean cliTestMode;

    Path testProjectRoot;

    @Option(names = { "--cli-test-dir" }, hidden = true)
    void setTestProjectRoot(String path) {
        // Allow the starting/project directory to be specified. Used during test.
        testProjectRoot = Paths.get(path).toAbsolutePath();
    }

    @Spec(Spec.Target.MIXEE)
    CommandSpec mixee;

    PrintWriter out;
    PrintWriter err;

    Help.ColorScheme colorScheme() {
        return Help.ColorScheme.DEFAULT;
    }

    public PrintWriter out() {
        PrintWriter o = out;
        if (o == null) {
            o = out = (mixee != null && mixee.commandLine() != null)
                    ? mixee.commandLine().getOut()
                    : new PrintWriter(System.out, true);
        }
        return o;
    }

    public PrintWriter err() {
        PrintWriter e = err;
        if (e == null) {
            e = err = (mixee != null && mixee.commandLine() != null)
                    ? mixee.commandLine().getErr()
                    : new PrintWriter(System.err, true);
        }
        return e;
    }

    public boolean isShowErrors() {
        return showErrors || quickcliDebugEnabled;
    }

    private static OutputOptionMixin getOutput(CommandSpec commandSpec) {
        return ((OutputProvider) commandSpec.root().userObject()).getOutput();
    }

    @Option(names = { "--verbose" }, description = "Verbose mode.")
    void setVerbose(boolean verbose) {
        getOutput(mixee).verbose = verbose;
    }

    public boolean getVerbose() {
        return getOutput(mixee).verbose;
    }

    public boolean isVerbose() {
        return getVerbose() || quickcliDebugEnabled;
    }

    public boolean isCliTest() {
        return cliTestMode;
    }

    public boolean isAnsiEnabled() {
        return Ansi.AUTO.enabled();
    }

    public void printText(String... text) {
        for (String line : text) {
            out().println(new Ansi.Text(line, null));
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
        out().println(new Ansi.Text(msg, null));
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
            out().println(new Ansi.Text("@|faint [DEBUG] " + msg + "|@", null));
        }
    }

    @Override
    public void warn(String msg) {
        out().println(new Ansi.Text("@|yellow " + WARN_ICON + " " + msg + "|@", null));
    }

    // CommandLine must be passed in (forwarded commands)
    public void throwIfUnmatchedArguments(CommandLine cmd) {
        List<String> unmatchedArguments = cmd.getUnmatchedArguments();
        if (!unmatchedArguments.isEmpty()) {
            throw new CommandLine.UnmatchedArgumentException("Unmatched arguments: " + unmatchedArguments);
        }
    }

    public int handleCommandException(Exception ex, String message) {
        CommandLine cmd = mixee.commandLine();
        printStackTrace(ex);
        if (ex instanceof CommandLine.ParameterException) {
            // suggestion printing not supported
        }
        error(message);

        if (!isShowErrors()) {
            info("\nAdd the -e/--errors option to get more information about the error. Add the --verbose option to get even more details.");
        }

        return cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : mixee.exitCodeOnInvalidInput();
    }

    @Override
    public String toString() {
        return "OutputOptions [testMode=" + cliTestMode
                + ", showErrors=" + showErrors
                + ", verbose=" + getVerbose() + "]";
    }

}
