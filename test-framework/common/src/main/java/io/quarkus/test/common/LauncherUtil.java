package io.quarkus.test.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.smallrye.config.SmallRyeConfig;

final class LauncherUtil {

    private LauncherUtil() {
    }

    static Config installAndGetSomeConfig() {
        final SmallRyeConfig config = ConfigUtils.configBuilder(false, LaunchMode.NORMAL).build();
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

    /**
     * Launches a process using the supplied arguments and makes sure the process's output is drained to standard out
     */
    static Process launchProcess(List<String> args) throws IOException {
        Process process = Runtime.getRuntime().exec(args.toArray(new String[0]));
        new Thread(new ProcessReader(process.getInputStream())).start();
        new Thread(new ProcessReader(process.getErrorStream())).start();
        return process;
    }

    /**
     * Waits (for a maximum of {@param waitTimeSeconds} seconds) until the launched process indicates the address it is
     * listening on.
     * If the wait time is exceeded an {@code IllegalStateException} is thrown.
     */
    static ListeningAddress waitForCapturedListeningData(Process quarkusProcess, Path logFile, long waitTimeSeconds) {
        CountDownLatch signal = new CountDownLatch(1);
        AtomicReference<ListeningAddress> resultReference = new AtomicReference<>();
        CaptureListeningDataReader captureListeningDataReader = new CaptureListeningDataReader(logFile,
                Duration.ofSeconds(waitTimeSeconds), signal, resultReference);
        new Thread(captureListeningDataReader, "capture-listening-data").start();
        try {
            signal.await(10, TimeUnit.SECONDS);
            ListeningAddress result = resultReference.get();
            if (result != null) {
                return result;
            }
            destroyProcess(quarkusProcess);
            throw new IllegalStateException(
                    "Unable to determine the status of the running process. See the above logs for details");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting to capture listening process port and protocol");
        }
    }

    /**
     * Try to destroy the process normally a few times
     * and resort to forceful destruction if necessary
     */
    private static void destroyProcess(Process quarkusProcess) {
        quarkusProcess.destroy();
        int i = 0;
        while (i++ < 10) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {

            }
            if (!quarkusProcess.isAlive()) {
                break;
            }
        }

        if (quarkusProcess.isAlive()) {
            quarkusProcess.destroyForcibly();
        }
    }

    /**
     * Updates the configuration necessary to make all test systems knowledgeable about the port on which the launched
     * process is listening
     */
    static void updateConfigForPort(Integer effectivePort) {
        System.setProperty("quarkus.http.port", effectivePort.toString()); //set the port as a system property in order to have it applied to Config
        System.setProperty("quarkus.http.test-port", effectivePort.toString()); // needed for RestAssuredManager
        installAndGetSomeConfig(); // reinitialize the configuration to make sure the actual port is used
        System.setProperty("test.url", TestHTTPResourceManager.getUri());
    }

    /**
     * Thread that reads a process output file looking for the line that indicates the address the application
     * is listening on.
     */
    private static class CaptureListeningDataReader implements Runnable {

        private final Path processOutput;
        private final Duration waitTime;
        private final CountDownLatch signal;
        private final AtomicReference<ListeningAddress> resultReference;
        private final Pattern listeningRegex = Pattern.compile("Listening on:\\s+(https?)://\\S*:(\\d+)");

        public CaptureListeningDataReader(Path processOutput, Duration waitTime, CountDownLatch signal,
                AtomicReference<ListeningAddress> resultReference) {
            this.processOutput = processOutput;
            this.waitTime = waitTime;
            this.signal = signal;
            this.resultReference = resultReference;
        }

        @Override
        public void run() {
            if (!ensureProcessOutputFileExists()) {
                return;
            }

            long bailoutTime = System.currentTimeMillis() + waitTime.toMillis();
            try (BufferedReader reader = new BufferedReader(new FileReader(processOutput.toFile()))) {
                while (true) {
                    if (reader.ready()) { // avoid blocking as the input is a file which continually gets more data added
                        String line = reader.readLine();
                        Matcher regexMatcher = listeningRegex.matcher(line);
                        if (regexMatcher.find()) {
                            dataDetermined(regexMatcher.group(1), Integer.valueOf(regexMatcher.group(2)));
                            return;
                        } else {
                            if (line.contains("Failed to start application (with profile")) {
                                unableToDetermineData("Application was not started: " + line);
                                return;
                            }
                        }
                    } else {
                        //wait until there is more of the file for us to read
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            unableToDetermineData(
                                    "Thread interrupted while waiting for more data to become available in proccess output file: "
                                            + processOutput.toAbsolutePath().toString());
                            return;
                        }
                        if (System.currentTimeMillis() > bailoutTime) {
                            unableToDetermineData("Waited " + waitTime.getSeconds() + "seconds for " + processOutput
                                    + " to contain info about the listening port and protocol but no such info was found");
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                unableToDetermineData("Exception occurred while reading process output from file " + processOutput);
                e.printStackTrace();
            }
        }

        private boolean ensureProcessOutputFileExists() {
            int i = 0;
            while (i++ < 25) {
                if (Files.exists(processOutput)) {
                    return true;
                } else {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        unableToDetermineData("Thread interrupted while waiting for process output file to be created");
                        return false;
                    }
                }
            }
            return false;
        }

        private void dataDetermined(String protocolValue, Integer portValue) {
            this.resultReference.set(new ListeningAddress(portValue, protocolValue));
            signal.countDown();
        }

        private void unableToDetermineData(String errorMessage) {
            System.err.println(errorMessage);
            this.resultReference.set(null);
            signal.countDown();
        }
    }

    /**
     * Used to drain the input of a launched process
     */
    private static class ProcessReader implements Runnable {

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
}
