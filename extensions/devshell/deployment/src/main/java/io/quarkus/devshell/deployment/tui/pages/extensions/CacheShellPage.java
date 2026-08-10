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
import tools.jackson.databind.node.ObjectNode;

public class CacheShellPage extends BaseExtensionPage {

    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();

    private List<CacheEntry> caches = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public CacheShellPage() {
        super("quarkus-cache", "Cache");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getAll");
        List<CacheEntry> entries = new ArrayList<>();
        JsonNode arr = result.isArray() ? result : result.path("_array");
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                entries.add(new CacheEntry(
                        item.path("name").asText(""),
                        item.path("size").asLong(0)));
            }
        }
        caches = entries;
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (caches.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No caches found", DIM_STYLE);
            return;
        }

        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < caches.size(); i++) {
            int idx = scrollOffset + i;
            CacheEntry entry = caches.get(idx);
            boolean selected = idx == selectedIndex;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            int x = area.x() + 1;
            buffer.setString(x, area.y() + i, truncate(entry.name, 40), selected ? SELECTED : VALUE_STYLE);
            x += 41;

            String size = "size: " + entry.size;
            buffer.setString(x, area.y() + i, size, selected ? SELECTED : DIM_STYLE);
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                if (selectedIndex < caches.size() - 1) {
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
            case 'c':
            case 'C':
                clearSelected();
                return true;
            case 'a':
            case 'A':
                clearAll();
                return true;
            default:
                return false;
        }
    }

    private void clearSelected() {
        if (caches.isEmpty())
            return;
        CacheEntry entry = caches.get(selectedIndex);
        try {
            ObjectNode params = createParams();
            params.put("name", entry.name);
            rpcCall("clear", params);
            loadDataAsync();
        } catch (Exception e) {
            errorMessage = e.getMessage();
            ctx.requestRedraw();
        }
    }

    private void clearAll() {
        try {
            for (CacheEntry entry : caches) {
                ObjectNode params = createParams();
                params.put("name", entry.name);
                rpcCall("clear", params);
            }
            loadDataAsync();
        } catch (Exception e) {
            errorMessage = e.getMessage();
            ctx.requestRedraw();
        }
    }

    @Override
    protected String getFooterText() {
        return "[j/k] Navigate  [C] Clear  [A] Clear All  " + caches.size() + " caches";
    }

    private record CacheEntry(String name, long size) {
    }
}
