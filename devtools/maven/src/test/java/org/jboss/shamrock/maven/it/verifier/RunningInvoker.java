package io.quarkus.maven.it.verifier;

import static io.quarkus.maven.it.MojoTestBase.installPluginToLocalRepository;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.*;
import org.jutils.jprocesses.JProcesses;
import org.jutils.jprocesses.model.ProcessInfo;

/**
 * Implementation of verifier using a forked process that is still running while verifying. The process is stop when
 * {@link RunningInvoker#stop()} is called.
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
        installPluginToLocalRepository(new File(repo));
        setLocalRepositoryDirectory(new File(repo));
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

    public void stop() {
        if (result == null) {
            return;
        }
        List<ProcessInfo> list = JProcesses.getProcessList().stream().filter(pi ->
        // Kill all process using the live reload and the live reload process.
        // This might be too much
        pi.getCommand().contains("quarkus:dev") || pi.getCommand().contains(getWorkingDirectory().getAbsolutePath()))
                .collect(Collectors.toList());

        list.stream()
                .forEach(i -> JProcesses.killProcess(Integer.valueOf(i.getPid())));
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

    public String log() throws IOException {
        return FileUtils.readFileToString(log, "UTF-8");
    }
}
