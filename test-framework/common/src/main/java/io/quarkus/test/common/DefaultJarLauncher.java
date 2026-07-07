package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.createStartedFunction;
import static io.quarkus.test.common.LauncherUtil.waitForCapturedListeningData;
import static io.quarkus.test.common.LauncherUtil.waitForStartedFunction;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.builditem.JvmStartupOptimizerArchiveType;
import io.quarkus.deployment.util.BoundedProcessRunner;
import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.smallrye.config.SmallRyeConfig;

/**
 * Default host-process implementation of {@link JarArtifactLauncher}.
 * <p>
 * For explicit startup-archive training it prepares fresh host output before launch, applies the supplied recording
 * arguments, runs any required post-close creation command with bounded process cleanup, and validates the resulting
 * file or directory before {@link #close()} returns.
 */
public class DefaultJarLauncher implements JarArtifactLauncher {
    private static final Logger log = Logger.getLogger(DefaultJarLauncher.class);

    private static final String JAVA_HOME_SYS = "java.home";
    private static final String JAVA_HOME_ENV = "JAVA_HOME";
    private static final String VERTX_HTTP_RECORDER = "io.quarkus.vertx.http.runtime.VertxHttpRecorder";
    private static final Duration POST_CLOSE_FORCE_TIMEOUT = Duration.ofSeconds(5);

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
    private Duration shutdownTimeout;
    private String testProfile;
    private List<String> argLine;
    private Map<String, String> env;
    private Path jarPath;
    private List<String> recordingArgs;
    private List<String> postCloseCommand;
    private Optional<Path> aotResultPath;
    private String aotResultDescription;
    private Optional<JvmStartupArchiveTraining> startupArchiveTraining;

    private final Map<String, String> systemProps = new HashMap<>();
    private Process quarkusProcess;

    private Path logFile;
    private List<String> programArgs;

    @Override
    public void init(JarArtifactLauncher.JarInitContext initContext) {
        this.httpPort = initContext.httpPort();
        this.httpsPort = initContext.httpsPort();
        this.waitTimeSeconds = initContext.waitTime().getSeconds();
        this.shutdownTimeout = initContext.shutdownTimeout();
        this.testProfile = initContext.testProfile();
        this.argLine = initContext.argLine();
        this.env = initContext.env();
        this.jarPath = initContext.jarPath();
        this.recordingArgs = initContext.recordingArgs();
        this.postCloseCommand = initContext.postCloseCommand();
        this.aotResultPath = initContext.aotResultPath();
        this.aotResultDescription = initContext.aotResultDescription();
        this.startupArchiveTraining = initContext.startupArchiveTraining();
    }

