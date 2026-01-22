package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.createStartedFunction;
import static io.quarkus.test.common.LauncherUtil.waitForCapturedListeningData;
import static io.quarkus.test.common.LauncherUtil.waitForStartedFunction;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.smallrye.config.SmallRyeConfig;

public class DefaultNativeImageLauncher implements NativeImageLauncher {
    private static final Logger log = Logger.getLogger(DefaultNativeImageLauncher.class);

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");

    private int httpPort;
    private int httpsPort;
    private long waitTimeSeconds;
    private String testProfile;
    private List<String> argLine;
    private Map<String, String> env;
    private String nativeImagePath;
    private String configuredOutputDirectory;
    private Class<?> testClass;

    private Process quarkusProcess;
    private final Map<String, String> systemProps = new HashMap<>();

    private Path logFile;

    @Override
    public void init(NativeImageInitContext initContext) {
        this.httpPort = initContext.httpPort();
        this.httpsPort = initContext.httpsPort();
        this.waitTimeSeconds = initContext.waitTime().getSeconds();
        this.testProfile = initContext.testProfile();
        this.nativeImagePath = initContext.nativeImagePath();
        this.configuredOutputDirectory = initContext.getConfiguredOutputDirectory();
        this.argLine = initContext.argLine();
        this.env = initContext.env();
        this.testClass = initContext.testClass();
    }

    @Override
    public LaunchResult runToCompletion(String[] args) {
        try {
            start(args, false);
            ProcessReader error = new ProcessReader(quarkusProcess.getErrorStream());
            ProcessReader stdout = new ProcessReader(quarkusProcess.getInputStream());
            Thread t1 = new Thread(error, "Error stream reader");
            t1.start();
            Thread t2 = new Thread(stdout, "Stdout stream reader");
            t2.start();
            t1.join();
            t2.join();
            byte[] s = stdout.get();
            byte[] e = error.get();
            return new LaunchResult(quarkusProcess.waitFor(), s, e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<ListeningAddress> start() throws IOException {
        start(new String[0], true);
        LogRuntimeConfig logRuntimeConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class)
                .getConfigMapping(LogRuntimeConfig.class);
        logFile = logRuntimeConfig.file().path().toPath();
        Function<IntegrationTestStartedNotifier.Context, IntegrationTestStartedNotifier.Result> startedFunction = createStartedFunction();
        if (startedFunction != null) {
            waitForStartedFunction(startedFunction, quarkusProcess, waitTimeSeconds, logRuntimeConfig.file().path().toPath());
            return Optional.empty();
        } else {
            return waitForCapturedListeningData(quarkusProcess, logRuntimeConfig.file().path().toPath(), waitTimeSeconds);
        }
    }

    public void start(String[] programArgs, boolean handleIo) throws IOException {
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        LogRuntimeConfig logRuntimeConfig = config.getConfigMapping(LogRuntimeConfig.class);

        if (nativeImagePath == null) {
            nativeImagePath = guessPath(testClass);
        }
        List<String> args = new ArrayList<>();
        args.add(nativeImagePath);
        if (!argLine.isEmpty()) {
            args.addAll(argLine);
        }
        if (DefaultJarLauncher.HTTP_PRESENT) {
            args.add("-Dquarkus.http.port=" + httpPort);
            args.add("-Dquarkus.http.ssl-port=" + httpsPort);
            args.add("-Dtest.url=" + LauncherUtil.generateTestUrl());
        }
        logFile = logRuntimeConfig.file().path().toPath();
        args.add("-Dquarkus.log.file.path=" + logFile.toAbsolutePath());
        args.add("-Dquarkus.log.file.enabled=true");
        args.add("-Dquarkus.log.category.\"io.quarkus\".level=INFO");
        if (testProfile != null) {
            args.add("-Dquarkus.profile=" + testProfile);
        }
        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            args.add("-D" + e.getKey() + "=" + e.getValue());
        }
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

    private void waitForStartedSupplier(Supplier<Boolean> startedSupplier, Process quarkusProcess, long waitTime) {
        long bailout = System.currentTimeMillis() + waitTime * 1000;
        boolean started = false;
        while (System.currentTimeMillis() < bailout) {
            if (!quarkusProcess.isAlive()) {
                throw new RuntimeException("Failed to start target quarkus application, process has exited");
            }
            try {
                Thread.sleep(100);
                if (startedSupplier.get()) {
                    started = true;
                    break;
                }
            } catch (Exception ignored) {

            }
        }
        if (!started) {
            quarkusProcess.destroyForcibly();
            throw new RuntimeException("Unable to start target quarkus application " + this.waitTimeSeconds + "s");
        }
    }

    private String guessPath(Class<?> testClass) {
        //ok, lets make a guess
        //this is a horrible hack, but it is intended to make this work in IDE's

        ClassLoader cl = testClass.getClassLoader();

        if (cl instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) cl).getURLs();
            for (URL url : urls) {
                final String applicationNativeImagePath = guessPath(url);
                if (applicationNativeImagePath != null) {
                    return applicationNativeImagePath;
                }
            }
        } else {
            // try the CodeSource way
            final CodeSource codeSource = testClass.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                final URL codeSourceLocation = codeSource.getLocation();
                final String applicationNativeImagePath = guessPath(codeSourceLocation);
                if (applicationNativeImagePath != null) {
                    return applicationNativeImagePath;
                }
            }
        }

        throw new RuntimeException(
                "Unable to automatically find native image, please set the native.image.path to the native executable you wish to test");
    }

    private String guessPath(final URL url) {
        if (url == null) {
            return null;
        }
        String file = null;
        if (url.getProtocol().equals("file") && url.getPath().endsWith("test-classes/")) {
            //we have the maven test classes dir
            File testClasses = new File(url.getPath());
            file = guessPathFromDir(testClasses.getParentFile());
        } else if (url.getProtocol().equals("file") && url.getPath().endsWith("test/")) {
            //we have the gradle test classes dir, build/classes/java/test
            File testClasses = new File(url.getPath());
            file = guessPathFromDir(testClasses.getParentFile().getParentFile().getParentFile());
        } else if (url.getProtocol().equals("file") && url.getPath().contains("/target/surefire/")) {
            //this will make mvn failsafe:integration-test work
            String path = url.getPath();
            int index = path.lastIndexOf("/target/");
            File targetDir = new File(path.substring(0, index) + "/target/");
            file = guessPathFromDir(targetDir);

        }
        return file;
    }

    private String guessPathFromDir(File dir) {
        if (dir == null) {
            return null;
        }
        if (configuredOutputDirectory != null) {
            dir = dir.toPath().resolve(configuredOutputDirectory).toFile();
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (isNativeExecutable(file)) {
                logGuessedPath(file.getAbsolutePath());
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    private static boolean isNativeExecutable(File file) {
        if (IS_WINDOWS) {
            return file.getName().endsWith("-runner.exe");
        } else {
            return file.getName().endsWith("-runner");
        }
    }

    private static void logGuessedPath(String guessedPath) {
        System.err.println("======================================================================================");
        System.err.println("  native.image.path was not set, making a guess for the correct path of native image");
        System.err.println("  guessed path: " + guessedPath);
        System.err.println("======================================================================================");
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
