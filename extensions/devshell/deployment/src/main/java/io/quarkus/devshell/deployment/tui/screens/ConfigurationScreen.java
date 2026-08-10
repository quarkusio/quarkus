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

public class ConfigurationScreen implements Screen {

    private static final Style PHASE_BUILD = Style.EMPTY.red();
    private static final Style PHASE_BUILD_RUN = Style.EMPTY.yellow();
    private static final Style PHASE_RUN = Style.EMPTY.green();
    private static final Style DIM = Style.EMPTY.gray();
    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style NORMAL = Style.EMPTY.white();

    private AppContext ctx;
    private volatile List<ConfigItem> items = List.of();
    private volatile List<ConfigItem> filteredItems = List.of();
    private volatile boolean loading = true;
    private volatile String errorMessage;
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private String filter = "";
    private boolean filterMode = false;

    @Override
    public String getTitle() {
        return "Configuration";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        loadDataAsync();
    }

    private void loadDataAsync() {
        loading = true;
        errorMessage = null;
        ctx.requestRedraw();

        CompletableFuture.runAsync(() -> {
            try {
                JsonNode result = ctx.getJsonRpcClient().call("devui-configuration", "getAllConfiguration");
                items = parseConfig(result);
                applyFilter();
                loading = false;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                items = List.of();
                filteredItems = List.of();
                loading = false;
            }
            ctx.requestRedraw();
        });
    }

    private List<ConfigItem> parseConfig(JsonNode result) {
        List<ConfigItem> list = new ArrayList<>();
        JsonNode arr = result.isArray() ? result : result.path("_array");
        if (!arr.isArray()) {
            return list;
        }

        for (JsonNode item : arr) {
            String currentValue = "";
            String sourceName = "";
            JsonNode cv = item.path("configValue");
            if (cv.isObject()) {
                currentValue = cv.path("value").asText("");
                sourceName = cv.path("sourceName").asText("");
            } else if (cv.isTextual()) {
                currentValue = cv.asText("");
            }

            list.add(new ConfigItem(
                    item.path("name").asText(""),
                    item.path("description").asText(""),
                    item.path("defaultValue").asText(""),
                    currentValue,
                    item.path("configPhase").asText(""),
                    sourceName));
        }
        return list;
    }

    private void applyFilter() {
        if (filter.isEmpty()) {
            filteredItems = new ArrayList<>(items);
        } else {
            String lowerFilter = filter.toLowerCase();
            filteredItems = items.stream()
                    .filter(i -> i.name.toLowerCase().contains(lowerFilter)
                            || i.value.toLowerCase().contains(lowerFilter))
                    .toList();
        }
        selectedIndex = Math.min(selectedIndex, Math.max(0, filteredItems.size() - 1));
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (loading) {
            buffer.setString(area.x() + 2, area.y() + 1, "Loading configuration...", Style.EMPTY.yellow());
            return;
        }
        if (errorMessage != null) {
            buffer.setString(area.x() + 2, area.y() + 1, "Error: " + errorMessage, Style.EMPTY.red());
            return;
        }

        var areas = Layout.vertical()
                .constraints(Constraint.length(1), Constraint.length(1), Constraint.fill(), Constraint.length(1))
                .split(area);

        renderLegend(areas.get(0), buffer);
        renderFilterBar(areas.get(1), buffer);
        renderTable(areas.get(2), buffer);
        renderFooter(areas.get(3), buffer);
    }

    private void renderLegend(Rect area, Buffer buffer) {
        buffer.setLine(area.x(), area.y(), Line.from(
                Span.styled(" * ", PHASE_BUILD), Span.styled("BUILD ", DIM),
                Span.styled(" * ", PHASE_BUILD_RUN), Span.styled("BUILD+RUN ", DIM),
                Span.styled(" * ", PHASE_RUN), Span.styled("RUN ", DIM),
                Span.styled("  " + filteredItems.size() + "/" + items.size() + " properties", DIM)));
    }

