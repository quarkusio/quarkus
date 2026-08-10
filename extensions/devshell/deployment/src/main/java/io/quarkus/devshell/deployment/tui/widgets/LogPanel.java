package io.quarkus.devshell.deployment.tui.widgets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import io.quarkus.devshell.deployment.tui.AppContext;
import tools.jackson.databind.JsonNode;

/**
 * A log streaming widget that renders into a provided area.
 * Displays real-time log output from the Dev UI logstream service
 * with tabs for Server (all logs) and Testing (filtered) views.
 */
public class LogPanel {

    private static final Logger log = Logger.getLogger(LogPanel.class);

    private static final int MAX_ENTRIES = 500;
    private static final String NAMESPACE = "devui-logstream";

    private static final Style TAB_ACTIVE = Style.EMPTY.white().bold().onBlack();
    private static final Style TAB_INACTIVE = Style.EMPTY.gray().onBlack();
    private static final Style TAB_BAR_BG = Style.EMPTY.onBlack();
    private static final Style LEVEL_ERROR = Style.EMPTY.red();
    private static final Style LEVEL_WARN = Style.EMPTY.yellow();
    private static final Style LEVEL_INFO = Style.EMPTY.green();
    private static final Style LEVEL_DEBUG = Style.EMPTY.blue();
    private static final Style LEVEL_TRACE = Style.EMPTY.gray();
    private static final Style TIMESTAMP_STYLE = Style.EMPTY.gray();
    private static final Style LOGGER_STYLE = Style.EMPTY.cyan();
    private static final Style MESSAGE_STYLE = Style.EMPTY.white();

    private final List<LogEntry> entries = Collections.synchronizedList(new ArrayList<>());
    private int activeTab = 0; // 0=Server, 1=Testing
    private int scrollOffset = 0;
    private boolean autoScroll = true;
    private int subscriptionId = -1;
    private boolean streaming = false;
    private AppContext ctx;

    public void start(AppContext ctx) {
        this.ctx = ctx;

        // Load history asynchronously
        ctx.getJsonRpcClient().callAsync(NAMESPACE, "history")
                .thenAccept(this::parseHistory)
                .exceptionally(t -> {
                    log.debug("Failed to load log history", t);
                    return null;
                });

        // Subscribe to live log stream
        try {
            subscriptionId = ctx.getJsonRpcClient().subscribe(NAMESPACE, "streamLog", this::onLogMessage);
            streaming = true;
        } catch (Exception e) {
            log.debug("Failed to subscribe to log stream", e);
            streaming = false;
        }
    }

    public void stop() {
        if (subscriptionId >= 0) {
            try {
                ctx.getJsonRpcClient().unsubscribe(subscriptionId);
            } catch (Exception e) {
                log.debug("Failed to unsubscribe from log stream", e);
            }
            subscriptionId = -1;
            streaming = false;
        }
    }

    public void render(Rect area, Buffer buffer) {
        if (area.height() < 2) {
            return;
        }

        // Fill background
        buffer.fill(area, new Cell(" ", Style.EMPTY));

        // Row 0: Tab bar
        renderTabBar(area, buffer);

        // Rows 1+: Log entries
        int contentHeight = area.height() - 1;
        if (contentHeight <= 0) {
            return;
        }

        List<LogEntry> filtered = getFilteredEntries();

        if (autoScroll) {
            scrollOffset = Math.max(0, filtered.size() - contentHeight);
        }

        for (int i = 0; i < contentHeight; i++) {
            int entryIndex = scrollOffset + i;
            int row = area.y() + 1 + i;

            if (entryIndex >= 0 && entryIndex < filtered.size()) {
                renderLogEntry(filtered.get(entryIndex), area.x(), row, area.width(), buffer);
            }
        }
    }

