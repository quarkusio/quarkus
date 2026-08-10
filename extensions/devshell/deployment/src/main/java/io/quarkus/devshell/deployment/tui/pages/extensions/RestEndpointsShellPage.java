package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;
import tools.jackson.databind.JsonNode;

public class RestEndpointsShellPage extends BaseExtensionPage {

    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style GET_STYLE = Style.EMPTY.green().bold();
    private static final Style POST_STYLE = Style.EMPTY.yellow().bold();
    private static final Style PUT_STYLE = Style.EMPTY.blue().bold();
    private static final Style DELETE_STYLE = Style.EMPTY.red().bold();

    private List<EndpointEntry> endpoints = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public RestEndpointsShellPage() {
        super("devui-endpoints", "REST Endpoints");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getAllEndpoints");
        List<EndpointEntry> entries = new ArrayList<>();
        JsonNode arr = result.isArray() ? result : result.path("_array");
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                entries.add(new EndpointEntry(
                        item.path("httpMethod").asText("GET").toUpperCase(),
                        item.path("uri").asText(""),
                        item.path("javaMethod").asText("")));
            }
        }
        endpoints = entries;
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (endpoints.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No endpoints found", DIM_STYLE);
            return;
        }

        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < endpoints.size(); i++) {
            int idx = scrollOffset + i;
            EndpointEntry entry = endpoints.get(idx);
            boolean selected = idx == selectedIndex;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            int x = area.x() + 1;
            String method = String.format("%-7s", entry.method);
            Style methodStyle = selected ? SELECTED : methodColor(entry.method);
            buffer.setString(x, area.y() + i, method, methodStyle);
            x += 8;
            buffer.setString(x, area.y() + i, truncate(entry.uri, area.width() - x + area.x() - 1),
                    selected ? SELECTED : VALUE_STYLE);
        }
    }

    private Style methodColor(String method) {
        return switch (method) {
            case "POST" -> POST_STYLE;
            case "PUT" -> PUT_STYLE;
            case "DELETE" -> DELETE_STYLE;
            default -> GET_STYLE;
        };
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                if (selectedIndex < endpoints.size() - 1) {
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
        return "[j/k] Navigate  " + endpoints.size() + " endpoints";
    }

    private record EndpointEntry(String method, String uri, String javaMethod) {
    }
}
