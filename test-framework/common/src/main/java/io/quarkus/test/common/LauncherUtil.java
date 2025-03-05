package io.quarkus.test.common;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.quarkus.utilities.OS;

public final class LauncherUtil {

    public static final int LOG_CHECK_INTERVAL = 50;

    private LauncherUtil() {
    }

    @Deprecated(forRemoval = true, since = "3.17")
    public static Config installAndGetSomeConfig() {
        return ConfigProvider.getConfig();
    }

    /**
     * Launches a process using the supplied arguments and makes sure the process's output is drained to standard out
     * <p>
     * Implementation detail: Avoid using ProcessBuilder's redirect here because it causes problems with Maven Failsafe
     * as can be seen in <a href="https://github.com/quarkusio/quarkus/issues/33229">here</a>
     */
    static Process launchProcessAndDrainIO(List<String> args, Map<String, String> env) throws IOException {
        Process process = launchProcess(args, env);
        new Thread(new ProcessReader(process.getInputStream())).start();
        new Thread(new ProcessReader(process.getErrorStream())).start();
        return process;
    }

    /**
     * Launches a process using the supplied arguments but does drain the IO
     */
    static Process launchProcess(List<String> args, Map<String, String> env) throws IOException {
        Process process;
        if (env.isEmpty()) {
            process = Runtime.getRuntime().exec(args.toArray(new String[0]));
        } else {
            Map<String, String> currentEnv = System.getenv();
            Map<String, String> finalEnv = new HashMap<>(currentEnv);
            finalEnv.putAll(env);
            String[] envArray = new String[finalEnv.size()];
            int i = 0;
            for (var entry : finalEnv.entrySet()) {
                envArray[i] = entry.getKey() + "=" + entry.getValue();
                i++;
            }
            process = Runtime.getRuntime().exec(args.toArray(new String[0]), envArray);
        }
        return process;
    }

    /**
     * Waits (for a maximum of {@param waitTimeSeconds} seconds) until the launched process indicates the address it is
     * listening on.
     * If the wait time is exceeded an {@code IllegalStateException} is thrown.
     */
    static ListeningAddress waitForCapturedListeningData(Process quarkusProcess, Path logFile, long waitTimeSeconds) {
        ensureProcessIsAlive(quarkusProcess);

        CountDownLatch signal = new CountDownLatch(1);
        AtomicReference<ListeningAddress> resultReference = new AtomicReference<>();
        CaptureListeningDataReader captureListeningDataReader = new CaptureListeningDataReader(logFile,
                Duration.ofSeconds(waitTimeSeconds), signal, resultReference);
        new Thread(captureListeningDataReader, "capture-listening-data").start();
        try {
            signal.await(waitTimeSeconds + 2, TimeUnit.SECONDS); // wait enough for the signal to be given by the capturing thread
            ListeningAddress result = resultReference.get();
            if (result != null) {
                return result;
            }
            // a null result means that we could not determine the status of the process so we need to abort testing
            destroyProcess(quarkusProcess);
            throw new IllegalStateException(
                    "Unable to determine the status of the running process. See the above logs for details");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting to capture listening process port and protocol");
        }
    }

