package io.quarkus.test.aesh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private LinkedBlockingQueue<Object> signalQueue;

    private Thread replThread;
    private volatile LaunchResult launchResult;

    public AeshLauncherImpl(QuarkusMainLauncher mainLauncher) {
        this.mainLauncher = mainLauncher;
    }

    @Override
    public void launch(String... args) {
        PipedInputStream stdinReader;
        try {
            stdinWriter = new PipedOutputStream();
            stdinReader = new PipedInputStream(stdinWriter, 4096);
            stdoutCapture = new ByteArrayOutputStream();
            signalQueue = new LinkedBlockingQueue<>();
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
                stdinReader, stdoutCapture, signalQueue);
        replThread.start();

        // Wait for the console to initialize and display the prompt.
        // Polls the output buffer instead of using a fixed sleep,
        // following the same pattern as LauncherUtil's CaptureListeningDataReader.
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (stdoutCapture.size() > 0) {
                String output = stripAnsi(stdoutCapture.toString(StandardCharsets.UTF_8));
                if (output.contains("$ ")) {
                    break;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public String executeCommand(String command) {
        return executeCommand(command, DEFAULT_TIMEOUT);
    }

    @Override
    public String executeCommand(String command, Duration timeout) {
        // Clear the output buffer and signal queue
        stdoutCapture.reset();
        signalQueue.clear();

        // Send the command
        try {
            stdinWriter.write((command + "\n").getBytes(StandardCharsets.UTF_8));
            stdinWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write command to REPL stdin", e);
        }

        try {
            Object signal = signalQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (signal == null) {
                throw new RuntimeException(
                        "Command '" + command + "' did not complete within " + timeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for command: " + command, e);
        }

        return stripAnsi(stdoutCapture.toString(StandardCharsets.UTF_8));
    }

    @Override
    public String getErrorOutput() {
        // Errors go to the same output stream in this implementation
        return "";
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