    private void renderTabBar(Rect area, Buffer buffer) {
        int y = area.y();

        // Fill tab bar background
        for (int x = area.x(); x < area.x() + area.width(); x++) {
            buffer.set(x, y, new Cell(" ", TAB_BAR_BG));
        }

        int serverCount;
        int testingCount;
        synchronized (entries) {
            serverCount = entries.size();
            testingCount = (int) entries.stream()
                    .filter(e -> e.threadName != null && e.threadName.contains("Test runner"))
                    .count();
        }

        String serverLabel = " Server (" + serverCount + ") ";
        String testingLabel = " Testing (" + testingCount + ") ";
        String switchHint = " [1/2] switch";

        Style serverStyle = activeTab == 0 ? TAB_ACTIVE : TAB_INACTIVE;
        Style testingStyle = activeTab == 1 ? TAB_ACTIVE : TAB_INACTIVE;

        int x = area.x() + 1;
        buffer.setLine(x, y, Line.from(
                Span.styled(serverLabel, serverStyle),
                Span.styled(" ", TAB_BAR_BG),
                Span.styled(testingLabel, testingStyle),
                Span.styled(" ", TAB_BAR_BG),
                Span.styled(switchHint, TAB_INACTIVE)));
    }

    private void renderLogEntry(LogEntry entry, int startX, int row, int width, Buffer buffer) {
        int x = startX;
        int maxX = startX + width;

        // Timestamp (HH:mm:ss)
        String ts = shortenTimestamp(entry.timestamp);
        if (x + ts.length() < maxX) {
            buffer.setString(x, row, ts, TIMESTAMP_STYLE);
            x += ts.length();
        }

        // Space
        if (x < maxX) {
            buffer.setString(x, row, " ", Style.EMPTY);
            x++;
        }

        // Level (5 chars, color-coded)
        String level = String.format("%-5s", entry.level);
        Style levelStyle = getLevelStyle(entry.level);
        if (x + level.length() < maxX) {
            buffer.setString(x, row, level, levelStyle);
            x += level.length();
        }

        // Space
        if (x < maxX) {
            buffer.setString(x, row, " ", Style.EMPTY);
            x++;
        }

        // Logger name (abbreviated)
        int remainingForLogger = Math.min(25, (maxX - x) / 3);
        if (remainingForLogger > 0) {
            String loggerShort = shortenLoggerName(entry.loggerName, remainingForLogger);
            loggerShort = String.format("%-" + remainingForLogger + "s", loggerShort);
            if (x + loggerShort.length() < maxX) {
                buffer.setString(x, row, loggerShort, LOGGER_STYLE);
                x += loggerShort.length();
            }
        }

        // Space
        if (x < maxX) {
            buffer.setString(x, row, " ", Style.EMPTY);
            x++;
        }

        // Message (fill remaining width)
        if (x < maxX && entry.message != null) {
            int remaining = maxX - x;
            String msg = entry.message.length() > remaining
                    ? entry.message.substring(0, remaining - 1) + "…"
                    : entry.message;
            buffer.setString(x, row, msg, MESSAGE_STYLE);
        }
    }

    public boolean handleKey(int key) {
        if (key == '1') {
            if (activeTab != 0) {
                activeTab = 0;
                scrollOffset = 0;
                autoScroll = true;
            }
            return true;
        } else if (key == '2') {
            if (activeTab != 1) {
                activeTab = 1;
                scrollOffset = 0;
                autoScroll = true;
            }
            return true;
        }
        return false;
    }

