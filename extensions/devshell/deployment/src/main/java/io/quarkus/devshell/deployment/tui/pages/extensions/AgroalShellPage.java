package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.deployment.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.deployment.tui.widgets.ListView;
import tools.jackson.databind.JsonNode;

public class AgroalShellPage extends BaseExtensionPage {

    private final ListView<DatasourceInfo> listView = new ListView<>(ds -> ds.name);
    private final List<DatasourceInfo> datasources = new ArrayList<>();

    public AgroalShellPage() {
        super("quarkus-agroal", "Agroal Connection Pools");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getDataSources");
        datasources.clear();

        if (result.isArray()) {
            for (JsonNode ds : result) {
                String name = ds.path("name").asText("default");
                String jdbcUrl = ds.path("jdbcUrl").asText("");
                int poolSize = ds.path("poolSize").asInt(ds.path("maxSize").asInt(0));
                int activeCount = ds.path("activeCount").asInt(0);
                int availableCount = ds.path("availableCount").asInt(0);
                datasources.add(new DatasourceInfo(name, jdbcUrl, poolSize, activeCount, availableCount, ds));
            }
        } else if (result.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = result.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode ds = entry.getValue();
                String jdbcUrl = ds.path("jdbcUrl").asText("");
                int poolSize = ds.path("poolSize").asInt(ds.path("maxSize").asInt(0));
                int activeCount = ds.path("activeCount").asInt(0);
                int availableCount = ds.path("availableCount").asInt(0);
                datasources.add(
                        new DatasourceInfo(entry.getKey(), jdbcUrl, poolSize, activeCount, availableCount, ds));
            }
        }
        listView.setItems(datasources);
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (datasources.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No Agroal datasources found", DIM_STYLE);
            return;
        }

        var areas = Layout.horizontal()
                .constraints(Constraint.percentage(35), Constraint.percentage(65))
                .split(area);

        // Datasource list
        Rect listArea = areas.get(0);
        listView.setVisibleRows(listArea.height());
        listView.setWidth(listArea.width() - 1);
        listView.render(buffer, listArea.y(), listArea.x() + 1);

        // Detail panel
        Rect detailArea = areas.get(1);
        int x = detailArea.x() + 1;
        int y = detailArea.y();
        int w = detailArea.width() - 2;

        DatasourceInfo selected = listView.getSelectedItem();
        if (selected != null) {
            KeyValuePanel detail = new KeyValuePanel(selected.name);
            detail.addIfPresent("JDBC URL", selected.jdbcUrl);
            detail.add("Pool Size", String.valueOf(selected.poolSize));
            detail.add("Active", String.valueOf(selected.activeCount));
            detail.add("Available", String.valueOf(selected.availableCount));

            // Add any extra fields from the raw JSON
            if (selected.raw.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = selected.raw.properties().iterator();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    String key = field.getKey();
                    if (!"name".equals(key) && !"jdbcUrl".equals(key)
                            && !"poolSize".equals(key) && !"maxSize".equals(key)
                            && !"activeCount".equals(key) && !"availableCount".equals(key)) {
                        detail.addIfPresent(key, field.getValue().asText(""));
                    }
                }
            }
            detail.render(buffer, y, x, w);
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                listView.moveDown();
                ctx.requestRedraw();
                return true;
            case 'k':
            case KeyCode.UP:
                listView.moveUp();
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_DOWN:
                listView.pageDown();
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_UP:
                listView.pageUp();
                ctx.requestRedraw();
                return true;
            default:
                return false;
        }
    }

    @Override
    protected String getFooterText() {
        return "[j/k] Navigate";
    }

    private static class DatasourceInfo {
        final String name;
        final String jdbcUrl;
        final int poolSize;
        final int activeCount;
        final int availableCount;
        final JsonNode raw;

        DatasourceInfo(String name, String jdbcUrl, int poolSize, int activeCount, int availableCount,
                JsonNode raw) {
            this.name = name;
            this.jdbcUrl = jdbcUrl;
            this.poolSize = poolSize;
            this.activeCount = activeCount;
            this.availableCount = availableCount;
            this.raw = raw;
        }
    }
}
