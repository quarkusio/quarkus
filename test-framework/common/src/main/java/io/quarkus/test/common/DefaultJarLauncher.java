package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.createStartedFunction;
import static io.quarkus.test.common.LauncherUtil.updateConfigForPort;
import static io.quarkus.test.common.LauncherUtil.waitForCapturedListeningData;
import static io.quarkus.test.common.LauncherUtil.waitForStartedFunction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.test.common.http.TestHTTPResourceManager;

public class DefaultJarLauncher implements JarArtifactLauncher {

    private static final String VERTX_HTTP_RECORDER = "io.quarkus.vertx.http.runtime.VertxHttpRecorder";
    static boolean HTTP_PRESENT;

    static {
        boolean http = true;
        try {
            Class.forName(VERTX_HTTP_RECORDER);
        } catch (ClassNotFoundException e) {
            http = false;
        }
        HTTP_PRESENT = http;
    }

    private int httpPort;
    private int httpsPort;
    private long waitTimeSeconds;
    private String testProfile;
    private List<String> argLine;
    private Path jarPath;

    private final Map<String, String> systemProps = new HashMap<>();
    private Process quarkusProcess;

    private boolean isSsl;

    @Override
    public void init(JarArtifactLauncher.JarInitContext initContext) {
        this.httpPort = initContext.httpPort();
        this.httpsPort = initContext.httpsPort();
        this.waitTimeSeconds = initContext.waitTime().getSeconds();
        this.testProfile = initContext.testProfile();
        this.argLine = initContext.argLine();
        this.jarPath = initContext.jarPath();
    }

    public void start() throws IOException {
        start(new String[0], true);
        Function<IntegrationTestStartedNotifier.Context, IntegrationTestStartedNotifier.Result> startedFunction = createStartedFunction();
        var logFile = PropertyTestUtil.getLogFilePath();
        if (startedFunction != null) {
            IntegrationTestStartedNotifier.Result result = waitForStartedFunction(startedFunction, quarkusProcess,
                    waitTimeSeconds, logFile);
            isSsl = result.isSsl();
        } else {
            ListeningAddress result = waitForCapturedListeningData(quarkusProcess, logFile, waitTimeSeconds);
            updateConfigForPort(result.getPort());
            isSsl = result.isSsl();
        }
    }

    @Override
    public LaunchResult runToCompletion(String[] args) {
        try {
            start(args, false);
            ProcessReader error = new ProcessReader(quarkusProcess.getErrorStream());
            ProcessReader stdout = new ProcessReader(quarkusProcess.getInputStream());
            Thread t = new Thread(error, "Error stream reader");
            t.start();
            t = new Thread(stdout, "Stdout stream reader");
            t.start();
            byte[] s = stdout.get();
            byte[] e = error.get();
            return new LaunchResult(quarkusProcess.waitFor(), s, e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start(String[] programArgs, boolean handleIo) throws IOException {

        System.setProperty("test.url", TestHTTPResourceManager.getUri());

        List<String> args = new ArrayList<>();
        args.add("java");
        if (!argLine.isEmpty()) {
            args.addAll(argLine);
        }
        if (HTTP_PRESENT) {
            args.add("-Dquarkus.http.port=" + httpPort);
            args.add("-Dquarkus.http.ssl-port=" + httpsPort);
            // this won't be correct when using the random port but it's really only used by us for the rest client tests
            // in the main module, since those tests hit the application itself
            args.add("-Dtest.url=" + TestHTTPResourceManager.getUri());
        }
        Path logFile = PropertyTestUtil.getLogFilePath();
        args.add("-Dquarkus.log.file.path=" + logFile.toAbsolutePath().toString());
        args.add("-Dquarkus.log.file.enable=true");
        if (testProfile != null) {
            args.add("-Dquarkus.profile=" + testProfile);
        }
        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            args.add("-D" + e.getKey() + "=" + e.getValue());
        }
        args.add("-jar");
        args.add(jarPath.toAbsolutePath().toString());
        args.addAll(Arrays.asList(programArgs));

        System.out.println("Executing " + args);

        Files.deleteIfExists(logFile);
        Files.createDirectories(logFile.getParent());

        if (handleIo) {
            quarkusProcess = LauncherUtil.launchProcess(args);
        } else {
            quarkusProcess = Runtime.getRuntime().exec(args.toArray(new String[0]));
        }

    }

    @Override
    public boolean listensOnSsl() {
        return isSsl;
    }

    @Override
    public void includeAsSysProps(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    @Override
    public void close() {
        quarkusProcess.destroy();
    }
}
