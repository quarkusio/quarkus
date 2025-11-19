package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.createStartedFunction;
import static io.quarkus.test.common.LauncherUtil.updateConfigForPort;
import static io.quarkus.test.common.LauncherUtil.waitForCapturedListeningData;
import static io.quarkus.test.common.LauncherUtil.waitForStartedFunction;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.smallrye.config.SmallRyeConfig;

public class DefaultJarLauncher implements JarArtifactLauncher {
    private static final Logger log = Logger.getLogger(DefaultJarLauncher.class);

    private static final String JAVA_HOME_SYS = "java.home";
    private static final String JAVA_HOME_ENV = "JAVA_HOME";
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
    private Map<String, String> env;
    private Path jarPath;
    private boolean generateAotFile;

    private final Map<String, String> systemProps = new HashMap<>();
    private Process quarkusProcess;

    private boolean isSsl;
    private Path logFile;

    @Override
    public void init(JarArtifactLauncher.JarInitContext initContext) {
        this.httpPort = initContext.httpPort();
        this.httpsPort = initContext.httpsPort();
        this.waitTimeSeconds = initContext.waitTime().getSeconds();
        this.testProfile = initContext.testProfile();
        this.argLine = initContext.argLine();
        this.env = initContext.env();
        this.jarPath = initContext.jarPath();
        this.generateAotFile = initContext.generateAotFile();
    }

    public void start() throws IOException {
        start(new String[0], true);
        Function<IntegrationTestStartedNotifier.Context, IntegrationTestStartedNotifier.Result> startedFunction = createStartedFunction();
        LogRuntimeConfig logRuntimeConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
                .getConfigMapping(LogRuntimeConfig.class);
        logFile = logRuntimeConfig.file().path().toPath();
        if (startedFunction != null) {
            IntegrationTestStartedNotifier.Result result = waitForStartedFunction(startedFunction, quarkusProcess,
                    waitTimeSeconds, logFile);
            isSsl = result.isSsl();
        } else {
            ListeningAddress result = waitForCapturedListeningData(quarkusProcess, logRuntimeConfig.file().path().toPath(),
                    waitTimeSeconds);
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
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        LogRuntimeConfig logRuntimeConfig = config.getConfigMapping(LogRuntimeConfig.class);
        logFile = logRuntimeConfig.file().path().toPath();
        System.setProperty("test.url", TestHTTPResourceManager.getUri());

        List<String> args = new ArrayList<>();
        args.add(determineJavaPath());
        if (!argLine.isEmpty()) {
            args.addAll(argLine);
        }
        if (generateAotFile) {
            args.add("-XX:AOTCacheOutput=%s".formatted(jarPath.resolveSibling("app.aot")));
        }
        if (HTTP_PRESENT) {
            args.add("-Dquarkus.http.port=" + httpPort);
            args.add("-Dquarkus.http.ssl-port=" + httpsPort);
            // this won't be correct when using the random port but it's really only used by us for the rest client
            // tests
            // in the main module, since those tests hit the application itself
            args.add("-Dtest.url=" + TestHTTPResourceManager.getUri());
        }
        args.add("-Dquarkus.log.file.path=" + logFile.toAbsolutePath());
        args.add("-Dquarkus.log.file.enabled=true");
        args.add("-Dquarkus.log.category.\"io.quarkus\".level=INFO");
        if (testProfile != null) {
            args.add("-Dquarkus.profile=" + testProfile);
        }
        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            args.add("-D" + e.getKey() + "=" + e.getValue());
        }
        args.add("-jar");
        args.add(jarPath.toAbsolutePath().toString());
        args.addAll(Arrays.asList(programArgs));

        System.out.println("Executing \"" + String.join(" ", args) + "\"");

        try {
            Files.deleteIfExists(logFile);
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
        } catch (FileSystemException e) {
            log.warnf("Log file %s deletion failed, could happen on Windows, we can carry on.", logFile);
        }

        if (handleIo) {
            quarkusProcess = LauncherUtil.launchProcessAndDrainIO(args, env);
        } else {
            quarkusProcess = LauncherUtil.launchProcess(args, env);
        }

    }

    private String determineJavaPath() {
        // try system property first - it will be the JAVA_HOME used by the current JVM
        String home = System.getProperty(JAVA_HOME_SYS);
        if (home == null) {
            // No luck, somewhat a odd JVM not enforcing this property
            // try with the JAVA_HOME environment variable
            home = System.getenv(JAVA_HOME_ENV);
        }
        if (home != null) {
            File javaHome = new File(home);
            File file = new File(javaHome, "bin/java");
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }

        // just assume 'java' is on the system path
        return "java";
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
        LauncherUtil.toStdOut(logFile);
        LauncherUtil.destroyProcess(quarkusProcess);
    }

}
