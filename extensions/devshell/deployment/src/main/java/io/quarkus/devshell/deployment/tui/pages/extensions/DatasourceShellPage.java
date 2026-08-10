package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.deployment.tui.widgets.KeyValuePanel;
import tools.jackson.databind.JsonNode;

public class DatasourceShellPage extends BaseExtensionPage {

    private final List<KeyValuePanel> panels = new ArrayList<>();
    private int scrollOffset = 0;

    public DatasourceShellPage() {
        super("quarkus-datasource", "Datasources");
    }

    @Override
    public void loadData() {
        JsonNode result = getBuildTimeDataAsJson("datasources");
        panels.clear();

        if (result.isArray()) {
            for (JsonNode ds : result) {
                String name = ds.path("name").asText("default");
                KeyValuePanel panel = new KeyValuePanel(name);
                panel.addIfPresent("JDBC URL", ds.path("jdbcUrl").asText(""));
                panel.addIfPresent("DB Kind", ds.path("dbKind").asText(""));
                panel.addIfPresent("Username", ds.path("username").asText(""));
                panel.addIfPresent("Driver", ds.path("driver").asText(""));
                panels.add(panel);
            }
        } else if (result.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = result.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                KeyValuePanel panel = new KeyValuePanel(entry.getKey());
                JsonNode ds = entry.getValue();
                if (ds.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> props = ds.properties().iterator();
                    while (props.hasNext()) {
                        Map.Entry<String, JsonNode> prop = props.next();
                        panel.add(prop.getKey(), prop.getValue().asText(""));
                    }
                } else {
                    panel.add(entry.getKey(), ds.asText(""));
                }
                panels.add(panel);
            }
        }
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (panels.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No datasources configured", DIM_STYLE);
            return;
        }
        int row = area.y() - scrollOffset;
        for (KeyValuePanel panel : panels) {
            row = panel.render(buffer, row, area.x() + 1, area.width() - 2);
            row++;
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                scrollOffset++;
                ctx.requestRedraw();
                return true;
            case 'k':
            case KeyCode.UP:
                if (scrollOffset > 0) {
                    scrollOffset--;
                    ctx.requestRedraw();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    protected String getFooterText() {
        return "[j/k] Scroll";
    }
}
