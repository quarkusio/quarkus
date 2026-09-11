package io.quarkus.test.aesh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.quarkus.aesh.runtime.AeshTestConnectionHolder;
import io.quarkus.aesh.runtime.AeshTestConnectionHolder.AeshTestThread;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;

/**
 * Default implementation of {@link AeshLauncher} that uses pipes and a
 * {@link LinkedBlockingQueue} to communicate with the Aesh REPL across
 * the Quarkus split classloader boundary.
 */
public class AeshLauncherImpl implements AeshLauncher {

    private final QuarkusMainLauncher mainLauncher;

    private PipedOutputStream stdinWriter;
    private ByteArrayOutputStream stdoutCapture;
    private ByteArrayOutputStream commandOutputCapture;
    private LinkedBlockingQueue<Object> signalQueue;
    private ConcurrentLinkedQueue<String> inputLineQueue;

    private Thread replThread;
    private volatile boolean launched;
    private volatile LaunchResult launchResult;
    private volatile int lastExitCode;
    private volatile Throwable lastError;
    private volatile List<StageResult> lastStageResults = List.of();
    private volatile String lastCommandOutput;
    private final StringBuilder accumulatedOutput = new StringBuilder();

    public AeshLauncherImpl(QuarkusMainLauncher mainLauncher) {
        this.mainLauncher = mainLauncher;
    }

