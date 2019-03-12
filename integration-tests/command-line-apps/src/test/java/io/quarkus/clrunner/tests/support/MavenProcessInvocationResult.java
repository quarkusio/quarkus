package io.quarkus.clrunner.tests.support;

import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.utils.cli.CommandLineException;

/*
 * Copied and adapted from devtools-maven
 */
public class MavenProcessInvocationResult implements InvocationResult {

    private Process process;
    private CommandLineException exception;

    MavenProcessInvocationResult setProcess(Process process) {
        this.process = process;
        return this;
    }

    public MavenProcessInvocationResult setException(CommandLineException exception) {
        // Print the stack trace immediately to give some feedback early
        // In intellij, the used `mvn` executable is not "executable" by default on Mac and probably linux.
        // You need to chmod +x the file.
        exception.printStackTrace();
        this.exception = exception;
        return this;
    }

    @Override
    public CommandLineException getExecutionException() {
        return exception;
    }

    @Override
    public int getExitCode() {
        if (process == null) {
            throw new IllegalStateException("No process");
        } else {
            return process.exitValue();
        }
    }

    public Process getProcess() {
        return process;
    }
}
