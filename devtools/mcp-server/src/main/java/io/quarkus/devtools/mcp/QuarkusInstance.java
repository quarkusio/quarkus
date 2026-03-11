package io.quarkus.devtools.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Represents a single managed Quarkus dev mode instance.
 * Tracks the child process, captures logs in a ring buffer,
 * and detects application state from process output.
 */
public class QuarkusInstance {

    public enum Status {
        STARTING,
        RUNNING,
        CRASHED,
        STOPPED
    }

    private static final int MAX_LOG_LINES = 500;

    private final String projectDir;
    private final Process process;
    private final LinkedList<String> logBuffer = new LinkedList<>();
    private volatile Status status = Status.STARTING;
    private volatile int httpPort = -1;
    private final Thread stdoutThread;
    private final Thread stderrThread;

    public QuarkusInstance(String projectDir, Process process) {
        this.projectDir = projectDir;
        this.process = process;

        // Capture stdout
        this.stdoutThread = new Thread(() -> captureStream(process.getInputStream()), "mcp-stdout-" + projectDir);
        this.stdoutThread.setDaemon(true);
        this.stdoutThread.start();

        // Capture stderr
        this.stderrThread = new Thread(() -> captureStream(process.getErrorStream()), "mcp-stderr-" + projectDir);
        this.stderrThread.setDaemon(true);
        this.stderrThread.start();

        // Monitor process exit
        Thread exitMonitor = new Thread(() -> {
            try {
                int exitCode = process.waitFor();
                if (status != Status.STOPPED) {
                    status = Status.CRASHED;
                    appendLog("[mcp] Process exited with code: " + exitCode);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "mcp-exit-monitor-" + projectDir);
        exitMonitor.setDaemon(true);
        exitMonitor.start();
    }

    private void captureStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                appendLog(line);
                // Also echo to stderr so the user can see logs
                System.err.println(line);

                // Detect startup completion and parse port
                if (line.contains("Listening on:")) {
                    parsePort(line);
                }
                if (status == Status.STARTING && isStartedLine(line)) {
                    status = Status.RUNNING;
                }
            }
        } catch (IOException e) {
            // Stream closed — expected on process termination
        }
    }

    private boolean isStartedLine(String line) {
        // Quarkus prints this on successful startup
        return line.contains("Listening on:") || line.contains("installed features:");
    }

    private void parsePort(String line) {
        // Parse port from "Listening on: http://localhost:8085"
        int idx = line.indexOf("http://");
        if (idx < 0) {
            idx = line.indexOf("https://");
        }
        if (idx >= 0) {
            String url = line.substring(idx).trim();
            int lastColon = url.lastIndexOf(':');
            if (lastColon > 5) { // after "http:"
                try {
                    httpPort = Integer.parseInt(url.substring(lastColon + 1).replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
    }

    private synchronized void appendLog(String line) {
        logBuffer.addLast(line);
        while (logBuffer.size() > MAX_LOG_LINES) {
            logBuffer.removeFirst();
        }
    }

    public String getProjectDir() {
        return projectDir;
    }

    public Status getStatus() {
        // Re-check if process is still alive
        if (status == Status.RUNNING && !process.isAlive()) {
            status = Status.CRASHED;
        }
        return status;
    }

    public synchronized String getRecentLogs(int lines) {
        int count = Math.min(lines, logBuffer.size());
        return logBuffer.subList(logBuffer.size() - count, logBuffer.size())
                .stream()
                .collect(Collectors.joining("\n"));
    }

    /**
     * Send a character to the dev mode console stdin (e.g., 's' for restart, 'q' for quit).
     */
    public void sendInput(char c) {
        OutputStream os = process.getOutputStream();
        try {
            os.write(c);
            os.write('\n');
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to send input to Quarkus process: " + e.getMessage(), e);
        }
    }

    /**
     * Force restart by sending 's' to the dev mode console.
     */
    public void restart() {
        if (!process.isAlive()) {
            throw new IllegalStateException(
                    "Process is not running. Use quarkus/start to start a new instance.");
        }
        status = Status.STARTING;
        sendInput('s');
    }

    /**
     * Stop the Quarkus process gracefully by sending 'q', then force-kill if needed.
     */
    public void stop() {
        status = Status.STOPPED;
        if (process.isAlive()) {
            try {
                sendInput('q');
                // Wait briefly for graceful shutdown
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    /**
     * Returns the detected HTTP port, or -1 if not yet detected.
     */
    public int getHttpPort() {
        return httpPort;
    }
}
