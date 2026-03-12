package io.quarkus.devshell.runtime.tui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import io.quarkus.devshell.runtime.DevShellRouter;
import io.quarkus.devshell.runtime.tui.JsonHelper;

/**
 * A log panel widget that displays recent application logs.
 * Supports both polling mode (history) and live streaming mode.
 * Includes multiple tabs: Server (all logs) and Testing (test runner logs).
 */
public class LogPanel {

    /**
     * Log view tabs available in the panel.
     */
    public enum LogTab {
        SERVER("Server", null),
        TESTING("Testing", "Test runner thread");

        private final String label;
        private final String threadFilter;

        LogTab(String label, String threadFilter) {
            this.label = label;
            this.threadFilter = threadFilter;
        }

        public String getLabel() {
            return label;
        }

        public String getThreadFilter() {
            return threadFilter;
        }
    }

    private final List<LogEntry> logEntries = new ArrayList<>();
    private int maxEntries = 100;
    private int visibleRows = 5;
    private int scrollOffset = 0;
    private boolean autoScroll = true;
    private long lastFetchTime = 0;
    private static final long FETCH_INTERVAL_MS = 2000; // Refresh every 2 seconds

    // Tab support
    private LogTab currentTab = LogTab.SERVER;

    // Streaming support
    private volatile int streamSubscriptionId = -1;
    private volatile boolean streaming = false;
    private Runnable onUpdateCallback;

    public void setVisibleRows(int rows) {
        this.visibleRows = Math.max(1, rows);
    }

    public void setMaxEntries(int max) {
        this.maxEntries = max;
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    /**
     * Get the current log tab.
     */
    public LogTab getCurrentTab() {
        return currentTab;
    }

    /**
     * Set the current log tab.
     */
    public void setCurrentTab(LogTab tab) {
        if (tab != currentTab) {
            this.currentTab = tab;
            // Reset scroll when switching tabs
            this.scrollOffset = 0;
            this.autoScroll = true;
        }
    }

    /**
     * Switch to the next tab.
     */
    public void nextTab() {
        LogTab[] tabs = LogTab.values();
        int currentIndex = currentTab.ordinal();
        int nextIndex = (currentIndex + 1) % tabs.length;
        setCurrentTab(tabs[nextIndex]);
    }

    /**
     * Switch to the previous tab.
     */
    public void previousTab() {
        LogTab[] tabs = LogTab.values();
        int currentIndex = currentTab.ordinal();
        int prevIndex = (currentIndex - 1 + tabs.length) % tabs.length;
        setCurrentTab(tabs[prevIndex]);
    }

    /**
     * Get filtered log entries based on current tab.
     */
    private List<LogEntry> getFilteredEntries() {
        if (currentTab.getThreadFilter() == null) {
            return logEntries;
        }

        List<LogEntry> filtered = new ArrayList<>();
        String filter = currentTab.getThreadFilter();
        synchronized (logEntries) {
            for (LogEntry entry : logEntries) {
                if (entry.threadName != null && entry.threadName.contains(filter)) {
                    filtered.add(entry);
                }
            }
        }
        return filtered;
    }

    /**
     * Fetch log history from the server if enough time has passed.
     */
    public void refreshIfNeeded(DevShellRouter router) {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime < FETCH_INTERVAL_MS) {
            return;
        }
        lastFetchTime = now;

        router.call("devui-logstream_history", Map.of())
                .thenAccept(this::parseLogHistory)
                .exceptionally(ex -> {
                    // Silently ignore errors
                    return null;
                });
    }

    /**
     * Force refresh logs.
     */
    public void refresh(DevShellRouter router) {
        lastFetchTime = 0;
        refreshIfNeeded(router);
    }

    /**
     * Start live streaming of logs.
     *
     * @param router the router to use for the subscription
     * @param onUpdate callback to invoke when new logs arrive (typically to trigger a redraw)
     */
    public void startStreaming(DevShellRouter router, Runnable onUpdate) {
        if (streaming) {
            return; // Already streaming
        }

        this.onUpdateCallback = onUpdate;
        this.streaming = true;

        // First, load history to have some initial logs
        refresh(router);

        // Then subscribe to the stream
        streamSubscriptionId = router.subscribe(
                "devui-logstream_streamLog",
                Map.of(),
                this::handleStreamMessage,
                error -> {
                    // On error, fall back to polling mode
                    streaming = false;
                    streamSubscriptionId = -1;
                },
                () -> {
                    // On complete, mark as not streaming (fall back to polling)
                    streaming = false;
                    streamSubscriptionId = -1;
                });

        // If subscribe returned -1, streaming failed - stay in polling mode
        if (streamSubscriptionId < 0) {
            streaming = false;
        }
    }

