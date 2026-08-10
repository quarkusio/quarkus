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

public class ReactiveMessagingShellPage extends BaseExtensionPage {

    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style CONNECTOR_STYLE = Style.EMPTY.yellow();

    private List<ChannelEntry> channels = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public ReactiveMessagingShellPage() {
        super("quarkus-messaging", "Reactive Messaging");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getInfo");
        List<ChannelEntry> entries = new ArrayList<>();

        JsonNode channelsNode = result.path("channels");
        if (!channelsNode.isArray()) {
            channelsNode = result.isArray() ? result : result.path("_array");
        }
        if (channelsNode.isArray()) {
            for (JsonNode item : channelsNode) {
                entries.add(new ChannelEntry(
                        item.path("name").asText(""),
                        item.path("connector").asText(""),
                        item.path("type").asText(""),
                        item.path("publisher").asText(""),
                        item.path("consumer").asText("")));
            }
        }
        channels = entries;
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (channels.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No channels found", DIM_STYLE);
            return;
        }

        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < channels.size(); i++) {
            int idx = scrollOffset + i;
            ChannelEntry entry = channels.get(idx);
            boolean selected = idx == selectedIndex;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            int x = area.x() + 1;
            buffer.setString(x, area.y() + i, truncate(entry.name, 30), selected ? SELECTED : VALUE_STYLE);
            x += 31;

            if (!entry.connector.isEmpty()) {
                String conn = "[" + entry.connector + "]";
                buffer.setString(x, area.y() + i, truncate(conn, 20), selected ? SELECTED : CONNECTOR_STYLE);
                x += 21;
            }

            if (!entry.type.isEmpty()) {
                buffer.setString(x, area.y() + i, truncate(entry.type, area.width() - x + area.x()),
                        selected ? SELECTED : DIM_STYLE);
            }
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                if (selectedIndex < channels.size() - 1) {
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
        return "[j/k] Navigate  " + channels.size() + " channels";
    }

    private record ChannelEntry(String name, String connector, String type, String publisher, String consumer) {
    }
}
