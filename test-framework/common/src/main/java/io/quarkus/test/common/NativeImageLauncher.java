package io.quarkus.test.common;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.ServiceLoader;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.wildfly.common.lock.Locks;

import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.smallrye.config.SmallRyeConfig;

public class NativeImageLauncher implements Closeable {

    private static final int DEFAULT_PORT = 8081;
    private static final int DEFAULT_HTTPS_PORT = 8444;
    private static final long DEFAULT_IMAGE_WAIT_TIME = 60;

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows");

    private final Class<?> testClass;
    private final String profile;
    private Process quarkusProcess;
    private int port;
    private final int httpsPort;
    private final long imageWaitTime;
    private final Map<String, String> systemProps = new HashMap<>();
    private List<NativeImageStartedNotifier> startedNotifiers;

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

    private static Config installAndGetSomeConfig() {
        final SmallRyeConfig config = ConfigUtils.configBuilder(false).build();
        QuarkusConfigFactory.setConfig(config);
        final ConfigProviderResolver cpr = ConfigProviderResolver.instance();
        try {
            final Config installed = cpr.getConfig();
            if (installed != config) {
                cpr.releaseConfig(installed);
            }
        } catch (IllegalStateException ignored) {
        }
        return config;
    }

    public NativeImageLauncher(Class<?> testClass, int port, int httpsPort, long imageWaitTime, String profile) {
        this.testClass = testClass;
        this.port = port;
        this.httpsPort = httpsPort;
        this.imageWaitTime = imageWaitTime;
        List<NativeImageStartedNotifier> startedNotifiers = new ArrayList<>();
        for (NativeImageStartedNotifier i : ServiceLoader.load(NativeImageStartedNotifier.class)) {
            startedNotifiers.add(i);
        }
        this.startedNotifiers = startedNotifiers;
        this.profile = profile;
    }

    public void start() throws IOException {

        System.setProperty("test.url", TestHTTPResourceManager.getUri());

        String path = System.getProperty("native.image.path");
        if (path == null) {
            path = guessPath(testClass);
        }
        List<String> args = new ArrayList<>();
        args.add(path);
        args.add("-Dquarkus.http.port=" + port);
        args.add("-Dquarkus.http.ssl-port=" + httpsPort);
        // this won't be correct when using the random port but it's really only used by us for the rest client tests
        // in the main module, since those tests hit the application itself
        args.add("-Dtest.url=" + TestHTTPResourceManager.getUri());
        args.add("-Dquarkus.log.file.path=" + PropertyTestUtil.getLogFileLocation());
        if (profile != null) {
            args.add("-Dquarkus.profile=" + profile);
        }
        for (Map.Entry<String, String> e : systemProps.entrySet()) {
            args.add("-D" + e.getKey() + "=" + e.getValue());
        }

        System.out.println("Executing " + args);

        quarkusProcess = Runtime.getRuntime().exec(args.toArray(new String[args.size()]));

        PortCapturingProcessReader portCapturingProcessReader = null;
        if (port == 0) {
            // when the port is 0, then the application starts on a random port and the only way for us to figure it out
            // is to capture the output
            portCapturingProcessReader = new PortCapturingProcessReader(quarkusProcess.getInputStream());
        }
        new Thread(portCapturingProcessReader != null ? portCapturingProcessReader
                : new ProcessReader(quarkusProcess.getInputStream())).start();
        new Thread(new ProcessReader(quarkusProcess.getErrorStream())).start();

        if (portCapturingProcessReader != null) {
            try {
                portCapturingProcessReader.awaitForPort();
            } catch (InterruptedException ignored) {

            }
            if (portCapturingProcessReader.port == null) {
                quarkusProcess.destroy();
                throw new RuntimeException("Unable to determine actual running port as dynamic port was used");
            }

            waitForQuarkus(portCapturingProcessReader.port);

            System.setProperty("quarkus.http.port", portCapturingProcessReader.port.toString()); //set the port as a system property in order to have it applied to Config
            System.setProperty("quarkus.http.test-port", portCapturingProcessReader.port.toString()); // needed for RestAssuredManager
            port = portCapturingProcessReader.port;
            installAndGetSomeConfig(); // reinitialize the configuration to make sure the actual port is used
            System.setProperty("test.url", TestHTTPResourceManager.getUri());
        } else {
            waitForQuarkus(port);
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

    private void waitForQuarkus(int port) {
        long bailout = System.currentTimeMillis() + imageWaitTime * 1000;

        while (System.currentTimeMillis() < bailout) {
            if (!quarkusProcess.isAlive()) {
                throw new RuntimeException("Failed to start native image, process has exited");
            }
            try {
                Thread.sleep(100);
                for (NativeImageStartedNotifier i : startedNotifiers) {
                    if (i.isNativeImageStarted()) {
                        return;
                    }
                }
                try {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress("localhost", port));
                        //SSL is bound after https
                        //we add a small delay to make sure SSL is available if installed
                        Thread.sleep(100);
                        return;
                    }
                } catch (Exception expected) {
                }
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress("localhost", httpsPort));
                    return;
                }
            } catch (Exception expected) {
            }
        }
        quarkusProcess.destroyForcibly();
        throw new RuntimeException("Unable to start native image in " + imageWaitTime + "s");
    }

    public boolean isDefaultSsl() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", port));
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    public void addSystemProperties(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    private static class ProcessReader implements Runnable {

        private final InputStream inputStream;

        private ProcessReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            handleStart();
            byte[] b = new byte[100];
            int i;
            try {
                while ((i = inputStream.read(b)) > 0) {
                    String str = new String(b, 0, i, StandardCharsets.UTF_8);
                    System.out.print(str);
                    handleString(str);
                }
            } catch (IOException e) {
                handleError(e);
            }
        }

        protected void handleStart() {

        }

        protected void handleString(String str) {

        }

        protected void handleError(IOException e) {

        }
    }

    private static final class PortCapturingProcessReader extends ProcessReader {
        private Integer port;

        private boolean portDetermined = false;
        private StringBuilder sb = new StringBuilder();
        private final Lock lock = Locks.reentrantLock();
        private final Condition portDeterminedCondition = lock.newCondition();
        private final Pattern portRegex = Pattern.compile("Listening on:\\s+https?://.*:(\\d+)");

        private PortCapturingProcessReader(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        protected void handleStart() {
            lock.lock();
        }

        @Override
        protected void handleString(String str) {
            if (portDetermined) { // we are done with determining the port
                return;
            }
            sb.append(str);
            String currentOutput = sb.toString();
            Matcher regexMatcher = portRegex.matcher(currentOutput);
            if (!regexMatcher.find()) { // haven't read enough data yet
                if (currentOutput.contains("Exception")) {
                    portDetermined(null);
                }
                return;
            }
            portDetermined(Integer.valueOf(regexMatcher.group(1)));
        }

        private void portDetermined(Integer portValue) {
            this.port = portValue;
            try {
                portDetermined = true;
                sb = null;
                portDeterminedCondition.signal();
            } finally {
                lock.unlock();
            }
        }

        @Override
        protected void handleError(IOException e) {
            if (!portDetermined) {
                portDetermined(null);
            }
        }

        public void awaitForPort() throws InterruptedException {
            lock.lock();
            try {
                while (!portDetermined) {
                    portDeterminedCondition.await();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void close() {
        quarkusProcess.destroy();
    }
}
