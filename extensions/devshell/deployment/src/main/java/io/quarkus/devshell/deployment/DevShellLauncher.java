package io.quarkus.devshell.deployment;

import java.lang.reflect.Method;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Size;
import org.jboss.logging.Logger;

import io.quarkus.deployment.console.AeshConsole;
import io.quarkus.deployment.console.DelegateConnection;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.devshell.runtime.DevShellRecorder;

/**
 * Launches the Dev Shell TUI with proper terminal handling.
 * Uses the Aesh Connection for raw keyboard input.
 * Communication with the runtime TUI happens via JDK BlockingQueues.
 */
public class DevShellLauncher {

    private static final Logger LOG = Logger.getLogger(DevShellLauncher.class);

    private static final String INPUT_QUEUE_KEY = "devshell-input-queue";
    private static final String OUTPUT_QUEUE_KEY = "devshell-output-queue";
    private static final String SIZE_REF_KEY = "devshell-size-ref";
    private static final String RUNNING_FLAG_KEY = "devshell-running-flag";

    private static volatile DelegateConnection delegateConnection;
    private static volatile boolean running = false;
    private static volatile Thread outputThread;

    /**
     * Launch the Dev Shell TUI.
     * This method is called from the console command when 't' is pressed.
     */
    public static void launch() {
        if (running) {
            LOG.info("Dev Shell is already running");
            return;
        }

        // Check if we have an AeshConsole
        if (!(QuarkusConsole.INSTANCE instanceof AeshConsole)) {
            LOG.warn("Dev Shell requires an ANSI-capable terminal");
            return;
        }

        AeshConsole aeshConsole = (AeshConsole) QuarkusConsole.INSTANCE;
        Connection connection = aeshConsole.getConnection();

        if (connection == null) {
            LOG.error("Failed to start Dev Shell: No terminal connection available");
            return;
        }

        running = true;

        // Create communication queues (JDK types work across classloaders)
        LinkedBlockingQueue<int[]> inputQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();
        AtomicReference<int[]> sizeRef = new AtomicReference<>();
        AtomicBoolean runningFlag = new AtomicBoolean(true);

        // Store in DevConsoleManager for runtime to access
        DevConsoleManager.setGlobal(INPUT_QUEUE_KEY, inputQueue);
        DevConsoleManager.setGlobal(OUTPUT_QUEUE_KEY, outputQueue);
        DevConsoleManager.setGlobal(SIZE_REF_KEY, sizeRef);
        DevConsoleManager.setGlobal(RUNNING_FLAG_KEY, runningFlag);

        // Get terminal size
        Size size = connection.size();
        if (size != null) {
            sizeRef.set(new int[] { size.getWidth(), size.getHeight() });
        } else {
            sizeRef.set(new int[] { 80, 24 });
        }

        // Create delegate connection to capture input
        delegateConnection = new DelegateConnection(connection);

        // Set up stdin handler to feed keys to the input queue
        delegateConnection.setStdinHandler(keys -> {
            if (keys != null && keys.length > 0) {
                inputQueue.offer(keys);
            }
        });

        // Tell AeshConsole we're taking over input
        // Pass true to allow ESC key to reach the TUI (for navigation)
        aeshConsole.setDelegateConnection(delegateConnection, true);

        // Start output handler thread to write to the connection
        outputThread = new Thread(() -> {
            try {
                while (runningFlag.get() || !outputQueue.isEmpty()) {
                    String output = outputQueue.poll(50, TimeUnit.MILLISECONDS);
                    if (output != null) {
                        connection.write(output);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Dev Shell Output");
        outputThread.setDaemon(true);
        outputThread.start();

        // Load DevShellLauncherRuntime from the runtime classloader (not the deployment CL)
        // so that all TUI classes are loaded by the runtime CL where Arc CDI is available.
        ClassLoader runtimeCL = DevConsoleManager.getGlobal(DevShellRecorder.RUNTIME_CL_KEY);
        if (runtimeCL == null) {
            LOG.error("Failed to start Dev Shell: Runtime classloader not available");
            exitShellMode(aeshConsole, connection, inputQueue, outputQueue, runningFlag);
            return;
        }

        try {
            Class<?> launcherRuntimeClass = runtimeCL
                    .loadClass("io.quarkus.devshell.runtime.DevShellLauncherRuntime");
            Method registerMethod = launcherRuntimeClass.getMethod("registerLauncher");
            registerMethod.invoke(null);
        } catch (Exception e) {
            LOG.error("Failed to register Dev Shell launcher: " + e.getMessage());
            exitShellMode(aeshConsole, connection, inputQueue, outputQueue, runningFlag);
            return;
        }

        // Start the TUI in a new thread with the runtime classloader as TCCL
        // so that all classes loaded by the TUI come from the runtime CL
        Thread tuiThread = new Thread(() -> {
            Thread.currentThread().setContextClassLoader(runtimeCL);
            try {
                DevShellRecorder.triggerLaunch();
            } finally {
                exitShellMode(aeshConsole, connection, inputQueue, outputQueue, runningFlag);
            }
        }, "Dev Shell TUI");
        tuiThread.setDaemon(true);
        tuiThread.start();
    }

    /**
     * Exit shell mode and restore normal console.
     */
    private static synchronized void exitShellMode(AeshConsole aeshConsole, Connection connection,
            LinkedBlockingQueue<int[]> inputQueue, LinkedBlockingQueue<String> outputQueue,
            AtomicBoolean runningFlag) {
        if (!running) {
            return;
        }

        running = false;
        runningFlag.set(false);

        // Wait for the output thread to finish draining the queue
        if (outputThread != null) {
            try {
                outputThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Drain any remaining output that may not have been flushed
        String output;
        while ((output = outputQueue.poll()) != null) {
            connection.write(output);
        }

        // Close and clear the delegate connection
        if (delegateConnection != null) {
            delegateConnection.close();
            delegateConnection = null;
        }

        // Tell AeshConsole to resume normal input handling
        aeshConsole.clearDelegateConnection();

        // Restore raw mode
        connection.enterRawMode();

        // Note: We don't need to clean up DevConsoleManager entries
        // They will be overwritten when the TUI launches again
        // ConcurrentHashMap doesn't allow null values anyway
    }
}