    /**
     * Stop live streaming of logs.
     *
     * @param router the router to use for unsubscribing
     */
    public void stopStreaming(DevShellRouter router) {
        if (!streaming || streamSubscriptionId < 0) {
            return;
        }

        streaming = false;
        router.unsubscribe(streamSubscriptionId);
        streamSubscriptionId = -1;
        onUpdateCallback = null;
    }

    /**
     * Check if currently streaming logs.
     */
    public boolean isStreaming() {
        return streaming;
    }

    /**
     * Handle incoming stream message.
     */
    private void handleStreamMessage(String jsonMessage) {
        try {
            // The stream sends individual log entry objects wrapped in JSON-RPC response
            String entryJson = extractStreamResult(jsonMessage);
            if (entryJson != null && !entryJson.isEmpty()) {
                LogEntry entry = parseLogEntry(entryJson);
                if (entry != null) {
                    synchronized (logEntries) {
                        logEntries.add(entry);

                        // Trim to max
                        while (logEntries.size() > maxEntries) {
                            logEntries.remove(0);
                        }
                    }

                    // Auto-scroll to bottom (use filtered entries for correct offset)
                    if (autoScroll) {
                        List<LogEntry> filtered = getFilteredEntries();
                        scrollOffset = Math.max(0, filtered.size() - visibleRows);
                    }

                    // Notify that we have new data
                    if (onUpdateCallback != null) {
                        onUpdateCallback.run();
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore parsing errors
        }
    }

    /**
     * Extract result from streaming JSON-RPC response.
     * Stream messages have format: {"id":X,"result":{...log entry...}}
     */
    private String extractStreamResult(String jsonMessage) {
        if (jsonMessage == null || jsonMessage.isEmpty()) {
            return null;
        }

        String result = JsonHelper.extractResult(jsonMessage);
        if (result != null) {
            return result;
        }

        // Maybe it's just the object directly
        if (jsonMessage.trim().startsWith("{") && jsonMessage.contains("\"message\"")) {
            return jsonMessage;
        }
        return null;
    }

    private void parseLogHistory(String jsonResponse) {
        try {
            String resultJson = JsonHelper.extractResult(jsonResponse);
            if (resultJson == null || resultJson.isEmpty()) {
                // Maybe it's just the array directly
                if (jsonResponse != null && jsonResponse.trim().startsWith("[")) {
                    resultJson = jsonResponse;
                } else {
                    return;
                }
            }

            List<LogEntry> newEntries = new ArrayList<>();

            // Parse array of log entries
            int idx = 0;
            while ((idx = resultJson.indexOf("{", idx)) != -1) {
                int endIdx = JsonHelper.findMatchingBrace(resultJson, idx);
                if (endIdx == -1) {
                    break;
                }

                String entryJson = resultJson.substring(idx, endIdx + 1);
                LogEntry entry = parseLogEntry(entryJson);
                if (entry != null) {
                    newEntries.add(entry);
                }

                idx = endIdx + 1;
            }

            // Update entries
            synchronized (logEntries) {
                logEntries.clear();
                logEntries.addAll(newEntries);

                // Trim to max
                while (logEntries.size() > maxEntries) {
                    logEntries.remove(0);
                }

                // Auto-scroll to bottom
                if (autoScroll) {
                    scrollOffset = Math.max(0, logEntries.size() - visibleRows);
                }
            }
        } catch (Exception e) {
            // Silently ignore parsing errors
        }
    }

    private LogEntry parseLogEntry(String json) {
        LogEntry entry = new LogEntry();
        entry.timestamp = JsonHelper.getString(json, "timestamp");
        entry.level = JsonHelper.getString(json, "level");
        entry.loggerName = JsonHelper.getString(json, "loggerName");
        entry.threadName = JsonHelper.getString(json, "threadName");
        entry.message = JsonHelper.getString(json, "formattedMessage");

        if (entry.message == null) {
            entry.message = JsonHelper.getString(json, "message");
        }

        if (entry.message != null) {
            return entry;
        }
        return null;
    }

    /**
     * Render the log panel into the buffer.
     *
     * @param buffer the buffer to render into
     * @param startRow starting row (0-based)
     * @param width the available width
     */
    public void render(Buffer buffer, int startRow, int width) {
        Style headerBgStyle = Style.create().white().onBlack();
        Style tabActiveStyle = Style.create().bold().white().onBlack();
        Style tabInactiveStyle = Style.create().gray().onBlack();
        Style hintStyle = Style.create().gray().onBlack();

        // Header line with tabs
        StringBuilder header = new StringBuilder(" ");
        for (LogTab tab : LogTab.values()) {
            if (tab == currentTab) {
                header.append("[").append(tab.getLabel()).append("]");
            } else {
                header.append(" ").append(tab.getLabel()).append(" ");
            }
            header.append(" ");
        }

        // Get filtered entries and show count
        List<LogEntry> filteredEntries = getFilteredEntries();
        header.append("(").append(filteredEntries.size()).append(")");
        header.append("  [1/2] switch");

        // Pad header to width
        String headerStr = header.toString();
        if (headerStr.length() < width) {
            headerStr = headerStr + " ".repeat(width - headerStr.length());
        }
        buffer.setString(0, startRow, headerStr, headerBgStyle);

        // Log lines (using filtered entries)
        int endIdx = Math.min(scrollOffset + visibleRows, filteredEntries.size());
        for (int i = scrollOffset; i < endIdx; i++) {
            int row = startRow + 1 + (i - scrollOffset);
            LogEntry entry = filteredEntries.get(i);
            renderLogEntry(buffer, row, entry, width);
        }

        // Fill remaining rows if needed
        for (int i = endIdx - scrollOffset; i < visibleRows; i++) {
            int row = startRow + 1 + i;
            buffer.setString(0, row, " ".repeat(width), Style.EMPTY);
        }
    }

    private void renderLogEntry(Buffer buffer, int row, LogEntry entry, int width) {
        int col = 0;

        // Timestamp (shortened)
        if (entry.timestamp != null && entry.timestamp.length() > 12) {
            String time = entry.timestamp;
            if (time.contains("T")) {
                time = time.substring(time.indexOf("T") + 1);
            }
            if (time.length() > 12) {
                time = time.substring(0, 12);
            }
            buffer.setString(col, row, time + " ", Style.create().gray());
            col += time.length() + 1;
        }

        // Level with color
        if (entry.level != null) {
            Style levelStyle = getLevelStyle(entry.level);
            String shortLevel = entry.level.length() > 5 ? entry.level.substring(0, 5) : entry.level;
            String formatted = String.format("%-5s", shortLevel);
            buffer.setString(col, row, formatted + " ", levelStyle);
            col += 6;
        }

        // Logger name (shortened)
        if (entry.loggerName != null) {
            String shortLogger = shortenLoggerName(entry.loggerName, 20);
            buffer.setString(col, row, shortLogger + " ", Style.create().cyan());
            col += shortLogger.length() + 1;
        }

        // Message
        if (entry.message != null) {
            int remaining = width - col;
            String msg = entry.message;
            if (msg.length() > remaining) {
                msg = msg.substring(0, Math.max(0, remaining));
            }
            buffer.setString(col, row, msg, Style.EMPTY);
        }
    }

    private Style getLevelStyle(String level) {
        if (level == null) {
            return Style.create().white();
        }
        switch (level.toUpperCase()) {
            case "ERROR":
            case "SEVERE":
                return Style.create().red();
            case "WARN":
            case "WARNING":
                return Style.create().yellow();
            case "INFO":
                return Style.create().green();
            case "DEBUG":
            case "FINE":
                return Style.create().blue();
            case "TRACE":
            case "FINEST":
                return Style.create().gray();
            default:
                return Style.create().white();
        }
    }

    private String shortenLoggerName(String name, int maxLen) {
        if (name == null || name.length() <= maxLen) {
            return name;
        }

        // Try to shorten package names
        String[] parts = name.split("\\.");
        if (parts.length <= 1) {
            return name.substring(name.length() - maxLen);
        }

        // Keep last part fully, abbreviate others
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (sb.length() > 0) {
                sb.append(".");
            }
            sb.append(parts[i].charAt(0));
        }
        sb.append(".").append(parts[parts.length - 1]);

        String result = sb.toString();
        if (result.length() > maxLen) {
            return result.substring(result.length() - maxLen);
        }
        return result;
    }

    public void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
            autoScroll = false;
        }
    }

    public void scrollDown() {
        int maxOffset = getFilteredEntries().size() - visibleRows;
        if (scrollOffset < maxOffset) {
            scrollOffset++;
        }
        if (scrollOffset >= maxOffset) {
            autoScroll = true;
        }
    }

    public void scrollToEnd() {
        List<LogEntry> filtered = getFilteredEntries();
        scrollOffset = Math.max(0, filtered.size() - visibleRows);
        autoScroll = true;
    }

    public int getLogCount() {
        return logEntries.size();
    }

    public int getFilteredLogCount() {
        return getFilteredEntries().size();
    }

    /**
     * Handle a key press for the log panel.
     *
     * @param key the key code
     * @return true if the key was handled
     */
    public boolean handleKey(int key) {
        switch (key) {
            case '1':
                setCurrentTab(LogTab.SERVER);
                return true;
            case '2':
                setCurrentTab(LogTab.TESTING);
                return true;
            default:
                return false;
        }
    }

    /**
     * Data class for log entries.
     */
    private static class LogEntry {
        String timestamp;
        String level;
        String loggerName;
        String threadName;
        String message;
    }
}