    private void renderFilterBar(Rect area, Buffer buffer) {
        if (filterMode) {
            buffer.setLine(area.x(), area.y(), Line.from(
                    Span.styled(" Filter: ", Style.EMPTY.yellow()),
                    Span.styled(filter + "_", NORMAL)));
        } else if (!filter.isEmpty()) {
            buffer.setLine(area.x(), area.y(), Line.from(
                    Span.styled(" Filter: ", DIM),
                    Span.styled(filter, NORMAL),
                    Span.styled("  (/ to edit, Esc to clear)", DIM)));
        }
    }

    private void renderTable(Rect area, Buffer buffer) {
        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < filteredItems.size(); i++) {
            int idx = scrollOffset + i;
            ConfigItem item = filteredItems.get(idx);
            boolean selected = idx == selectedIndex;
            Style rowStyle = selected ? SELECTED : NORMAL;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            Style phaseStyle = switch (item.configPhase) {
                case "BUILD_TIME" -> PHASE_BUILD;
                case "BUILD_AND_RUN_TIME_FIXED" -> PHASE_BUILD_RUN;
                default -> PHASE_RUN;
            };

            int x = area.x();
            buffer.setString(x, area.y() + i, " * ", selected ? rowStyle : phaseStyle);
            x += 3;

            int nameWidth = area.width() / 2;
            String name = truncate(item.name, nameWidth);
            buffer.setString(x, area.y() + i, name, rowStyle);
            x += nameWidth;

            String value = item.value.isEmpty() ? "(not set)" : item.value;
            value = truncate(value, area.width() - x + area.x());
            buffer.setString(x, area.y() + i, value, selected ? rowStyle : DIM);
        }
    }

    private void renderFooter(Rect area, Buffer buffer) {
        buffer.setLine(area.x(), area.y(), Line.from(
                Span.styled(" Enter", Style.EMPTY.cyan()), Span.styled(" Details  ", DIM),
                Span.styled("/", Style.EMPTY.cyan()), Span.styled(" Filter  ", DIM),
                Span.styled("R", Style.EMPTY.cyan()), Span.styled(" Refresh  ", DIM),
                Span.styled("ESC", Style.EMPTY.cyan()), Span.styled(" Back", DIM)));
    }

    @Override
    public boolean handleKey(int[] keys) {
        if (loading) {
            return false;
        }
        if (filterMode) {
            return handleFilterKey(keys);
        }

        if (keys.length == 1) {
            switch (keys[0]) {
                case 'j':
                    return moveDown();
                case 'k':
                    return moveUp();
                case '/':
                    filterMode = true;
                    ctx.requestRedraw();
                    return true;
                case '\r':
                case '\n':
                    if (!filteredItems.isEmpty()) {
                        ctx.navigateTo(new ConfigDetailScreen(filteredItems.get(selectedIndex)));
                    }
                    return true;
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

    private boolean handleFilterKey(int[] keys) {
        if (keys.length == 1) {
            int k = keys[0];
            if (k == 27) {
                filterMode = false;
                filter = "";
                applyFilter();
                ctx.requestRedraw();
                return true;
            }
            if (k == '\r' || k == '\n') {
                filterMode = false;
                ctx.requestRedraw();
                return true;
            }
            if (k == 127 || k == 8) {
                if (!filter.isEmpty()) {
                    filter = filter.substring(0, filter.length() - 1);
                    applyFilter();
                    ctx.requestRedraw();
                }
                return true;
            }
            if (k >= 32 && k < 127) {
                filter += (char) k;
                applyFilter();
                ctx.requestRedraw();
                return true;
            }
        }
        return true;
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
        if (selectedIndex < filteredItems.size() - 1) {
            selectedIndex++;
            ctx.requestRedraw();
            return true;
        }
        return false;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || maxLen <= 0)
            return "";
        if (s.length() <= maxLen)
            return s;
        return s.substring(0, maxLen - 1) + "~";
    }

    record ConfigItem(String name, String description, String defaultValue, String value, String configPhase,
            String sourceName) {
    }
}
