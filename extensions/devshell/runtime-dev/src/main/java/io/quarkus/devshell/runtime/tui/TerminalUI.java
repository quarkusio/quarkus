package io.quarkus.devshell.runtime.tui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.DevShellRouter;
import io.quarkus.devshell.runtime.ReflectiveBuildTimeDataReader;
import io.quarkus.devshell.runtime.tui.widgets.LogPanel;

/**
 * Main TUI controller that manages screens, rendering, and input handling.
 * Uses TamboUI's Buffer/Frame system for rendering.
 */
public class TerminalUI {

    private final DevShellRouter router;
    private final ReflectiveBuildTimeDataReader buildTimeDataReader;
    private final List<ShellExtension> extensions;
    private final Map<String, ShellPageInfo> shellPages;
    private final BlockingQueue<int[]> inputQueue;
    private final BlockingQueue<String> outputQueue;
    private final AtomicReference<int[]> sizeRef;

    private final Deque<Screen> screenStack = new ArrayDeque<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean redrawRequested = new AtomicBoolean(true);

    private AppContext context;
    private volatile String statusMessage = "";
    private int width;
    private int height;

    // Log panel
    private final LogPanel logPanel = new LogPanel();
    private boolean showLogPanel = false;
    private static final int LOG_PANEL_HEIGHT = 8; // Header + 7 log lines

    public TerminalUI(DevShellRouter router, ReflectiveBuildTimeDataReader buildTimeDataReader,
            List<ShellExtension> extensions, Map<String, ShellPageInfo> shellPages,
            BlockingQueue<int[]> inputQueue, BlockingQueue<String> outputQueue,
            AtomicReference<int[]> sizeRef, int width, int height) {
        this.router = router;
        this.buildTimeDataReader = buildTimeDataReader;
        this.extensions = extensions != null ? extensions : List.of();
        this.shellPages = shellPages != null ? shellPages : Map.of();
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.sizeRef = sizeRef;
        this.width = width;
        this.height = height;
    }

    public ReflectiveBuildTimeDataReader getBuildTimeDataReader() {
        return buildTimeDataReader;
    }

