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

public class InfoShellPage extends BaseExtensionPage {

    private final List<KeyValuePanel> panels = new ArrayList<>();
    private int scrollOffset = 0;

    public InfoShellPage() {
        super("quarkus-info", "Info");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getApplicationAndEnvironmentInfo");
        panels.clear();

        if (result.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> sections = result.properties().iterator();
            while (sections.hasNext()) {
                Map.Entry<String, JsonNode> section = sections.next();
                KeyValuePanel panel = new KeyValuePanel(section.getKey());
                JsonNode value = section.getValue();
                if (value.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> fields = value.properties().iterator();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        panel.add(field.getKey(), field.getValue().asText(""));
                    }
                } else {
                    panel.add(section.getKey(), value.asText(""));
                }
                panels.add(panel);
            }
        }
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (panels.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No info available", DIM_STYLE);
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
