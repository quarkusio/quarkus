package io.quarkus.test.common;

import static io.quarkus.test.common.LauncherUtil.installAndGetSomeConfig;
import static io.quarkus.test.common.LauncherUtil.updateConfigForPort;
import static io.quarkus.test.common.LauncherUtil.waitForCapturedListeningData;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.ServiceLoader;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;

import io.quarkus.test.common.http.TestHTTPResourceManager;

public class NativeImageLauncher implements ArtifactLauncher {

    private static final int DEFAULT_PORT = 8081;
    private static final int DEFAULT_HTTPS_PORT = 8444;
    private static final long DEFAULT_IMAGE_WAIT_TIME = 60;

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");

    private final Class<?> testClass;
    private final String profile;
    private Process quarkusProcess;
    private final int httpPort;
    private final int httpsPort;
    private final long waitTimeSeconds;
    private final Map<String, String> systemProps = new HashMap<>();
    private Supplier<Boolean> startedSupplier = null;

    private boolean isSsl;

    private NativeImageLauncher(Class<?> testClass, Config config) {
        this(testClass,
                config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                config.getValue("quarkus.test.native-image-wait-time", OptionalLong.class).orElse(DEFAULT_IMAGE_WAIT_TIME),
                config.getOptionalValue("quarkus.test.native-image-profile", String.class)
                        .orElse(null));
    }

    public NativeImageLauncher(Class<?> testClass) {
        // todo: accessing run time config from here doesn't make sense
        this(testClass, installAndGetSomeConfig());
    }

    public NativeImageLauncher(Class<?> testClass, int httpPort, int httpsPort, long waitTimeSeconds, String profile) {
        this.testClass = testClass;
        this.httpPort = httpPort;
        this.httpsPort = httpsPort;
        this.waitTimeSeconds = waitTimeSeconds;
        List<NativeImageStartedNotifier> startedNotifiers = new ArrayList<>();
        for (NativeImageStartedNotifier i : ServiceLoader.load(NativeImageStartedNotifier.class)) {
            startedNotifiers.add(i);
        }
        this.profile = profile;
        if (!startedNotifiers.isEmpty()) {
            this.startedSupplier = () -> {
                for (NativeImageStartedNotifier i : startedNotifiers) {
                    if (i.isNativeImageStarted()) {
                        return true;
                    }
                }
                return false;
            };
        }

    }

    public void start() throws IOException {
        System.setProperty("test.url", TestHTTPResourceManager.getUri());

        String path = System.getProperty("native.image.path");
        if (path == null) {
            path = guessPath(testClass);
        }
        List<String> args = new ArrayList<>();
        args.add(path);
        args.add("-Dquarkus.http.port=" + httpPort);
        args.add("-Dquarkus.http.ssl-port=" + httpsPort);
        // this won't be correct when using the random port but it's really only used by us for the rest client tests
        // in the main module, since those tests hit the application itself
        args.add("-Dtest.url=" + TestHTTPResourceManager.getUri());
        Path logFile = PropertyTestUtil.getLogFilePath();
        args.add("-Dquarkus.log.file.path=" + logFile.toAbsolutePath().toString());
        args.add("-Dquarkus.log.file.enable=true");
        if (profile != null) {
            args.add("-Dquarkus.profile=" + profile);
        }
        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            args.add("-D" + e.getKey() + "=" + e.getValue());
        }

        System.out.println("Executing " + args);

        Files.deleteIfExists(logFile);
        Files.createDirectories(logFile.getParent());

        quarkusProcess = LauncherUtil.launchProcess(args);

        if (startedSupplier != null) {
            waitForStartedSupplier(quarkusProcess, startedSupplier, waitTimeSeconds);
        } else {
            ListeningAddress result = waitForCapturedListeningData(quarkusProcess, logFile, waitTimeSeconds);
            updateConfigForPort(result.getPort());
            isSsl = result.isSsl();
        }
    }

    private void waitForStartedSupplier(Process quarkusProcess, Supplier<Boolean> startedSupplier, long waitTime) {
        long bailout = System.currentTimeMillis() + waitTime * 1000;
        boolean started = false;
        while (System.currentTimeMillis() < bailout) {
            if (!quarkusProcess.isAlive()) {
                throw new RuntimeException("Failed to start target quarkus application, process has exited");
            }
            try {
                Thread.sleep(100);
                if (startedSupplier.get()) {
                    isSsl = false;
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

    private static String guessPath(Class<?> testClass) {
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

    private static String guessPath(final URL url) {
        if (url == null) {
            return null;
        }
        if (url.getProtocol().equals("file") && url.getPath().endsWith("test-classes/")) {
            //we have the maven test classes dir
            File testClasses = new File(url.getPath());
            for (File file : testClasses.getParentFile().listFiles()) {
                if (isNativeExecutable(file)) {
                    logGuessedPath(file.getAbsolutePath());
                    return file.getAbsolutePath();
                }
            }
        } else if (url.getProtocol().equals("file") && url.getPath().endsWith("test/")) {
            //we have the gradle test classes dir, build/classes/java/test
            File testClasses = new File(url.getPath());
            for (File file : testClasses.getParentFile().getParentFile().getParentFile().listFiles()) {
                if (isNativeExecutable(file)) {
                    logGuessedPath(file.getAbsolutePath());
                    return file.getAbsolutePath();
                }
            }
        } else if (url.getProtocol().equals("file") && url.getPath().contains("/target/surefire/")) {
            //this will make mvn failsafe:integration-test work
            String path = url.getPath();
            int index = path.lastIndexOf("/target/");
            File targetDir = new File(path.substring(0, index) + "/target/");
            for (File file : targetDir.listFiles()) {
                if (isNativeExecutable(file)) {
                    logGuessedPath(file.getAbsolutePath());
                    return file.getAbsolutePath();
                }
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

    public boolean listensOnSsl() {
        return isSsl;
    }

    public void addSystemProperties(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    @Override
    public void close() {
        quarkusProcess.destroy();
    }
}
