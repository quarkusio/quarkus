package io.quarkus.maven.it.verifier;

import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.utils.cli.CommandLineException;

/**
 * Result of {@link MavenProcessInvoker#execute(InvocationRequest)}. It keeps a reference on the created process.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class MavenProcessInvocationResult implements InvocationResult {

    private Process process;
    private CommandLineException exception;

    void destroy() {
        if (process != null) {
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

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
