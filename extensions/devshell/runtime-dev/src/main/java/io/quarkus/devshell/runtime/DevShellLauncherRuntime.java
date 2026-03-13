package io.quarkus.devshell.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.ShellPageInfo;
import io.quarkus.devshell.runtime.tui.TerminalUI;
import io.quarkus.devshell.runtime.tui.screens.MainMenuScreen;

/**
 * Runtime launcher for the Dev Shell TUI.
 * This class is in runtime-dev because it depends on TUI classes.
 * It registers itself as the launcher consumer that DevShellRecorder triggers.
 */
public class DevShellLauncherRuntime {

    private static final Logger LOG = Logger.getLogger(DevShellLauncherRuntime.class);

    private static final String INPUT_QUEUE_KEY = "devshell-input-queue";
    private static final String OUTPUT_QUEUE_KEY = "devshell-output-queue";
    private static final String SIZE_REF_KEY = "devshell-size-ref";
    private static final String RUNNING_FLAG_KEY = "devshell-running-flag";

    private static final AtomicReference<TerminalUI> CURRENT_TUI = new AtomicReference<>();

    /**
     * Register the launcher consumer. Called from DevShellLauncher in deployment.
     */
    public static void registerLauncher() {
        Consumer<Object> launcher = (signal) -> launchTUI();
        DevConsoleManager.setGlobal(DevShellRecorder.LAUNCHER_KEY, launcher);
    }

    /**
     * Internal method that launches the interactive TUI.
     */
    @SuppressWarnings("unchecked")
    private static void launchTUI() {
        // Check if TUI is already running
        TerminalUI currentTui = CURRENT_TUI.get();
        if (currentTui != null && currentTui.isRunning()) {
            System.out.println("Dev Shell is already running");
            return;
        }

        try {
            // Get the router from DevConsoleManager
            // Note: The router may be from a different classloader, so we wrap it
            Object routerObject = DevConsoleManager.getGlobal(DevShellRecorder.ROUTER_KEY);
            if (routerObject == null) {
                System.err.println("Failed to start Dev Shell: Router is not initialized");
                return;
            }

            // Create a router wrapper that uses reflection to call methods
            DevShellRouter router = new ReflectiveRouterWrapper(routerObject);

            // Get the BuildTimeDataReader from DevConsoleManager
            Object buildTimeDataReaderObject = DevConsoleManager.getGlobal(DevShellRecorder.BUILD_TIME_DATA_READER_KEY);
            ReflectiveBuildTimeDataReader buildTimeDataReader = null;
            if (buildTimeDataReaderObject != null) {
                buildTimeDataReader = new ReflectiveBuildTimeDataReader(buildTimeDataReaderObject);
            }

            // Get the communication queues from DevConsoleManager
            BlockingQueue<int[]> inputQueue = DevConsoleManager.getGlobal(INPUT_QUEUE_KEY);
            BlockingQueue<String> outputQueue = DevConsoleManager.getGlobal(OUTPUT_QUEUE_KEY);
            AtomicReference<int[]> sizeRef = DevConsoleManager.getGlobal(SIZE_REF_KEY);
            AtomicBoolean runningFlag = DevConsoleManager.getGlobal(RUNNING_FLAG_KEY);

            if (inputQueue == null || outputQueue == null) {
                System.err.println("Failed to start Dev Shell: Communication queues not set up");
                return;
            }

            // Get extensions from DevConsoleManager and convert back to ShellExtension
            List<ShellExtension> extensions = new ArrayList<>();
            List<String[]> extensionData = DevConsoleManager.getGlobal(DevShellRecorder.EXTENSIONS_KEY);
            if (extensionData != null) {
                for (String[] data : extensionData) {
                    extensions.add(ShellExtension.of(
                            data[0], // namespace
                            data[1], // name
                            data[2], // description
                            data[3], // status
                            "true".equals(data[4]) // isActive
                    ));
                }
            }

            // Get shell pages from DevConsoleManager and convert back to ShellPageInfo
            Map<String, ShellPageInfo> shellPages = new HashMap<>();
            Map<String, String[]> pageData = DevConsoleManager.getGlobal(DevShellRecorder.SHELL_PAGES_KEY);
            LOG.debugf("launchTUI: Retrieved pageData from DevConsoleManager, count=%d",
                    pageData != null ? pageData.size() : 0);
            if (pageData != null) {
                for (Map.Entry<String, String[]> entry : pageData.entrySet()) {
                    String[] data = entry.getValue();
                    String customPageClass = data.length > 5 && !data[5].isEmpty() ? data[5] : null;
                    LOG.debugf("  Loading shell page: id=%s, title=%s, customPageClass=%s",
                            data[0], data[1], customPageClass != null ? customPageClass : "(none)");
                    shellPages.put(entry.getKey(), new ShellPageInfo(
                            data[0], // id
                            data[1], // title
                            data[2].isEmpty() ? '\0' : data[2].charAt(0), // shortcutKey
                            data[3].isEmpty() ? null : data[3], // jsonRpcNamespace
                            data[4].isEmpty() ? null : data[4], // providerClassName
                            customPageClass // customPageClassName
                    ));
                }
            }
            LOG.debugf("launchTUI: Loaded %d shell pages for TUI", shellPages.size());

            // Get initial size
            int width = 80;
            int height = 24;
            if (sizeRef != null) {
                int[] size = sizeRef.get();
                if (size != null && size.length >= 2) {
                    width = size[0];
                    height = size[1];
                }
            }

            // Create and start the TUI with queue-based I/O
            TerminalUI tui = new TerminalUI(router, buildTimeDataReader, extensions, shellPages, inputQueue, outputQueue,
                    sizeRef, width, height);
            CURRENT_TUI.set(tui);

            // Mark as running
            if (runningFlag != null) {
                runningFlag.set(true);
            }

            // Start with the main menu screen
            tui.start(new MainMenuScreen());

        } catch (Exception e) {
            System.err.println("Failed to start Dev Shell: " + e.getMessage());
            e.printStackTrace();
        } finally {
            CURRENT_TUI.set(null);
            // Signal that we're done
            AtomicBoolean runningFlag = DevConsoleManager.getGlobal(RUNNING_FLAG_KEY);
            if (runningFlag != null) {
                runningFlag.set(false);
            }
        }
    }

    /**
     * Notify the running TUI of a hot reload.
     */
    public static void notifyHotReload() {
        TerminalUI tui = CURRENT_TUI.get();
        if (tui != null && tui.isRunning()) {
            tui.setStatus("Application reloaded");
            tui.requestRedraw();
        }
    }

    /**
     * Stop the running TUI.
     */
    public static void stopTui() {
        TerminalUI tui = CURRENT_TUI.get();
        if (tui != null) {
            tui.stop();
        }
    }
}
