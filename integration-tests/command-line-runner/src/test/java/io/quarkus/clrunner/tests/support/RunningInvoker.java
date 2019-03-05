package io.quarkus.clrunner.tests.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.InvokerLogger;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;
import org.apache.maven.shared.invoker.PrintStreamLogger;

/*
 * Copied and adapted from devtools-maven
 */
public class RunningInvoker extends MavenProcessInvoker {

    private final boolean debug;
    private MavenProcessInvocationResult result;
    private final File log;
    private final PrintStreamHandler logHandler;

    public RunningInvoker(File basedir, boolean debug) throws FileNotFoundException {
        this.debug = debug;
        setWorkingDirectory(basedir);
        String repo = System.getProperty("maven.repo");
        if (repo == null) {
            repo = new File(System.getProperty("user.home"), ".m2/repository").getAbsolutePath();
        }
        log = new File(basedir, "build-" + basedir.getName() + ".log");
        PrintStream stream = null;
        try {
            stream = new PrintStream(log, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            stream = new PrintStream(log);
        }
        logHandler = new PrintStreamHandler(stream, true);
        setErrorHandler(logHandler);
        setOutputHandler(logHandler);
        setLogger(new PrintStreamLogger(stream, InvokerLogger.DEBUG));
    }

    public MavenProcessInvocationResult execute(List<String> goals, Map<String, String> envVars)
            throws MavenInvocationException {
        DefaultInvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(goals);
        request.setDebug(debug);
        request.setLocalRepositoryDirectory(getLocalRepositoryDirectory());
        request.setBaseDirectory(getWorkingDirectory());
        request.setPomFile(new File(getWorkingDirectory(), "pom.xml"));

        if (System.getProperty("mavenOpts") != null) {
            request.setMavenOpts(System.getProperty("mavenOpts"));
        }

        request.setShellEnvironmentInherited(true);
        envVars.forEach(request::addShellEnvironment);
        request.setOutputHandler(logHandler);
        request.setErrorHandler(logHandler);
        this.result = (MavenProcessInvocationResult) execute(request);
        return result;
    }

    @Override
    public InvocationResult execute(InvocationRequest request) throws MavenInvocationException {
        return super.execute(request);
    }

}
