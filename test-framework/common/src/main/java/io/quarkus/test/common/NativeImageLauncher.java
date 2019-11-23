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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.smallrye.config.SmallRyeConfig;

public class NativeImageLauncher implements Closeable {

    private static final int DEFAULT_PORT = 8081;
    private static final long DEFAULT_IMAGE_WAIT_TIME = 60;

    private final Class<?> testClass;
    private final String profile;
    private Process quarkusProcess;
    private final int port;
    private final long imageWaitTime;
    private final Map<String, String> systemProps = new HashMap<>();
    private List<NativeImageStartedNotifier> startedNotifiers;

    private NativeImageLauncher(Class<?> testClass, Config config) {
        this(testClass,
                config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
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

    public NativeImageLauncher(Class<?> testClass, int port, long imageWaitTime, String profile) {
        this.testClass = testClass;
        this.port = port;
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
        new Thread(new ProcessReader(quarkusProcess.getInputStream())).start();
        new Thread(new ProcessReader(quarkusProcess.getErrorStream())).start();

        waitForQuarkus();
    }

    private static String guessPath(Class<?> testClass) {
        //ok, lets make a guess
        //this is a horrible hack, but it is intended to make this work in IDE's

        ClassLoader cl = testClass.getClassLoader();

        if (cl instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) cl).getURLs();
            for (URL url : urls) {
                if (url.getProtocol().equals("file") && url.getPath().endsWith("test-classes/")) {
                    //we have the maven test classes dir
                    File testClasses = new File(url.getPath());
                    for (File file : testClasses.getParentFile().listFiles()) {
                        if (file.getName().endsWith("-runner")) {
                            logGuessedPath(file.getAbsolutePath());
                            return file.getAbsolutePath();
                        }
                    }
                } else if (url.getProtocol().equals("file") && url.getPath().endsWith("test/")) {
                    //we have the gradle test classes dir, build/classes/java/test
                    File testClasses = new File(url.getPath());
                    for (File file : testClasses.getParentFile().getParentFile().getParentFile().listFiles()) {
                        if (file.getName().endsWith("-runner")) {
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
                        if (file.getName().endsWith("-runner")) {
                            logGuessedPath(file.getAbsolutePath());
                            return file.getAbsolutePath();
                        }
                    }

                }
            }
        }

        throw new RuntimeException(
                "Unable to automatically find native image, please set the native.image.path to the native executable you wish to test");
    }

    private static void logGuessedPath(String guessedPath) {
        System.err.println("======================================================================================");
        System.err.println("  native.image.path was not set, making a guess for the correct path of native image");
        System.err.println("  guessed path: " + guessedPath);
        System.err.println("======================================================================================");
    }

    private void waitForQuarkus() {
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
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress("localhost", port));
                    return;
                }
            } catch (Exception expected) {
            }
        }

        throw new RuntimeException("Unable to start native image in " + imageWaitTime + "s");
    }

    public void addSystemProperties(Map<String, String> systemProps) {
        this.systemProps.putAll(systemProps);
    }

    private static final class ProcessReader implements Runnable {

        private final InputStream inputStream;

        private ProcessReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            byte[] b = new byte[100];
            int i;
            try {
                while ((i = inputStream.read(b)) > 0) {
                    System.out.print(new String(b, 0, i, StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                //ignore
            }
        }
    }

    @Override
    public void close() {
        quarkusProcess.destroy();
    }
}
