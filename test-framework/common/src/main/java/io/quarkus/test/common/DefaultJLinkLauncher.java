package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.createStartedFunction;
import static io.quarkus.test.common.LauncherUtil.waitForCapturedListeningData;
import static io.quarkus.test.common.LauncherUtil.waitForStartedFunction;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.smallrye.config.SmallRyeConfig;

public class DefaultJLinkLauncher implements JLinkLauncher {
    private static final Logger log = Logger.getLogger(DefaultJLinkLauncher.class);

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");

    private int httpPort;
    private int httpsPort;
    private long waitTimeSeconds;
    private Duration shutdownTimeout;
    private String testProfile;
    private List<String> argLine;
    private Map<String, String> env;
    private Path imagePath;
    private String launcherName;
    private List<String> recordingArgs;
    private List<String> postCloseCommand;
    private Optional<Path> aotResultPath;
    private String aotResultDescription;

    private final Map<String, String> systemProps = new HashMap<>();
    private Process quarkusProcess;

    private Path logFile;
    private List<String> programArgs;

    @Override
    public void init(JLinkInitContext initContext) {
        this.httpPort = initContext.httpPort();
        this.httpsPort = initContext.httpsPort();
        this.waitTimeSeconds = initContext.waitTime().getSeconds();
        this.shutdownTimeout = initContext.shutdownTimeout();
        this.testProfile = initContext.testProfile();
        this.argLine = initContext.argLine();
        this.env = initContext.env();
        this.imagePath = initContext.imagePath();
        this.launcherName = initContext.launcherName();
        this.recordingArgs = initContext.recordingArgs();
        this.postCloseCommand = initContext.postCloseCommand();
        this.aotResultPath = initContext.aotResultPath();
        this.aotResultDescription = initContext.aotResultDescription();
    }

    @Override
    public ListeningResults start() throws IOException {
        start(new String[0], true);
        Function<IntegrationTestStartedNotifier.Context, IntegrationTestStartedNotifier.Result> startedFunction = createStartedFunction();
        LogRuntimeConfig logRuntimeConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
                .getConfigMapping(LogRuntimeConfig.class);
        logFile = logRuntimeConfig.file().path().toPath();
        if (startedFunction != null) {
            waitForStartedFunction(startedFunction, quarkusProcess, waitTimeSeconds, logFile);
            return ListeningResults.EMPTY;
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

    public void start(String[] programArgs, boolean handleIo) throws IOException {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        LogRuntimeConfig logRuntimeConfig = config.getConfigMapping(LogRuntimeConfig.class);
        logFile = logRuntimeConfig.file().path().toPath();

        List<String> jvmOptions = new ArrayList<>();
        if (!argLine.isEmpty()) {
            jvmOptions.addAll(argLine);
        }
        if (!recordingArgs.isEmpty()) {
            jvmOptions.addAll(recordingArgs);
        }
        if (DefaultJarLauncher.HTTP_PRESENT) {
            jvmOptions.add("-Dquarkus.http.port=" + httpPort);
            jvmOptions.add("-Dquarkus.http.ssl-port=" + httpsPort);
            jvmOptions.add("-Dtest.url=" + LauncherUtil.generateTestUrl());
        }
        jvmOptions.add("-Dquarkus.log.file.path=" + logFile.toAbsolutePath());
        jvmOptions.add("-Dquarkus.log.file.enabled=true");
        jvmOptions.add("-Dquarkus.log.category.\"io.quarkus\".level=INFO");
        if (testProfile != null) {
            jvmOptions.add("-Dquarkus.profile=" + testProfile);
        }
        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            jvmOptions.add("-D" + e.getKey() + "=" + e.getValue());
        }

        List<String> args = new ArrayList<>();
        args.add(determineLauncherPath());
        this.programArgs = Arrays.asList(programArgs);
        args.addAll(this.programArgs);

        Map<String, String> launchEnv = new HashMap<>(env);
        launchEnv.put("JDK_JAVA_OPTIONS", String.join(" ", jvmOptions));

        System.out.println("Executing \"" + String.join(" ", args) + "\" with JDK_JAVA_OPTIONS=\""
                + String.join(" ", jvmOptions) + "\"");

        try {
            Files.deleteIfExists(logFile);
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
        } catch (FileSystemException e) {
            log.warnf("Log file %s deletion failed, could happen on Windows, we can carry on.", logFile);
        }

        if (handleIo) {
            quarkusProcess = LauncherUtil.launchProcessAndDrainIO(args, launchEnv);
        } else {
            quarkusProcess = LauncherUtil.launchProcess(args, launchEnv);
        }
    }

    private String determineLauncherPath() {
        String name = IS_WINDOWS ? launcherName + ".bat" : launcherName;
        return imagePath.resolve("bin").resolve(name).toAbsolutePath().toString();
    }

    @Override
    public void includeAsSysProps(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    @Override
    public void close() {
        LauncherUtil.destroyProcess(quarkusProcess, true);
        if (!postCloseCommand.isEmpty()) {
            runPostCloseCommand(postCloseCommand);
        }
        if (aotResultPath.isPresent()) {
            Path path = aotResultPath.get();
            if (Files.exists(path)) {
                log.infof("%s '%s' created", aotResultDescription, path.toAbsolutePath());
            } else {
                log.debug("Expected AOT result not found: " + path);
            }
        }
    }

    private Duration getAdjustedShutdownTimeout() {
        return shutdownTimeout.plus(!recordingArgs.isEmpty() ? Duration.ofMinutes(1) : Duration.ofSeconds(10));
    }

    private void runPostCloseCommand(List<String> baseCommand) {
        List<String> jvmOptions = new ArrayList<>();
        if (!argLine.isEmpty()) {
            jvmOptions.addAll(argLine);
        }
        jvmOptions.addAll(baseCommand);
        if (DefaultJarLauncher.HTTP_PRESENT) {
            jvmOptions.add("-Dquarkus.http.port=" + httpPort);
            jvmOptions.add("-Dquarkus.http.ssl-port=" + httpsPort);
            jvmOptions.add("-Dtest.url=" + LauncherUtil.generateTestUrl());
        }
        jvmOptions.add("-Dquarkus.log.file.path=" + logFile.toAbsolutePath());
        jvmOptions.add("-Dquarkus.log.file.enabled=true");
        jvmOptions.add("-Dquarkus.log.category.\"io.quarkus\".level=INFO");
        if (testProfile != null) {
            jvmOptions.add("-Dquarkus.profile=" + testProfile);
        }
        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            jvmOptions.add("-D" + e.getKey() + "=" + e.getValue());
        }

        List<String> command = new ArrayList<>();
        command.add(determineLauncherPath());
        command.addAll(programArgs);

        log.debugf("Running post-close command: %s with JDK_JAVA_OPTIONS=%s",
                String.join(" ", command), String.join(" ", jvmOptions));
        try {
            ProcessBuilder pb = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.environment().put("JDK_JAVA_OPTIONS", String.join(" ", jvmOptions));
            pb.start().waitFor(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Post-close command failed", e);
        }
    }
}
