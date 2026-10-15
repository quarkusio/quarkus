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
import java.util.Map;
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
    // Last readline-arm timestamp observed right after the previous command
    // completed (-1 when no command completed yet). Compared against the live
    // value on timeout: an unchanged value proves re-arming never ran after
    // the previous command, as opposed to running without effect.
    private volatile long armNanosAtLastCompletion = -1;
    // Baselines for the re-arm tripwire: handler-replacement count and close
    // timestamp at the previous acceptance. Every completed command advances
    // the count by at least two (submit stub plus re-arm) unless the console
    // closed instead.
    private volatile long armCountAtLastCompletion;
    private volatile long closeNanosAtLastCompletion;
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
                throw new RuntimeException("REPL did not start within " + DEFAULT_TIMEOUT + ". "
                        + diagnoseTimeout("waiting for REPL ready signal"));
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
        // Fail fast if the REPL input reader died in an earlier command:
        // anything written now would never be delivered.
        Throwable readerDeath = ((AeshTestThread) replThread).readerDeath().get();
        if (readerDeath != null) {
            throw new RuntimeException(
                    "Cannot execute '" + command + "': the REPL input reader thread died",
                    readerDeath);
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
            Object signal = pollForSignal(command, options.timeout());
            // Snapshot the arm time at acceptance: comparing it with the live
            // value on a later timeout tells whether re-arming ran after this
            // command completed. (Snapshotting at send time would be useless —
            // the previous re-arm already ran before the signal arrived.)
            armNanosAtLastCompletion = ((AeshTestThread) replThread).lastReadlineArmNanos().get();
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

        // Re-arm tripwire, only for commands that actually succeeded: every
        // command submitted as a line installs its submit stub, and a
        // completed command is followed by either a re-arm or a console
        // close — so the handler-replacement count must advance by at least
        // two since the previous acceptance (or the close timestamp must
        // have advanced instead). A skipped post-completion re-arm would
        // otherwise surface one command later as an undebuggable hang
        // (see https://github.com/aeshell/aesh/issues/634).
        AeshTestThread testThread = (AeshTestThread) replThread;
        long armNow = testThread.armCount().get();
        long closeNow = testThread.connectionCloseNanos().get();
        if (lastExitCode == 0 && armNow - armCountAtLastCompletion < 2
                && closeNow == closeNanosAtLastCompletion) {
            throw new AssertionError(
                    "Command '" + command + "' completed but readline was not re-armed afterwards: "
                            + (armNow - armCountAtLastCompletion)
                            + " handler replacement(s) since last completion (expected >= 2 or console close). "
                            + diagnoseTimeout("re-arm tripwire"));
        }
        armCountAtLastCompletion = armNow;
        closeNanosAtLastCompletion = closeNow;

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
     * Waits for the completion signal of the in-flight command.
     *
     * @throws RuntimeException with thread/queue diagnostics on timeout
     */
    private Object pollForSignal(String command, Duration timeout) throws InterruptedException {
        Object signal = signalQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (signal == null) {
            throw new RuntimeException(
                    "Command '" + command + "' did not complete within " + timeout + ". "
                            + diagnoseTimeout("waiting for completion signal"));
        }
        Throwable death = readerDeathCause(signal);
        if (death != null) {
            // The input reader died while delivering this command: fail
            // immediately with the cause instead of hanging on the timeout.
            throw new RuntimeException(
                    "Command '" + command + "' cannot complete: the REPL input reader thread died",
                    death);
        }
        return signal;
    }

    /**
     * Marker for reader-death signals offered by {@code AeshStreamConnection}.
     * Duplicated here because the runtime class is not visible across the
     * split classloader boundary; must stay identical to the runtime literal.
     */
    private static final String READER_DEATH_MARKER = "aesh-test-reader-death";

    /**
     * Extracts the reader-death cause from a death-marker signal, or
     * {@code null} for ordinary completion signals.
     */
    private static Throwable readerDeathCause(Object signal) {
        if (signal instanceof Object[] arr && arr.length >= 2
                && READER_DEATH_MARKER.equals(arr[0]) && arr[1] instanceof Throwable t) {
            return t;
        }
        return null;
    }

    /**
     * Describes the REPL state for timeout failure messages: whether the
     * REPL thread is alive and what it is doing, pending signals, and any
     * other aesh-related threads that might be stuck (pipe stages, reader).
     */
    private String diagnoseTimeout(String context) {
        StringBuilder sb = new StringBuilder("Diagnostics (").append(context).append("): ");
        Thread repl = replThread;
        sb.append("replAlive=").append(repl != null && repl.isAlive());
        if (repl != null) {
            sb.append(", replState=").append(repl.getState());
        }
        sb.append(", queuedSignals=").append(signalQueue != null ? signalQueue.size() : -1);
        Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
        if (repl instanceof AeshTestThread testThread) {
            sb.append(", readerDeath=").append(testThread.readerDeath().get());
            long armNow = testThread.lastReadlineArmNanos().get();
            sb.append(", lastArmNanos=").append(armNow);
            sb.append(", closeNanos=").append(testThread.connectionCloseNanos().get());
            if (armNanosAtLastCompletion >= 0) {
                sb.append(", rearmedSinceLastCompletion=").append(armNow > armNanosAtLastCompletion);
            }
        }
        // Thread stacks come last: CI truncates long failure lines, so the
        // verdict and scalars above must survive. The REPL thread is always
        // included; other aesh threads follow within a hard length budget.
        sb.append(", threads=").append(all.size()).append(" [");
        if (repl != null && all.containsKey(repl)) {
            appendThreadStack(sb, repl, all.get(repl), 8);
        }
        int others = 0;
        for (Map.Entry<Thread, StackTraceElement[]> entry : all.entrySet()) {
            Thread thread = entry.getKey();
            if (thread == repl) {
                continue;
            }
            String name = thread.getName();
            if (!name.contains("aesh") && !name.contains("Aesh")) {
                continue;
            }
            if (others++ > 0) {
                sb.append("; ");
            }
            appendThreadStack(sb, thread, entry.getValue(), 4);
            if (others >= 3 || sb.length() > 850) {
                sb.append("...[truncated]");
                break;
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Appends one thread's name, state, and top stack frames using simple
     * class names to conserve failure-message space.
     */
    private static void appendThreadStack(StringBuilder sb, Thread thread, StackTraceElement[] stack, int maxFrames) {
        sb.append(thread.getName()).append('(').append(thread.getState()).append("): ");
        if (stack != null) {
            for (int i = 0; i < Math.min(stack.length, maxFrames); i++) {
                if (i > 0) {
                    sb.append(" <- ");
                }
                String className = stack[i].getClassName();
                sb.append(className.substring(className.lastIndexOf('.') + 1))
                        .append('#').append(stack[i].getMethodName());
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
