package io.quarkus.devshell.deployment.tui.pages;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import io.quarkus.devshell.deployment.tui.KeyCode;
import tools.jackson.databind.JsonNode;

public class ConfigurationPage extends BaseExtensionPage {

    private static final Style PHASE_BUILD = Style.EMPTY.red();
    private static final Style PHASE_BUILD_RUN = Style.EMPTY.yellow();
    private static final Style PHASE_RUN = Style.EMPTY.green();
    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();

    private List<ConfigEntry> allEntries = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public ConfigurationPage() {
        super("devui-configuration", "Configuration");
        setTabs("All", "Changed", "Build Time", "Runtime");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getAllConfiguration");
        List<ConfigEntry> entries = new ArrayList<>();
        JsonNode arr = result.isArray() ? result : result.path("_array");
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                String value = "";
                JsonNode cv = item.path("configValue");
                if (cv.isObject()) {
                    value = cv.path("value").asText("");
                } else if (cv.isTextual()) {
                    value = cv.asText("");
                }
                entries.add(new ConfigEntry(
                        item.path("name").asText(""),
                        value,
                        item.path("defaultValue").asText(""),
                        item.path("configPhase").asText("")));
            }
        }
        allEntries = entries;
    }

    private List<ConfigEntry> getFilteredEntries() {
        return switch (getActiveTab()) {
            case 1 -> allEntries.stream()
                    .filter(e -> !e.value.isEmpty() && !e.value.equals(e.defaultValue))
                    .toList();
            case 2 -> allEntries.stream()
                    .filter(e -> "BUILD_TIME".equals(e.phase) || "BUILD_AND_RUN_TIME_FIXED".equals(e.phase))
                    .toList();
            case 3 -> allEntries.stream()
                    .filter(e -> "RUN_TIME".equals(e.phase))
                    .toList();
            default -> allEntries;
        };
    }

    @Override
    protected void onTabChanged(int newTab) {
        selectedIndex = 0;
        scrollOffset = 0;
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        List<ConfigEntry> entries = getFilteredEntries();
        int visibleRows = area.height();

        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < entries.size(); i++) {
            int idx = scrollOffset + i;
            ConfigEntry entry = entries.get(idx);
            boolean selected = idx == selectedIndex;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            Style phaseStyle = switch (entry.phase) {
                case "BUILD_TIME" -> PHASE_BUILD;
                case "BUILD_AND_RUN_TIME_FIXED" -> PHASE_BUILD_RUN;
                default -> PHASE_RUN;
            };

            int x = area.x();
            buffer.setString(x, area.y() + i, " * ", selected ? SELECTED : phaseStyle);
            x += 3;

            int nameWidth = area.width() / 2;
            buffer.setString(x, area.y() + i, truncate(entry.name, nameWidth), selected ? SELECTED : VALUE_STYLE);
            x += nameWidth;

            String value = entry.value.isEmpty() ? "(not set)" : entry.value;
            buffer.setString(x, area.y() + i, truncate(value, area.width() - x + area.x()),
                    selected ? SELECTED : DIM_STYLE);
        }

        if (entries.isEmpty()) {
            buffer.setString(area.x() + 2, area.y() + 1, "No configuration properties found", DIM_STYLE);
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        List<ConfigEntry> entries = getFilteredEntries();

        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                if (selectedIndex < entries.size() - 1) {
                    selectedIndex++;
                    ctx.requestRedraw();
                }
                return true;
            case 'k':
            case KeyCode.UP:
                if (selectedIndex > 0) {
                    selectedIndex--;
                    ctx.requestRedraw();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    protected String getFooterText() {
        List<ConfigEntry> entries = getFilteredEntries();
        return entries.size() + "/" + allEntries.size() + " properties";
    }

    private record ConfigEntry(String name, String value, String defaultValue, String phase) {
    }
}
