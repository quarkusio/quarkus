package io.quarkus.test.common;

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
import java.util.ServiceLoader;
import java.util.function.Supplier;

import io.quarkus.test.common.http.TestHTTPResourceManager;

public class DefaultNativeImageLauncher implements NativeImageLauncher {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");

    private int httpPort;
    private int httpsPort;
    private long waitTimeSeconds;
    private String testProfile;
    private String nativeImagePath;
    private Class<?> testClass;

    private Process quarkusProcess;
    private final Map<String, String> systemProps = new HashMap<>();
    private Supplier<Boolean> startedSupplier = null;

    private boolean isSsl;

    @Override
    public void init(NativeImageInitContext initContext) {
        this.httpPort = initContext.httpPort();
        this.httpsPort = initContext.httpsPort();
        this.waitTimeSeconds = initContext.waitTime().getSeconds();
        this.testProfile = initContext.testProfile();
        this.nativeImagePath = initContext.nativeImagePath();
        this.testClass = initContext.testClass();

        List<NativeImageStartedNotifier> startedNotifiers = new ArrayList<>();
        for (NativeImageStartedNotifier i : ServiceLoader.load(NativeImageStartedNotifier.class)) {
            startedNotifiers.add(i);
        }
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

        if (nativeImagePath == null) {
            nativeImagePath = guessPath(testClass);
        }
        List<String> args = new ArrayList<>();
        args.add(nativeImagePath);
        args.add("-Dquarkus.http.port=" + httpPort);
        args.add("-Dquarkus.http.ssl-port=" + httpsPort);
        // this won't be correct when using the random port but it's really only used by us for the rest client tests
        // in the main module, since those tests hit the application itself
        args.add("-Dtest.url=" + TestHTTPResourceManager.getUri());
        Path logFile = PropertyTestUtil.getLogFilePath();
        args.add("-Dquarkus.log.file.path=" + logFile.toAbsolutePath().toString());
        args.add("-Dquarkus.log.file.enable=true");
        if (testProfile != null) {
            args.add("-Dquarkus.profile=" + testProfile);
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

    @Override
    public void includeAsSysProps(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    @Override
    public void close() {
        quarkusProcess.destroy();
    }
}