    @Override
    public ListeningAddresses start() throws IOException {
        start(new String[0], true);
        Function<IntegrationTestStartedNotifier.Context, IntegrationTestStartedNotifier.Result> startedFunction = createStartedFunction();
        LogRuntimeConfig logRuntimeConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
                .getConfigMapping(LogRuntimeConfig.class);
        logFile = logRuntimeConfig.file().path().toPath();
        if (startedFunction != null) {
            waitForStartedFunction(startedFunction, quarkusProcess, waitTimeSeconds, logFile);
            return ListeningAddresses.EMPTY;
        } else {
            return waitForCapturedListeningData(quarkusProcess, logRuntimeConfig.file().path().toPath(), waitTimeSeconds);
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

    /**
     * Starts the configured JAR with explicit application arguments and I/O handling.
     *
     * @param programArgs arguments passed to the packaged application
     * @param handleIo whether the launcher drains and forwards process I/O
     * @throws IOException if training output cannot be prepared or the process cannot be started
     */
    public void start(String[] programArgs, boolean handleIo) throws IOException {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        LogRuntimeConfig logRuntimeConfig = config.getConfigMapping(LogRuntimeConfig.class);
        logFile = logRuntimeConfig.file().path().toPath();
        prepareStartupArchiveTraining();

        List<String> args = new ArrayList<>();
        args.add(determineJavaPath());
        if (!argLine.isEmpty()) {
            args.addAll(argLine);
        }
        if (!recordingArgs.isEmpty()) {
            args.addAll(recordingArgs);
        }
        if (HTTP_PRESENT) {
            args.add("-Dquarkus.http.port=" + httpPort);
            args.add("-Dquarkus.http.ssl-port=" + httpsPort);
            args.add("-Dtest.url=" + LauncherUtil.generateTestUrl());
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
        this.programArgs = Arrays.asList(programArgs);
        args.addAll(this.programArgs);

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

    private void prepareStartupArchiveTraining() throws IOException {
        if (startupArchiveTraining.isEmpty()) {
            return;
        }
        startupArchiveTraining.get().prepareHostOutput();
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
    public void includeAsSysProps(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    @Override
    public void close() {
        LauncherUtil.destroyProcess(quarkusProcess, getAdjustedShutdownTimeout());
        PostCloseCommandResult postCloseCommandResult = PostCloseCommandResult.notRun();
        if (!postCloseCommand.isEmpty()) {
            postCloseCommandResult = runPostCloseCommand(postCloseCommand);
        }
        if (aotResultPath.isPresent()) {
            var path = aotResultPath.get();
            if (Files.exists(path)) {
                log.infof("%s '%s' created", aotResultDescription, path.toAbsolutePath());
            } else {
                log.debug("Expected AOT result not found: " + path);
            }
        }

        RuntimeException failure = null;
        try {
            if (startupArchiveTraining.isPresent()) {
                if (!postCloseCommandResult.succeeded()) {
                    failure = new IllegalStateException("The startup-archive post-close command failed");
                } else {
                    startupArchiveTraining.get().validateProducedArchive();
                }
            }
        } catch (RuntimeException e) {
            failure = e;
        } finally {
            if (postCloseCommandResult.terminated()) {
                // The recording configuration may be removed only after the process that consumes it is known to
                // have terminated.
                try {
                    Path aotConfiguration = startupArchiveTraining
                            .filter(training -> training.type() == JvmStartupOptimizerArchiveType.AOT)
                            .map(JvmStartupArchiveTraining::aotConfigurationDestination)
                            .orElseGet(() -> jarPath.resolveSibling("app.aotconf"));
                    Files.deleteIfExists(aotConfiguration);
                } catch (IOException e) {
                    log.debug("Unable to delete AOT config file", e);
                }
            } else {
                log.warn("Retaining the AOT configuration because the post-close command may still be running");
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private Duration getAdjustedShutdownTimeout() {
        return shutdownTimeout.plus(!recordingArgs.isEmpty() || startupArchiveTraining.isPresent()
                ? Duration.ofMinutes(1)
                : Duration.ofSeconds(10));
    }

    private PostCloseCommandResult runPostCloseCommand(List<String> baseCommand) {
        // The base command contains only the AOT-specific flags.
        // We prepend java binary and argLine, then append runtime system props,
        // -jar, jar path, and program args.
        List<String> command = new ArrayList<>();
        command.add(determineJavaPath());
        if (!argLine.isEmpty()) {
            command.addAll(argLine);
        }
        command.addAll(baseCommand);
        if (HTTP_PRESENT) {
            command.add("-Dquarkus.http.port=" + httpPort);
            command.add("-Dquarkus.http.ssl-port=" + httpsPort);
            command.add("-Dtest.url=" + LauncherUtil.generateTestUrl());
        }
        command.add("-Dquarkus.log.file.path=" + logFile.toAbsolutePath());
        command.add("-Dquarkus.log.file.enabled=true");
        command.add("-Dquarkus.log.category.\"io.quarkus\".level=INFO");
        if (testProfile != null) {
            command.add("-Dquarkus.profile=" + testProfile);
        }
        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            command.add("-D" + e.getKey() + "=" + e.getValue());
        }
        command.add("-jar");
        command.add(jarPath.toAbsolutePath().toString());
        command.addAll(programArgs);

        log.debugf("Running post-close command: %s", String.join(" ", command));
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            Duration timeout = startupArchiveTraining.isEmpty()
                    ? Duration.ofSeconds(20)
                    : getAdjustedShutdownTimeout();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                boolean terminated = BoundedProcessRunner.forceTerminateAndWait(process, POST_CLOSE_FORCE_TIMEOUT);
                if (!terminated) {
                    log.warn("Post-close command did not terminate after it was forcibly destroyed");
                }
                log.warn("Post-close command timed out");
                return new PostCloseCommandResult(false, terminated);
            }
            if (process.exitValue() != 0) {
                log.warnf("Post-close command failed with exit code %d", process.exitValue());
                return PostCloseCommandResult.failed();
            }
            return PostCloseCommandResult.successful();
        } catch (InterruptedException e) {
            boolean terminated = process == null
                    || BoundedProcessRunner.forceTerminateAndWait(process, POST_CLOSE_FORCE_TIMEOUT);
            if (!terminated) {
                log.warn("Interrupted post-close command did not terminate after it was forcibly destroyed");
            }
            Thread.currentThread().interrupt();
            log.warn("Post-close command was interrupted", e);
            return new PostCloseCommandResult(false, terminated);
        } catch (Exception e) {
            log.warn("Post-close command failed", e);
            return new PostCloseCommandResult(false, process == null || !process.isAlive());
        }
    }

    /**
     * Keeps command success separate from proven termination: success controls archive validation, while termination
     * controls whether the recording configuration is safe to delete.
     */
    private record PostCloseCommandResult(boolean succeeded, boolean terminated) {

        private static PostCloseCommandResult notRun() {
            return new PostCloseCommandResult(true, true);
        }

        private static PostCloseCommandResult successful() {
            return new PostCloseCommandResult(true, true);
        }

        private static PostCloseCommandResult failed() {
            return new PostCloseCommandResult(false, true);
        }
    }

}
