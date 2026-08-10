package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;
import tools.jackson.databind.JsonNode;

public class HealthShellPage extends BaseExtensionPage {

    private static final Style UP_STYLE = Style.EMPTY.green();
    private static final Style DOWN_STYLE = Style.EMPTY.red();
    private static final Style SELECTED_STYLE = Style.EMPTY.reversed();

    private String overallStatus = "";
    private final List<HealthCheck> checks = new ArrayList<>();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public HealthShellPage() {
        super("quarkus-smallrye-health", "Health");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getHealth");
        overallStatus = result.path("status").asText("UNKNOWN");
        checks.clear();

        JsonNode checksNode = result.path("checks");
        if (checksNode.isArray()) {
            for (JsonNode check : checksNode) {
                String name = check.path("name").asText("");
                String status = check.path("status").asText("UNKNOWN");
                JsonNode data = check.path("data");
                checks.add(new HealthCheck(name, status, data));
            }
        }
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        int x = area.x() + 1;
        int y = area.y();
        int w = area.width() - 2;

        // Overall status
        Style statusStyle = "UP".equals(overallStatus) ? UP_STYLE : DOWN_STYLE;
        buffer.setString(x, y, "Overall: ", LABEL_STYLE);
        buffer.setString(x + 9, y, overallStatus, statusStyle);
        y += 2;

        if (checks.isEmpty()) {
            buffer.setString(x, y, "No health checks found", DIM_STYLE);
            return;
        }

        // Checks list
        int visibleRows = Math.min(checks.size(), area.height() - 4);
        for (int i = 0; i < visibleRows; i++) {
            int idx = scrollOffset + i;
            if (idx >= checks.size())
                break;

            HealthCheck check = checks.get(idx);
            boolean selected = idx == selectedIndex;
            String icon = "UP".equals(check.status) ? "+ " : "x ";
            Style iconStyle = "UP".equals(check.status) ? UP_STYLE : DOWN_STYLE;

            if (selected) {
                buffer.fill(new Rect(x, y + i, w, 1), new Cell(" ", SELECTED_STYLE));
            }

            buffer.setString(x, y + i, icon, iconStyle);
            buffer.setString(x + 2, y + i, truncate(check.name, w - 2),
                    selected ? SELECTED_STYLE : VALUE_STYLE);
        }

        // Detail for selected check
        if (selectedIndex >= 0 && selectedIndex < checks.size()) {
            int detailY = y + visibleRows + 1;
            HealthCheck selected = checks.get(selectedIndex);
            buffer.setString(x, detailY, "─".repeat(w), DIM_STYLE);
            detailY++;
            buffer.setString(x, detailY, selected.name, HEADER_STYLE);
            detailY++;
            buffer.setString(x, detailY, "Status: ", LABEL_STYLE);
            Style st = "UP".equals(selected.status) ? UP_STYLE : DOWN_STYLE;
            buffer.setString(x + 8, detailY, selected.status, st);
            detailY++;

            if (selected.data != null && selected.data.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = selected.data.properties().iterator();
                while (fields.hasNext() && detailY < area.y() + area.height()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String label = field.getKey() + ": ";
                    buffer.setString(x, detailY, label, LABEL_STYLE);
                    buffer.setString(x + label.length(), detailY,
                            truncate(field.getValue().asText(""), w - label.length()), VALUE_STYLE);
                    detailY++;
                }
            }
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                if (selectedIndex < checks.size() - 1) {
                    selectedIndex++;
                    if (selectedIndex >= scrollOffset + 10)
                        scrollOffset++;
                    ctx.requestRedraw();
                }
                return true;
            case 'k':
            case KeyCode.UP:
                if (selectedIndex > 0) {
                    selectedIndex--;
                    if (selectedIndex < scrollOffset)
                        scrollOffset = selectedIndex;
                    ctx.requestRedraw();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    protected String getFooterText() {
        return "[j/k] Navigate";
    }

    private static class HealthCheck {
        final String name;
        final String status;
        final JsonNode data;

        HealthCheck(String name, String status, JsonNode data) {
            this.name = name;
            this.status = status;
            this.data = data;
        }
    }
}