    private static void ensureProcessIsAlive(Process quarkusProcess) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
            throw new RuntimeException(
                    "Interrupted while waiting to determine the status of process '" + quarkusProcess.pid() + "'.");
        }
        if (!quarkusProcess.isAlive()) {
            int exit = quarkusProcess.exitValue();
            String message = "Unable to successfully launch process '" + quarkusProcess.pid() + "'. Exit code is: '"
                    + exit + "'.";
            if (OS.determineOS().equals(OS.MAC) && exit == 126) {
                message += System.lineSeparator()
                        + "This may be caused by building the native binary in a Linux container while the host is macOS.";
            }
            throw new RuntimeException(message);
        }
    }

    /**
     * Try to destroy the process normally a few times
     * and resort to forceful destruction if necessary
     */
    static void destroyProcess(Process quarkusProcess) {
        quarkusProcess.destroy();
        int i = 0;
        while (i++ < 10) {
            try {
                Thread.sleep(LOG_CHECK_INTERVAL);
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

    static void destroyProcess(ProcessHandle quarkusProcess) {
        try {
            CompletableFuture<ProcessHandle> exit = quarkusProcess.onExit();
            if (!quarkusProcess.destroy()) {
                quarkusProcess.destroyForcibly();
                return;
            }
            exit.get(LOG_CHECK_INTERVAL * 10, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
        }
        if (quarkusProcess.isAlive()) {
            quarkusProcess.destroyForcibly();
        }
    }

    static void destroyProcess(Process process, boolean children) {
        if (!children) {
            destroyProcess(process);
            return;
        }
        process.descendants().forEach((p) -> destroyProcess(p));
        destroyProcess(process);
    }

    static Function<IntegrationTestStartedNotifier.Context, IntegrationTestStartedNotifier.Result> createStartedFunction() {
        List<IntegrationTestStartedNotifier> startedNotifiers = new ArrayList<>();
        for (IntegrationTestStartedNotifier i : ServiceLoader.load(IntegrationTestStartedNotifier.class)) {
            startedNotifiers.add(i);
        }
        if (startedNotifiers.isEmpty()) {
            return null;
        }
        return (ctx) -> {
            for (IntegrationTestStartedNotifier startedNotifier : startedNotifiers) {
                IntegrationTestStartedNotifier.Result result = startedNotifier.check(ctx);
                if (result.isStarted()) {
                    return result;
                }
            }
            return IntegrationTestStartedNotifier.Result.NotStarted.INSTANCE;
        };
    }

    /**
     * Waits for {@param startedFunction} to indicate that the application has started.
     *
     * @return the {@link io.quarkus.test.common.IntegrationTestStartedNotifier.Result} indicating a successful start
     * @throws RuntimeException if no successful start was indicated by {@param startedFunction}
     */
    static IntegrationTestStartedNotifier.Result waitForStartedFunction(
            Function<IntegrationTestStartedNotifier.Context, IntegrationTestStartedNotifier.Result> startedFunction,
            Process quarkusProcess, long waitTimeSeconds, Path logFile) {
        long bailout = System.currentTimeMillis() + waitTimeSeconds * 1000;
        IntegrationTestStartedNotifier.Result result = null;
        SimpleContext context = new SimpleContext(logFile);
        while (System.currentTimeMillis() < bailout) {
            if (!quarkusProcess.isAlive()) {
                throw new RuntimeException("Failed to start target quarkus application, process has exited");
            }
            try {
                Thread.sleep(100);
                result = startedFunction.apply(context);
                if (result.isStarted()) {
                    break;
                }
            } catch (Exception ignored) {

            }
        }
        if (result == null) {
            destroyProcess(quarkusProcess);
            throw new RuntimeException("Unable to start target quarkus application " + waitTimeSeconds + "s");
        }
        return result;
    }

    /**
     * Updates the configuration necessary to make all test systems knowledgeable about the port on which the launched
     * process is listening
     */
    static void updateConfigForPort(Integer effectivePort) {
        if (effectivePort != null) {
            System.setProperty("quarkus.http.port", effectivePort.toString()); //set the port as a system property in order to have it applied to Config
            System.setProperty("quarkus.http.test-port", effectivePort.toString()); // needed for RestAssuredManager
            System.clearProperty("test.url"); // make sure the old value does not interfere with setting the new one
            System.setProperty("test.url", TestHTTPResourceManager.getUri());
        }
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
        private final Pattern listeningRegex = Pattern.compile("Listening on:\\s+(https?)://[^:]*:(\\d+)");
        private final Pattern startedRegex = Pattern.compile(".*Quarkus .* started in \\d+.*s.*");

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
                unableToDetermineData("Log file '" + processOutput.toAbsolutePath() + "' was not created. "
                        + "Check if the options quarkus.log.level and quarkus.log.file.level are at least INFO (or more verbose).");
                return;
            }

            long bailoutTime = System.currentTimeMillis() + waitTime.toMillis();
            try (BufferedReader reader = new BufferedReader(new FileReader(processOutput.toFile()))) {
                long timeStarted = Long.MAX_VALUE;
                boolean started = false;
                // generally, we want to start as soon as info about Quarkus having started is printed
                // but just in case the line with http host and port is printed later, let's wait a bit more
                while (true) {
                    if (reader.ready()) { // avoid blocking as the input is a file which continually gets more data added
                        String line = reader.readLine();

                        if (startedRegex.matcher(line).matches()) {
                            timeStarted = System.currentTimeMillis();
                            started = true;
                        }

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

                        long now = System.currentTimeMillis();
                        // if we have seen info that the app is started in the log a while ago
                        // or waiting the next check interval will exceed the bailout time, it's time to finish waiting:
                        if (now + LOG_CHECK_INTERVAL > bailoutTime || now - 2 * LOG_CHECK_INTERVAL > timeStarted) {
                            if (started) {
                                dataDetermined(null, null); // no http, all is null
                            } else {
                                unableToDetermineData("Waited " + waitTime.getSeconds() + " seconds for " + processOutput
                                        + " to contain info about the listening port and protocol but no such info was found. "
                                        + "Check if the options quarkus.log.level and quarkus.log.file.level are at least INFO (or more verbose).");
                            }
                            return;
                        }

                        try {
                            Thread.sleep(LOG_CHECK_INTERVAL);
                        } catch (InterruptedException e) {
                            unableToDetermineData(
                                    "Thread interrupted while waiting for more data to become available in process output file: "
                                            + processOutput.toAbsolutePath());
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
            long bailoutTime = System.currentTimeMillis() + waitTime.toMillis();
            while (System.currentTimeMillis() < bailoutTime) {
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

    private static class SimpleContext implements IntegrationTestStartedNotifier.Context {
        private final Path logFile;

        public SimpleContext(Path logFile) {
            this.logFile = logFile;
        }

        @Override
        public Path logFile() {
            return logFile;
        }
    }
}
