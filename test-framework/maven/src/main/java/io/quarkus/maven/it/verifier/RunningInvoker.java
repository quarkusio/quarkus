package io.quarkus.maven.it.verifier;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;
import org.apache.maven.shared.invoker.PrintStreamLogger;

import io.quarkus.maven.it.MojoTestBase;
import io.quarkus.test.devmode.util.DevModeTestUtils;

/**
 * Implementation of verifier using a forked process that is still running while verifying. The process is stop when
 * {@link RunningInvoker#stop()} is called.
 */
public class RunningInvoker extends MavenProcessInvoker {

    private final boolean parallel;
    private final boolean debug;
    private MavenProcessInvocationResult result;
    private final File log;
    private final PrintStreamHandler outStreamHandler;
    private final PrintStreamHandler errStreamHandler;

    public RunningInvoker(File basedir, boolean debug) {
        this(basedir, debug, false);
    }

    public RunningInvoker(File basedir, boolean debug, boolean parallel) {
        this.parallel = parallel;
        this.debug = debug;
        setWorkingDirectory(basedir);
        String repo = System.getProperty("maven.repo.local");
        if (repo == null) {
            repo = new File(System.getProperty("user.home"), ".m2/repository").getAbsolutePath();
        }
        setLocalRepositoryDirectory(new File(repo));
        log = new File(basedir, "build-" + basedir.getName() + ".log");
        PrintStream outStream;
        try {
            outStream = createTeePrintStream(System.out, Files.newOutputStream(log.toPath()));
        } catch (IOException ioe) {
            outStream = System.out;
        }
        this.outStreamHandler = new PrintStreamHandler(outStream, true);
        setOutputHandler(this.outStreamHandler);

        PrintStream errStream;
        try {
            errStream = createTeePrintStream(System.err, Files.newOutputStream(log.toPath()));
        } catch (IOException ioe) {
            errStream = System.err;
        }
        this.errStreamHandler = new PrintStreamHandler(errStream, true);
        setErrorHandler(this.errStreamHandler);

        setLogger(new PrintStreamLogger(outStream, debug ? InvokerLogger.DEBUG : InvokerLogger.INFO));
    }

    /**
     * Creates a {@link PrintStream} with an underlying {@link TeeOutputStream} composed of {@code one}
     * and {@code two} outputstreams
     *
     * @param one
     * @param two
     * @return
     */
    private static PrintStream createTeePrintStream(final OutputStream one, final OutputStream two) {
        final OutputStream tee = new TeeOutputStream(one, two);
        PrintStream stream;
        try {
            stream = new PrintStream(tee, true, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            stream = new PrintStream(tee, true);
        }
        return stream;
    }

    public void stop() {
        if (result == null) {
            return;
        }
        // Kill all processes that were (indirectly) spawned by the current process.
        // It's important to do it this way (instead of calling result.destroy() first)
        // because otherwise children of that process can become orphaned zombies.
        DevModeTestUtils.killDescendingProcesses();
        // This is now more or less "symbolic" since the previous call should have also killed that result's process.
        result.destroy();
    }

    public MavenProcessInvocationResult execute(List<String> goals, Map<String, String> envVars)
            throws MavenInvocationException {
        return execute(goals, envVars, new Properties());
    }

    public MavenProcessInvocationResult execute(List<String> goals, Map<String, String> envVars, Properties properties)
            throws MavenInvocationException {

        DefaultInvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(goals);
        request.setDebug(debug);
        if (parallel) {
            request.setThreads("1C");
        }
        request.setLocalRepositoryDirectory(getLocalRepositoryDirectory());
        request.setBaseDirectory(getWorkingDirectory());
        request.setPomFile(new File(getWorkingDirectory(), "pom.xml"));
        request.setProperties(properties);

        if (System.getProperty("mavenOpts") != null) {
            request.setMavenOpts(System.getProperty("mavenOpts"));
        } else {
            //we need to limit the memory consumption, as we can have a lot of these processes
            //running at once, if they add default to 75% of total mem we can easily run out
            //of physical memory as they will consume way more than what they need instead of
            //just running GC
            request.setMavenOpts("-Xmx128m");
        }

        request.setShellEnvironmentInherited(true);
        envVars.forEach(request::addShellEnvironment);
        request.setOutputHandler(outStreamHandler);
        request.setErrorHandler(errStreamHandler);
        this.result = (MavenProcessInvocationResult) execute(request);
        return result;
    }

    @Override
    public InvocationResult execute(InvocationRequest request) throws MavenInvocationException {
        MojoTestBase.passUserSettings(request);
        return super.execute(request);
    }

    public String log() throws IOException {
        if (log == null) {
            return null;
        }
        return FileUtils.readFileToString(log, "UTF-8");
    }

    public MavenProcessInvocationResult getResult() {
        return result;
    }
}