    public void switchTab() {
        activeTab = activeTab == 0 ? 1 : 0;
        scrollOffset = 0;
        autoScroll = true;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public int getActiveTab() {
        return activeTab;
    }

    private void onLogMessage(JsonNode msg) {
        LogEntry entry = parseLogEntry(msg);
        if (entry != null) {
            synchronized (entries) {
                entries.add(entry);
                while (entries.size() > MAX_ENTRIES) {
                    entries.remove(0);
                }
            }
            if (autoScroll && ctx != null) {
                ctx.requestRedraw();
            }
        }
    }

    private void parseHistory(JsonNode result) {
        if (result == null || !result.isArray()) {
            return;
        }
        synchronized (entries) {
            for (JsonNode item : result) {
                LogEntry entry = parseLogEntry(item);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            while (entries.size() > MAX_ENTRIES) {
                entries.remove(0);
            }
        }
        autoScroll = true;
        if (ctx != null) {
            ctx.requestRedraw();
        }
    }

    private LogEntry parseLogEntry(JsonNode node) {
        if (node == null) {
            return null;
        }
        String timestamp = node.path("timestamp").asText("");
        String level = node.path("level").asText("INFO");
        String loggerName = node.path("loggerName").asText("");
        String message = node.path("message").asText("");
        String threadName = node.path("threadName").asText("");
        return new LogEntry(timestamp, level, loggerName, message, threadName);
    }

    private List<LogEntry> getFilteredEntries() {
        synchronized (entries) {
            if (activeTab == 1) {
                List<LogEntry> filtered = new ArrayList<>();
                for (LogEntry e : entries) {
                    if (e.threadName != null && e.threadName.contains("Test runner")) {
                        filtered.add(e);
                    }
                }
                return filtered;
            }
            return new ArrayList<>(entries);
        }
    }

    private String shortenTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "        ";
        }
        // Try to extract HH:mm:ss from various timestamp formats
        // ISO format: 2024-01-01T12:34:56 or similar
        int tIndex = timestamp.indexOf('T');
        if (tIndex >= 0 && tIndex + 9 <= timestamp.length()) {
            return timestamp.substring(tIndex + 1, tIndex + 9);
        }
        // Already short or has space separator
        int spaceIndex = timestamp.indexOf(' ');
        if (spaceIndex >= 0 && spaceIndex + 9 <= timestamp.length()) {
            return timestamp.substring(spaceIndex + 1, spaceIndex + 9);
        }
        // If already 8 chars (HH:mm:ss), return as-is
        if (timestamp.length() == 8 && timestamp.charAt(2) == ':') {
            return timestamp;
        }
        // Truncate if too long
        if (timestamp.length() > 8) {
            return timestamp.substring(0, 8);
        }
        return String.format("%-8s", timestamp);
    }

    static String shortenLoggerName(String name, int maxLen) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        if (name.length() <= maxLen) {
            return name;
        }

        String[] parts = name.split("\\.");
        if (parts.length <= 1) {
            return name.length() > maxLen ? name.substring(0, maxLen) : name;
        }

        // Keep last segment full, abbreviate earlier segments to first char
        String lastPart = parts[parts.length - 1];

        // If even the last part is too long, truncate it
        if (lastPart.length() >= maxLen) {
            return lastPart.substring(0, maxLen);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].length() > 0) {
                sb.append(parts[i].charAt(0)).append('.');
            }
            // Check if remaining parts fit
            String remaining = sb.toString() + lastPart;
            if (remaining.length() >= maxLen) {
                // Too long with abbreviations, just use last part
                return lastPart.length() > maxLen ? lastPart.substring(0, maxLen) : lastPart;
            }
        }
        sb.append(lastPart);

        String result = sb.toString();
        return result.length() > maxLen ? result.substring(0, maxLen) : result;
    }

    private Style getLevelStyle(String level) {
        if (level == null) {
            return MESSAGE_STYLE;
        }
        switch (level.toUpperCase()) {
            case "ERROR":
            case "FATAL":
            case "SEVERE":
                return LEVEL_ERROR;
            case "WARN":
            case "WARNING":
                return LEVEL_WARN;
            case "INFO":
                return LEVEL_INFO;
            case "DEBUG":
                return LEVEL_DEBUG;
            case "TRACE":
            case "FINE":
            case "FINER":
            case "FINEST":
                return LEVEL_TRACE;
            default:
                return MESSAGE_STYLE;
        }
    }

    /**
     * A single log entry from the Dev UI logstream.
     */
    static class LogEntry {
        final String timestamp;
        final String level;
        final String loggerName;
        final String message;
        final String threadName;

        LogEntry(String timestamp, String level, String loggerName, String message, String threadName) {
            this.timestamp = timestamp;
            this.level = level;
            this.loggerName = loggerName;
            this.message = message;
            this.threadName = threadName;
        }
    }
}
