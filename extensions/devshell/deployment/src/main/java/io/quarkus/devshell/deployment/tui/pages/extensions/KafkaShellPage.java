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

public class KafkaShellPage extends BaseExtensionPage {

    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();

    private List<String> topics = List.of();
    private List<String> consumerGroups = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public KafkaShellPage() {
        super("quarkus-kafka-client", "Kafka");
        setTabs("Topics", "Consumer Groups");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getInfo");
        List<String> t = new ArrayList<>();
        List<String> cg = new ArrayList<>();

        JsonNode topicsNode = result.path("topics");
        if (topicsNode.isArray()) {
            for (JsonNode item : topicsNode) {
                t.add(item.isObject() ? item.path("name").asText(item.toString()) : item.asText(""));
            }
        }

        JsonNode groupsNode = result.path("consumerGroups");
        if (groupsNode.isArray()) {
            for (JsonNode item : groupsNode) {
                cg.add(item.isObject() ? item.path("groupId").asText(item.toString()) : item.asText(""));
            }
        }

        topics = t;
        consumerGroups = cg;
    }

    @Override
    protected void onTabChanged(int newTab) {
        selectedIndex = 0;
        scrollOffset = 0;
    }

    private List<String> currentList() {
        return getActiveTab() == 0 ? topics : consumerGroups;
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        List<String> items = currentList();
        if (items.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No " + getActiveTabName() + " found", DIM_STYLE);
            return;
        }

        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < items.size(); i++) {
            int idx = scrollOffset + i;
            boolean selected = idx == selectedIndex;
            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }
            buffer.setString(area.x() + 2, area.y() + i, truncate(items.get(idx), area.width() - 3),
                    selected ? SELECTED : VALUE_STYLE);
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        List<String> items = currentList();
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                if (selectedIndex < items.size() - 1) {
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
        return "[j/k] Navigate  " + currentList().size() + " items";
    }
}
