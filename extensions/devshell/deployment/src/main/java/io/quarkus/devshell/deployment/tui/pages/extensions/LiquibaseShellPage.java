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

public class LiquibaseShellPage extends BaseExtensionPage {

    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style SUCCESS_STYLE = Style.EMPTY.green();
    private static final Style PENDING_STYLE = Style.EMPTY.yellow();

    private List<DatasourceEntry> datasources = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public LiquibaseShellPage() {
        super("quarkus-liquibase", "Liquibase");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getLiquibaseFactories");
        List<DatasourceEntry> entries = new ArrayList<>();
        JsonNode arr = result.isArray() ? result : result.path("_array");
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                String name = item.path("name").asText(item.path("datasource").asText("default"));
                String status = item.path("status").asText(item.path("state").asText(""));
                int changesets = item.path("changeSetsCount").asInt(
                        item.path("changesets").isArray() ? item.path("changesets").size() : 0);
                entries.add(new DatasourceEntry(name, status, changesets));
            }
        }
        datasources = entries;
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (datasources.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No datasources found", DIM_STYLE);
            return;
        }

        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < datasources.size(); i++) {
            int idx = scrollOffset + i;
            DatasourceEntry entry = datasources.get(idx);
            boolean selected = idx == selectedIndex;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            boolean upToDate = "up-to-date".equalsIgnoreCase(entry.status)
                    || "EXECUTED".equalsIgnoreCase(entry.status);
            int x = area.x() + 1;
            buffer.setString(x, area.y() + i, truncate(entry.name, 25), selected ? SELECTED : VALUE_STYLE);
            x += 26;

            String statusText = entry.status.isEmpty() ? entry.changesets + " changesets" : entry.status;
            buffer.setString(x, area.y() + i, truncate(statusText, area.width() - x + area.x()),
                    selected ? SELECTED : (upToDate ? SUCCESS_STYLE : PENDING_STYLE));
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                if (selectedIndex < datasources.size() - 1) {
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
            case 'u':
            case 'U':
                doUpdate();
                return true;
            default:
                return false;
        }
    }

    private void doUpdate() {
        try {
            rpcCall("migrate");
            loadDataAsync();
        } catch (Exception e) {
            errorMessage = e.getMessage();
            ctx.requestRedraw();
        }
    }

    @Override
    protected String getFooterText() {
        return "[j/k] Navigate  [U] Update  " + datasources.size() + " datasources";
    }

    private record DatasourceEntry(String name, String status, int changesets) {
    }
}
