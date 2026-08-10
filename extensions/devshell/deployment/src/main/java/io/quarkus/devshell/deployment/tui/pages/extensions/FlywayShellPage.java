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

public class FlywayShellPage extends BaseExtensionPage {

    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style SUCCESS_STYLE = Style.EMPTY.green();
    private static final Style PENDING_STYLE = Style.EMPTY.yellow();

    private List<DatasourceEntry> datasources = List.of();
    private List<MigrationEntry> migrations = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public FlywayShellPage() {
        super("quarkus-flyway", "Flyway");
        setTabs("Info", "Migrations");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getDatasources");
        List<DatasourceEntry> ds = new ArrayList<>();
        List<MigrationEntry> migs = new ArrayList<>();

        JsonNode arr = result.isArray() ? result : result.path("_array");
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                String name = item.path("name").asText(item.path("datasource").asText("default"));
                ds.add(new DatasourceEntry(name,
                        item.path("currentVersion").asText(""),
                        item.path("schemaVersion").asText("")));

                JsonNode migsNode = item.path("migrations");
                if (migsNode.isArray()) {
                    for (JsonNode mig : migsNode) {
                        migs.add(new MigrationEntry(
                                name,
                                mig.path("version").asText(""),
                                mig.path("description").asText(""),
                                mig.path("state").asText(mig.path("status").asText(""))));
                    }
                }
            }
        }
        datasources = ds;
        migrations = migs;
    }

    @Override
    protected void onTabChanged(int newTab) {
        selectedIndex = 0;
        scrollOffset = 0;
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (getActiveTab() == 0) {
            renderInfo(area, buffer);
        } else {
            renderMigrations(area, buffer);
        }
    }

    private void renderInfo(Rect area, Buffer buffer) {
        if (datasources.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No datasources found", DIM_STYLE);
            return;
        }

        int row = area.y();
        for (int i = 0; i < datasources.size() && row < area.y() + area.height(); i++) {
            DatasourceEntry ds = datasources.get(i);
            buffer.setString(area.x() + 1, row, ds.name, HEADER_STYLE);
            row++;
            if (row < area.y() + area.height()) {
                buffer.setString(area.x() + 3, row, "Version: " + ds.currentVersion, VALUE_STYLE);
                row++;
            }
            if (row < area.y() + area.height() && !ds.schemaVersion.isEmpty()) {
                buffer.setString(area.x() + 3, row, "Schema:  " + ds.schemaVersion, DIM_STYLE);
                row++;
            }
            row++;
        }
    }

    private void renderMigrations(Rect area, Buffer buffer) {
        if (migrations.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No migrations found", DIM_STYLE);
            return;
        }

        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < migrations.size(); i++) {
            int idx = scrollOffset + i;
            MigrationEntry entry = migrations.get(idx);
            boolean selected = idx == selectedIndex;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            boolean success = "SUCCESS".equalsIgnoreCase(entry.state) || "APPLIED".equalsIgnoreCase(entry.state);
            int x = area.x() + 1;
            buffer.setString(x, area.y() + i, truncate(entry.version, 10), selected ? SELECTED : LABEL_STYLE);
            x += 11;
            buffer.setString(x, area.y() + i, truncate(entry.description, 35), selected ? SELECTED : VALUE_STYLE);
            x += 36;
            buffer.setString(x, area.y() + i, truncate(entry.state, 12),
                    selected ? SELECTED : (success ? SUCCESS_STYLE : PENDING_STYLE));
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                if (getActiveTab() == 1 && selectedIndex < migrations.size() - 1) {
                    selectedIndex++;
                    ctx.requestRedraw();
                }
                return true;
            case 'k':
            case KeyCode.UP:
                if (getActiveTab() == 1 && selectedIndex > 0) {
                    selectedIndex--;
                    ctx.requestRedraw();
                }
                return true;
            case 'm':
            case 'M':
                doAction("migrate");
                return true;
            case 'c':
            case 'C':
                doAction("clean");
                return true;
            default:
                return false;
        }
    }

    private void doAction(String method) {
        try {
            rpcCall(method);
            loadDataAsync();
        } catch (Exception e) {
            errorMessage = e.getMessage();
            ctx.requestRedraw();
        }
    }

    @Override
    protected String getFooterText() {
        return "[M] Migrate  [C] Clean  [j/k] Navigate";
    }

    private record DatasourceEntry(String name, String currentVersion, String schemaVersion) {
    }

    private record MigrationEntry(String datasource, String version, String description, String state) {
    }

}
