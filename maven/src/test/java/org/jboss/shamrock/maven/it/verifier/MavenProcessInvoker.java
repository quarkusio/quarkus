package org.jboss.shamrock.maven.it.verifier;

import org.apache.maven.shared.invoker.*;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.shared.utils.cli.StreamConsumer;
import org.apache.maven.shared.utils.cli.StreamPumper;

import java.io.File;
import java.io.PrintStream;
import java.util.stream.Collectors;

/**
 * An implementation of {@link DefaultInvoker} launching Maven, but does not wait for the termination of the process.
 * The launched process is passed in the {@link InvocationResult}.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class MavenProcessInvoker extends DefaultInvoker {

    private static final InvocationOutputHandler DEFAULT_OUTPUT_HANDLER = new SystemOutHandler();

    private InvocationOutputHandler outputHandler = DEFAULT_OUTPUT_HANDLER;

    private InvocationOutputHandler errorHandler = DEFAULT_OUTPUT_HANDLER;

    @Override
    public InvocationResult execute(InvocationRequest request) throws MavenInvocationException {
        MavenCommandLineBuilder cliBuilder = new MavenCommandLineBuilder();

        InvokerLogger logger = getLogger();
        if (logger != null) {
            cliBuilder.setLogger(getLogger());
        }

        File localRepo = getLocalRepositoryDirectory();
        if (localRepo != null) {
            cliBuilder.setLocalRepositoryDirectory(getLocalRepositoryDirectory());
        }

        File mavenHome = getMavenHome();
        if (mavenHome != null) {
            cliBuilder.setMavenHome(getMavenHome());
        }

        File mavenExecutable = getMavenExecutable();
        if (mavenExecutable != null) {
            cliBuilder.setMavenExecutable(mavenExecutable);
        }

        File workingDirectory = getWorkingDirectory();
        if (workingDirectory != null) {
            cliBuilder.setWorkingDirectory(getWorkingDirectory());
        }

        request.setBatchMode(true);

        Commandline cli;
        try {
            cli = cliBuilder.build(request);
        } catch (CommandLineConfigurationException e) {
            throw new MavenInvocationException("Error configuring command-line. Reason: " + e.getMessage(), e);
        }

        MavenProcessInvocationResult result = new MavenProcessInvocationResult();

        try {
            Process process = executeCommandLine(cli, request);
            result.setProcess(process);
        } catch (CommandLineException e) {
            result.setException(e);
        }

        return result;
    }

    private Process executeCommandLine(Commandline cli, InvocationRequest request)
            throws CommandLineException {
        InvocationOutputHandler outputHandler = request.getOutputHandler(this.outputHandler);
        InvocationOutputHandler errorHandler = request.getErrorHandler(this.errorHandler);
        return executeCommandLine(cli, outputHandler, errorHandler);
    }


    private static Process executeCommandLine(Commandline cl, StreamConsumer systemOut, StreamConsumer systemErr)
            throws CommandLineException {
        if (cl == null) {
            throw new IllegalArgumentException("the command line cannot be null.");
        } else {
            final Process p = cl.execute();
            final StreamPumper outputPumper = new StreamPumper(p.getInputStream(), systemOut);
            final StreamPumper errorPumper = new StreamPumper(p.getErrorStream(), systemErr);

            outputPumper.start();
            errorPumper.start();

            new Thread(() -> {
                try {
                    // Wait for termination
                    p.waitFor();
                    outputPumper.waitUntilDone();
                    errorPumper.waitUntilDone();
                } catch (Exception e) {
                    outputPumper.disable();
                    errorPumper.disable();
                    e.printStackTrace();
                }
            }).start();

            return p;
        }
    }
}