    @Override
    public void launch(String... args) {
        if (launched) {
            return;
        }
        launched = true;
        PipedInputStream stdinReader;
        try {
            stdinWriter = new PipedOutputStream();
            stdinReader = new PipedInputStream(stdinWriter, 4096);
            stdoutCapture = new ByteArrayOutputStream();
            commandOutputCapture = new ByteArrayOutputStream();
            signalQueue = new LinkedBlockingQueue<>();
            inputLineQueue = new ConcurrentLinkedQueue<>();
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up test pipes", e);
        }

        ClassLoader testCl = Thread.currentThread().getContextClassLoader();
        replThread = new AeshTestThread(
                () -> {
                    launchResult = mainLauncher.launch(args);
                },
                AeshTestConnectionHolder.TEST_THREAD_NAME,
                testCl,
                stdinReader, stdoutCapture, signalQueue,
                commandOutputCapture, inputLineQueue);
        replThread.start();

        // Wait for the REPL thread to signal that it is about to start.
        // CliRunner offers a "ready" signal just before runner.start()
        // blocks. Command bytes written after this point are buffered in
        // the PipedInputStream until readline arms and reads them.
        try {
            Object signal = signalQueue.poll(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (signal == null) {
                throw new RuntimeException("REPL did not start within " + DEFAULT_TIMEOUT);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public String execute(String command, ExecuteOptions options) {
        if (!launched) {
            launch();
        }
        // Clear the output buffers, signal queue, and last result
        stdoutCapture.reset();
        commandOutputCapture.reset();
        signalQueue.clear();
        lastExitCode = 0;
        lastError = null;
        lastStageResults = List.of();
        lastCommandOutput = null;

        // Load pre-canned input responses into the queue for invocation.inputLine()
        inputLineQueue.clear();
        inputLineQueue.addAll(options.input());

        // Send the command
        try {
            stdinWriter.write((command + "\n").getBytes(StandardCharsets.UTF_8));
            stdinWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write command to REPL stdin", e);
        }

        try {
            Object signal = signalQueue.poll(options.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (signal == null) {
                throw new RuntimeException(
                        "Command '" + command + "' did not complete within " + options.timeout());
            }
            // Extract exit code, error, and stage data from the signal.
            // The signal is Object[] { exitCode, error, stageData } from CliRunner.
            // stageData is null for single commands, List<Object[]> for pipelines.
            if (!(signal instanceof Object[] arr) || arr.length < 2 || !(arr[0] instanceof Integer exitCode)) {
                throw new RuntimeException(
                        "Unexpected signal from REPL for command '" + command + "': " + signal);
            }
            lastExitCode = exitCode;
            if (arr[1] instanceof Throwable t) {
                lastError = t;
            }
            if (arr.length > 2 && arr[2] instanceof List<?> rawStages) {
                lastStageResults = toStageResults(command, rawStages);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for command: " + command, e);
        }

        // Capture clean command output (no prompt, no echo).
        // Normalize CRLF to LF so assertions work on all platforms.
        lastCommandOutput = commandOutputCapture.toString(StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        accumulatedOutput.append(lastCommandOutput);

        // Assert the exit code matches the expected result
        if (lastExitCode != options.expectedResult().getExitCode()) {
            String msg = "Command '" + command + "' returned exit code " + lastExitCode
                    + " but expected " + options.expectedResult().getExitCode();
            if (lastError != null) {
                msg += ": " + lastError.getMessage();
            }
            throw new AssertionError(msg);
        }

        return stripAnsi(stdoutCapture.toString(StandardCharsets.UTF_8));
    }

    @Override
    public void sendInput(String input) {
        if (!launched) {
            launch();
        }
        try {
            stdinWriter.write((input + "\n").getBytes(StandardCharsets.UTF_8));
            stdinWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write input to REPL stdin", e);
        }
    }

    @Override
    public int getLastExitCode() {
        return lastExitCode;
    }

    @Override
    public String getCommandOutput() {
        return lastCommandOutput != null ? lastCommandOutput : "";
    }

    @Override
    public String getOutput() {
        return accumulatedOutput.toString();
    }

    @Override
    public void resetOutput() {
        accumulatedOutput.setLength(0);
    }

    @Override
    public String getErrorOutput() {
        Throwable err = lastError;
        return err != null ? err.getMessage() : "";
    }

    @Override
    public Throwable getLastError() {
        return lastError;
    }

    @Override
    public List<StageResult> getStageResults() {
        return lastStageResults;
    }

    @Override
    public LaunchResult getLaunchResult() {
        return launchResult;
    }

    @Override
    public boolean isRunning() {
        return replThread != null && replThread.isAlive();
    }

    @Override
    public boolean waitForExit(Duration timeout) {
        if (replThread == null) {
            return true;
        }
        try {
            replThread.join(timeout.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return !replThread.isAlive();
    }

    @Override
    public void exit() {
        try {
            stdinWriter.write("exit\n".getBytes(StandardCharsets.UTF_8));
            stdinWriter.flush();
            // Close the writer to signal EOF to the PipedInputStream reader.
            // This is needed because PipedInputStream.read() blocks with wait()
            // and closing the InputStream itself does not unblock it.
            stdinWriter.close();
        } catch (IOException e) {
            // May already be closed
        }

        if (replThread != null) {
            try {
                replThread.join(10_000);
                if (replThread.isAlive()) {
                    replThread.interrupt();
                    replThread.join(2_000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Convert raw stage data from the signal queue to {@link StageResult} records.
     * Each raw entry is {@code Object[] {commandName, stageIndex, stageCount,
     * exitCode, errorMessage, errorClass, durationMs}} — all classloader-safe types.
     * Malformed entries fail fast with a descriptive error rather than a
     * {@link ClassCastException} deep in the conversion.
     */
    private static List<StageResult> toStageResults(String command, List<?> rawStages) {
        List<StageResult> results = new ArrayList<>(rawStages.size());
        for (Object raw : rawStages) {
            if (!(raw instanceof Object[] arr) || arr.length != 7
                    || !(arr[0] instanceof String commandName)
                    || !(arr[1] instanceof Integer stageIndex)
                    || !(arr[2] instanceof Integer stageCount)
                    || !(arr[3] instanceof Integer exitCode)
                    || (arr[4] != null && !(arr[4] instanceof String))
                    || (arr[5] != null && !(arr[5] instanceof String))
                    || !(arr[6] instanceof Long durationMs)) {
                throw new RuntimeException(
                        "Malformed stage data from REPL for command '" + command + "'");
            }
            results.add(new StageResult(
                    commandName,
                    stageIndex,
                    stageCount,
                    exitCode,
                    (String) arr[4],
                    (String) arr[5],
                    durationMs));
        }
        return Collections.unmodifiableList(results);
    }

    /**
     * Strip ANSI escape sequences and carriage returns from the output
     * for clean assertions. The {@code \r} stripping handles Windows-style
     * line endings ({@code \r\n}).
     */
    private static String stripAnsi(String text) {
        return text.replaceAll("\\u001B\\[(.*?)[a-zA-Z]|\\r", "");
    }
}
