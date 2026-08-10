package io.quarkus.devshell.deployment.tui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.paragraph.Paragraph;
import io.quarkus.devshell.deployment.DevShellJsonRpcClient;
import io.quarkus.devshell.deployment.tui.widgets.LogPanel;

/**
 * Main TUI controller. Manages the screen stack, rendering loop, and input handling.
 * Uses TamboUI for rendering to an in-memory Buffer, then writes the buffer's
 * ANSI output via the {@link TerminalBackend}.
 */
public class TerminalUI {

    private static final Logger log = Logger.getLogger(TerminalUI.class);

    private final TerminalBackend backend;
    private final DevShellJsonRpcClient jsonRpcClient;
    private final Deque<Screen> screenStack = new ArrayDeque<>();
    private final LinkedBlockingQueue<int[]> inputQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean redrawRequested = new AtomicBoolean(true);
    private final AppContext appContext;
    private volatile int width;
    private volatile int height;
    private Buffer previousBuffer;
    private LogPanel logPanel;
    private boolean logPanelVisible = false;
    private static final int LOG_PANEL_HEIGHT = 8;

    public TerminalUI(TerminalBackend backend, DevShellJsonRpcClient jsonRpcClient) {
        this.backend = backend;
        this.jsonRpcClient = jsonRpcClient;
        this.width = backend.getWidth();
        this.height = backend.getHeight();
        this.appContext = new AppContext(jsonRpcClient, this);
    }

    public void start(Screen initialScreen) {
        running.set(true);

        backend.setInputHandler(keys -> {
            if (keys != null) {
                inputQueue.offer(keys);
            }
        });

        backend.setResizeHandler(size -> {
            if (size != null && size.length == 2) {
                this.width = size[0];
                this.height = size[1];
                Screen current = screenStack.peek();
                if (current != null) {
                    int effectiveHeight = logPanelVisible ? height - LOG_PANEL_HEIGHT - 1 : height - 1;
                    current.onResize(width, effectiveHeight);
                }
                previousBuffer = null;
                requestRedraw();
            }
        });

        navigateTo(initialScreen);
        backend.hideCursor();

        while (running.get()) {
            try {
                int[] keys = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                if (keys != null) {
                    handleInput(keys);
                }

                Screen current = screenStack.peek();
                if (current != null) {
                    current.tick();
                }

                if (redrawRequested.compareAndSet(true, false)) {
                    render();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in TUI loop", e);
            }
        }

        backend.showCursor();
        backend.write("\033[0m\033[2J\033[H");
    }

    private void handleInput(int[] keys) {
        if (keys.length == 1 && keys[0] == -1) {
            exit();
            return;
        }

        // Let log panel handle tab switching keys before screens
        if (logPanelVisible && logPanel != null) {
            int parsedKey = KeyCode.parse(keys);
            if (logPanel.handleKey(parsedKey)) {
                requestRedraw();
                return;
            }
        }

        Screen current = screenStack.peek();
        if (current != null && current.handleKey(keys)) {
            requestRedraw();
            return;
        }

        if (keys.length == 1) {
            int k = keys[0];
            if (k == 'l' || k == 'L') {
                toggleLogPanel();
            } else if (k == 27) { // ESC
                goBack();
            } else if (k == 'q' || k == 'Q') {
                if (screenStack.size() <= 1) {
                    exit();
                } else {
                    goBack();
                }
            }
        }
    }

    private void render() {
        if (width <= 0 || height <= 0) {
            return;
        }
        Screen current = screenStack.peek();
        if (current == null) {
            return;
        }

        Rect fullArea = new Rect(0, 0, width, height);
        Buffer buffer = Buffer.empty(fullArea);

        if (logPanelVisible && logPanel != null) {
            var areas = Layout.vertical()
                    .constraints(Constraint.fill(), Constraint.length(LOG_PANEL_HEIGHT), Constraint.length(1))
                    .split(fullArea);
            current.render(areas.get(0), buffer);
            logPanel.render(areas.get(1), buffer);
            renderStatusBar(areas.get(2), buffer, current);
        } else {
            var areas = Layout.vertical()
                    .constraints(Constraint.fill(), Constraint.length(1))
                    .split(fullArea);
            current.render(areas.get(0), buffer);
            renderStatusBar(areas.get(1), buffer, current);
        }

        String output;
        if (previousBuffer == null) {
            output = "\033[2J" + AnsiWriter.fullRender(buffer);
        } else {
            output = AnsiWriter.diffRender(buffer, previousBuffer);
        }
        if (!output.isEmpty()) {
            backend.write(output);
        }
        previousBuffer = buffer;
    }

    private void renderStatusBar(Rect area, Buffer buffer, Screen current) {
        Style statusStyle = Style.EMPTY.onBlue().white();
        Style boldStatus = statusStyle.bold();

        buffer.fill(area, new Cell(" ", statusStyle));

        Span logSpan;
        if (logPanelVisible && logPanel != null) {
            String tabName = logPanel.getActiveTab() == 0 ? "Server" : "Testing";
            String streamInfo = logPanel.isStreaming() ? " live" : "";
            logSpan = Span.styled("  [L] " + tabName + streamInfo, statusStyle);
        } else {
            logSpan = Span.styled("  [L] Logs", statusStyle);
        }

        Line statusLine = Line.from(
                Span.styled(" Dev Shell", boldStatus),
                Span.styled(" | ", statusStyle),
                Span.styled(current.getTitle(), statusStyle),
                Span.styled(" | ", statusStyle),
                Span.styled("ESC: back  q: quit", statusStyle),
                logSpan);

        Paragraph statusParagraph = Paragraph.builder()
                .text(Text.from(statusLine))
                .style(statusStyle)
                .build();
        statusParagraph.render(area, buffer);
    }

    void navigateTo(Screen screen) {
        screen.onEnter(appContext);
        screenStack.push(screen);
        previousBuffer = null;
        requestRedraw();
    }

    void goBack() {
        if (screenStack.size() <= 1) {
            exit();
            return;
        }
        Screen removed = screenStack.pop();
        if (removed != null) {
            removed.onLeave();
        }
        previousBuffer = null;
        requestRedraw();
    }

    void exit() {
        running.set(false);
        if (logPanel != null) {
            logPanel.stop();
        }
        while (!screenStack.isEmpty()) {
            screenStack.pop().onLeave();
        }
    }

    private void toggleLogPanel() {
        logPanelVisible = !logPanelVisible;
        if (logPanelVisible) {
            if (logPanel == null) {
                logPanel = new LogPanel();
                logPanel.start(appContext);
            }
        }
        Screen current = screenStack.peek();
        if (current != null) {
            int effectiveHeight = logPanelVisible ? height - LOG_PANEL_HEIGHT - 1 : height - 1;
            current.onResize(width, effectiveHeight);
        }
        requestRedraw();
    }

    void requestRedraw() {
        redrawRequested.set(true);
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }
}