    /**
     * Start the TUI with the given initial screen.
     * This method blocks until the TUI is stopped.
     */
    public void start(Screen initialScreen) {
        running.set(true);

        // Enter alternate screen buffer and hide cursor
        write(AnsiRenderer.ENTER_ALTERNATE_SCREEN);
        write(AnsiRenderer.HIDE_CURSOR);
        write(AnsiRenderer.clearScreen());

        // Initialize context
        context = new AppContext(this, router, width, height);

        // Push initial screen
        navigateTo(initialScreen);

        // Main loop - render and handle input
        try {
            while (running.get()) {
                // Check for size changes
                updateSize();

                // Render if needed
                if (redrawRequested.getAndSet(false)) {
                    render();
                }

                // Wait for input with timeout
                int[] keys = null;
                try {
                    keys = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (keys != null && keys.length > 0) {
                    // Check for close signal
                    if (keys.length == 1 && keys[0] == -1) {
                        break;
                    }

                    int key = KeyCode.parse(keys);
                    handleKey(key);
                }
            }
        } finally {
            // Cleanup: show cursor and exit alternate screen
            write(AnsiRenderer.SHOW_CURSOR);
            write(AnsiRenderer.EXIT_ALTERNATE_SCREEN);
        }
    }

    private void updateSize() {
        if (sizeRef != null) {
            int[] size = sizeRef.get();
            if (size != null && size.length >= 2) {
                int newWidth = size[0];
                int newHeight = size[1];
                if (newWidth != width || newHeight != height) {
                    width = newWidth;
                    height = newHeight;
                    // Account for log panel if visible
                    int effectiveHeight = showLogPanel ? height - LOG_PANEL_HEIGHT : height;
                    context.setSize(width, effectiveHeight);
                    Screen current = screenStack.peek();
                    if (current != null) {
                        current.onResize(width, effectiveHeight);
                    }
                    requestRedraw();
                }
            }
        }
    }

    private void handleKey(int key) {
        Screen currentScreen = screenStack.peek();
        if (currentScreen == null) {
            return;
        }

        // If log panel is visible, let it handle certain keys first
        if (showLogPanel && logPanel.handleKey(key)) {
            requestRedraw();
            return;
        }

        // Let the current screen handle the key first
        boolean handled = currentScreen.handleKey(key);

        if (!handled) {
            // Global key handling
            switch (key) {
                case 'q':
                case 'Q':
                    if (screenStack.size() <= 1) {
                        stop();
                    } else {
                        goBack();
                    }
                    break;
                case KeyCode.ESCAPE:
                    if (screenStack.size() <= 1) {
                        stop();
                    } else {
                        goBack();
                    }
                    break;
                case 'l':
                case 'L':
                    toggleLogPanel();
                    break;
            }
        }
    }

    /**
     * Toggle the log panel visibility.
     */
    private void toggleLogPanel() {
        showLogPanel = !showLogPanel;
        if (showLogPanel) {
            // Start live streaming logs
            logPanel.startStreaming(router, this::requestRedraw);
            // Adjust context height to account for log panel
            context.setSize(width, height - LOG_PANEL_HEIGHT);
        } else {
            // Stop streaming when hiding
            logPanel.stopStreaming(router);
            // Restore full height
            context.setSize(width, height);
        }
        // Notify current screen of size change
        Screen current = screenStack.peek();
        if (current != null) {
            current.onResize(width, showLogPanel ? height - LOG_PANEL_HEIGHT : height);
        }
        requestRedraw();
    }

    private void render() {
        Screen currentScreen = screenStack.peek();
        if (currentScreen == null) {
            return;
        }

        // Refresh log panel if visible and not streaming (fallback polling mode)
        if (showLogPanel && !logPanel.isStreaming()) {
            logPanel.refreshIfNeeded(router);
        }

        // Create a buffer for the full terminal
        Rect fullArea = new Rect(0, 0, width, height);
        Buffer buffer = Buffer.empty(fullArea);

        // Determine the area for the screen (exclude log panel and status bar)
        int effectiveHeight = showLogPanel ? height - LOG_PANEL_HEIGHT : height;

        // Create a frame for the screen to render into
        Frame frame = Frame.forTesting(buffer);

        // Let the screen render into the buffer
        currentScreen.render(frame);

        // Render log panel if visible (above status bar)
        if (showLogPanel) {
            int logPanelStartRow = height - LOG_PANEL_HEIGHT;
            logPanel.setVisibleRows(LOG_PANEL_HEIGHT - 1); // -1 for header
            logPanel.render(buffer, logPanelStartRow, width);
        }

        // Render status bar at bottom
        renderStatusBar(buffer);

        // Convert buffer to output and send
        write("\033[H"); // Move cursor to home (1,1)
        write(buffer.toAnsiString());
    }

    private void renderStatusBar(Buffer buffer) {
        int row = height - 1; // 0-based

        // Build status bar content
        String logHint;
        if (showLogPanel) {
            String tabInfo = logPanel.getCurrentTab().getLabel();
            String streamInfo = logPanel.isStreaming() ? " live" : "";
            logHint = "[L] " + tabInfo + streamInfo;
        } else {
            logHint = "[L] Logs";
        }
        String hints = " [Up/Down] Navigate  [Enter] Select  [Esc] Back  " + logHint + "  [Q] Quit ";

        Style barStyle = Style.create().reversed();

        // Write the hints
        buffer.setString(0, row, hints, barStyle);

        // Fill remaining width
        int remaining = width - hints.length();
        if (remaining > 0) {
            if (statusMessage != null && !statusMessage.isEmpty()) {
                String msg = " " + statusMessage + " ";
                if (msg.length() > remaining) {
                    msg = msg.substring(0, remaining);
                }
                int spacesLen = remaining - msg.length();
                if (spacesLen > 0) {
                    buffer.setString(hints.length(), row, " ".repeat(spacesLen), barStyle);
                }
                buffer.setString(hints.length() + spacesLen, row, msg, barStyle);
            } else {
                buffer.setString(hints.length(), row, " ".repeat(remaining), barStyle);
            }
        }
    }

    /**
     * Navigate to a new screen, pushing it onto the stack.
     */
    public void navigateTo(Screen screen) {
        Screen current = screenStack.peek();
        if (current != null) {
            current.onLeave();
        }

        screenStack.push(screen);
        // Update context size for log panel if visible
        int effectiveHeight = showLogPanel ? height - LOG_PANEL_HEIGHT : height;
        context.setSize(width, effectiveHeight);
        screen.onEnter(context);
        requestRedraw();
    }

    /**
     * Go back to the previous screen.
     *
     * @return true if went back, false if already at root
     */
    public boolean goBack() {
        if (screenStack.size() <= 1) {
            return false;
        }

        Screen current = screenStack.pop();
        current.onLeave();

        Screen previous = screenStack.peek();
        if (previous != null) {
            previous.onEnter(context);
        }

        requestRedraw();
        return true;
    }

    /**
     * Stop the TUI and return to normal console.
     */
    public void stop() {
        // Stop log streaming if active
        if (logPanel.isStreaming()) {
            logPanel.stopStreaming(router);
        }
        running.set(false);
    }

    /**
     * Request a screen redraw.
     */
    public void requestRedraw() {
        redrawRequested.set(true);
    }

    /**
     * Write text to the terminal via the output queue.
     */
    public void write(String text) {
        if (text != null && outputQueue != null) {
            outputQueue.offer(text);
        }
    }

    /**
     * Set the status bar message.
     */
    public void setStatus(String message) {
        this.statusMessage = message;
        requestRedraw();
    }

    /**
     * Get the list of extensions.
     */
    public List<ShellExtension> getExtensions() {
        return extensions;
    }

    /**
     * Get the shell page info map.
     */
    public Map<String, ShellPageInfo> getShellPages() {
        return shellPages;
    }

    /**
     * Check if the TUI is running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Get the current width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the current height.
     */
    public int getHeight() {
        return height;
    }
}
