package io.quarkus.devshell.deployment.tui.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.Screen;
import tools.jackson.databind.JsonNode;

public class BuildMetricsScreen implements Screen {

    private static final Style DIM = Style.EMPTY.gray();
    private static final Style NORMAL = Style.EMPTY.white();
    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style HEADER = Style.EMPTY.cyan().bold();
    private static final Style GREEN = Style.EMPTY.green();

    private AppContext ctx;
    private volatile boolean loading = true;
    private volatile String errorMessage;
    private volatile long totalDuration;
    private volatile int threadCount;
    private volatile List<BuildStep> steps = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    @Override
    public String getTitle() {
        return "Build Metrics";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        loadDataAsync();
    }

    private void loadDataAsync() {
        loading = true;
        ctx.requestRedraw();

        CompletableFuture.runAsync(() -> {
            try {
                JsonNode result = ctx.getJsonRpcClient().call("devui-build-metrics", "getBuildMetrics");
                parseBuildMetrics(result);
                loading = false;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                loading = false;
            }
            ctx.requestRedraw();
        });
    }

    private void parseBuildMetrics(JsonNode result) {
        totalDuration = result.path("duration").asLong(0);
        threadCount = result.path("numberOfThreads").asInt(0);

        List<BuildStep> list = new ArrayList<>();
        JsonNode items = result.path("records");
        if (items.isArray()) {
            for (JsonNode item : items) {
                list.add(new BuildStep(
                        item.path("stepId").asText(item.path("name").asText("")),
                        item.path("duration").asLong(0),
                        item.path("thread").asText("")));
            }
        }
        list.sort((a, b) -> Long.compare(b.duration, a.duration));
        steps = list;
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (loading) {
            buffer.setString(area.x() + 2, area.y() + 1, "Loading build metrics...", Style.EMPTY.yellow());
            return;
        }
        if (errorMessage != null) {
            buffer.setString(area.x() + 2, area.y() + 1, "Error: " + errorMessage, Style.EMPTY.red());
            return;
        }

        var areas = Layout.vertical()
                .constraints(Constraint.length(2), Constraint.fill(), Constraint.length(1))
                .split(area);

        renderSummary(areas.get(0), buffer);
        renderSteps(areas.get(1), buffer);
        renderFooter(areas.get(2), buffer);
    }

    private void renderSummary(Rect area, Buffer buffer) {
        buffer.setLine(area.x(), area.y(), Line.from(
                Span.styled(" Build duration: ", DIM),
                Span.styled(formatDuration(totalDuration), GREEN),
                Span.styled("  Threads: ", DIM),
                Span.styled(String.valueOf(threadCount), NORMAL),
                Span.styled("  Steps: ", DIM),
                Span.styled(String.valueOf(steps.size()), NORMAL)));

        buffer.setLine(area.x(), area.y() + 1, Line.from(
                Span.styled(" Step", HEADER),
                Span.styled(" ".repeat(Math.max(1, area.width() / 2 - 10)), Style.EMPTY),
                Span.styled("Duration", HEADER),
                Span.styled("    Thread", HEADER)));
    }

    private void renderSteps(Rect area, Buffer buffer) {
        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        int nameWidth = area.width() / 2;

        for (int i = 0; i < visibleRows && (scrollOffset + i) < steps.size(); i++) {
            int idx = scrollOffset + i;
            BuildStep step = steps.get(idx);
            boolean selected = idx == selectedIndex;
            Style rowStyle = selected ? SELECTED : NORMAL;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            String name = truncate(step.name, nameWidth);
            buffer.setString(area.x() + 1, area.y() + i, name, rowStyle);
            buffer.setString(area.x() + nameWidth + 1, area.y() + i,
                    String.format("%8s", formatDuration(step.duration)), selected ? SELECTED : GREEN);
            buffer.setString(area.x() + nameWidth + 12, area.y() + i,
                    truncate(step.thread, 20), selected ? SELECTED : DIM);
        }
    }

    private void renderFooter(Rect area, Buffer buffer) {
        buffer.setLine(area.x(), area.y(), Line.from(
                Span.styled(" R", Style.EMPTY.cyan()), Span.styled(" Refresh  ", DIM),
                Span.styled("ESC", Style.EMPTY.cyan()), Span.styled(" Back", DIM)));
    }

    @Override
    public boolean handleKey(int[] keys) {
        if (keys.length == 1) {
            switch (keys[0]) {
                case 'j':
                    return moveDown();
                case 'k':
                    return moveUp();
                case 'r':
                case 'R':
                    loadDataAsync();
                    return true;
            }
        }
        if (keys.length == 3 && keys[0] == 27 && keys[1] == '[') {
            if (keys[2] == 'A')
                return moveUp();
            if (keys[2] == 'B')
                return moveDown();
        }
        return false;
    }

    private boolean moveUp() {
        if (selectedIndex > 0) {
            selectedIndex--;
            ctx.requestRedraw();
            return true;
        }
        return false;
    }

    private boolean moveDown() {
        if (selectedIndex < steps.size() - 1) {
            selectedIndex++;
            ctx.requestRedraw();
            return true;
        }
        return false;
    }

    private static String formatDuration(long ms) {
        if (ms < 1000)
            return ms + "ms";
        return String.format("%.1fs", ms / 1000.0);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || maxLen <= 0)
            return "";
        if (s.length() <= maxLen)
            return s;
        return s.substring(0, maxLen - 1) + "~";
    }

    private record BuildStep(String name, long duration, String thread) {
    }
}
