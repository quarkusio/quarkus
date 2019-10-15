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
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.test.common.http.TestHTTPResourceManager;

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

    public NativeImageLauncher(Class<?> testClass) {
        this(testClass,
                ConfigProvider.getConfig().getOptionalValue("quarkus.http.test-port", Integer.class).orElse(DEFAULT_PORT),
                ConfigProvider.getConfig().getOptionalValue("quarkus.test.native-image-wait-time", Long.class)
                        .orElse(DEFAULT_IMAGE_WAIT_TIME),
                ConfigProvider.getConfig().getOptionalValue("quarkus.test.native-image-profile", String.class)
                        .orElse(null));
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
                }
            }
        }

        throw new RuntimeException("Unable to find native image, make sure native.image.path is set");
    }

    private static void logGuessedPath(String guessedPath) {
        String errorString = "\n=native.image.path was not set, making a guess that  " + guessedPath
                + " is the correct native image=";
        for (int i = 0; i < errorString.length(); ++i) {
            System.err.print("=");
        }
        System.err.println(errorString);
        for (int i = 0; i < errorString.length(); ++i) {
            System.err.print("=");
        }
        System.err.println();
    }

    private void waitForQuarkus() {
        long bailout = System.currentTimeMillis() + imageWaitTime * 1000;

        while (System.currentTimeMillis() < bailout) {
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
