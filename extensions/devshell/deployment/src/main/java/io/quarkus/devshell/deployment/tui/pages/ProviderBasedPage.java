package io.quarkus.devshell.deployment.tui.pages;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.screens.ExtensionsListScreen;
import tools.jackson.databind.JsonNode;

/**
 * Renders data for extensions that expose a generic JSON-RPC data endpoint.
 * Falls back to showing extension info when no data is available.
 */
public class ProviderBasedPage extends BaseExtensionPage {

    private final ExtensionsListScreen.ExtensionInfo extensionInfo;
    private final List<Section> sections = new ArrayList<>();
    private int scrollOffset = 0;

    public ProviderBasedPage(ExtensionsListScreen.ExtensionInfo ext, String jsonRpcNamespace) {
        super(jsonRpcNamespace != null ? jsonRpcNamespace : ext.namespace(), ext.name());
        this.extensionInfo = ext;
    }

    @Override
    public void loadData() {
        sections.clear();
        try {
            JsonNode result = rpcCall("getAll");
            parseSections(result);
        } catch (Exception e) {
            // Extension may not have a getAll method, that's OK
        }
    }

    private void parseSections(JsonNode result) {
        if (result == null) {
            return;
        }
        if (result.isArray()) {
            Section section = new Section("Data");
            for (JsonNode item : result) {
                if (item.isObject()) {
                    item.properties().forEach(field -> section.items.add(
                            new Item(field.getKey(), field.getValue().asText(""))));
                } else {
                    section.items.add(new Item("", item.asText("")));
                }
            }
            if (!section.items.isEmpty()) {
                sections.add(section);
            }
        } else if (result.isObject()) {
            Section section = new Section("Data");
            result.properties().forEach(field -> {
                JsonNode value = field.getValue();
                if (value.isObject() || value.isArray()) {
                    section.items.add(new Item(field.getKey(), value.toString()));
                } else {
                    section.items.add(new Item(field.getKey(), value.asText("")));
                }
            });
            if (!section.items.isEmpty()) {
                sections.add(section);
            }
        }
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        int x = area.x() + 1;
        int y = area.y();
        int maxWidth = area.width() - 2;

        if (sections.isEmpty()) {
            buffer.setString(x, y, extensionInfo.name(), HEADER_STYLE);
            y += 2;
            buffer.setString(x, y, "Namespace: " + extensionInfo.namespace(), DIM_STYLE);
            y += 1;
            buffer.setString(x, y, extensionInfo.active() ? "Active" : "Inactive",
                    extensionInfo.active() ? Style.EMPTY.green() : DIM_STYLE);
            if (extensionInfo.description() != null && !extensionInfo.description().isEmpty()) {
                y += 2;
                buffer.setString(x, y, truncate(extensionInfo.description(), maxWidth), VALUE_STYLE);
            }
            return;
        }

        int row = y - scrollOffset;
        for (Section section : sections) {
            if (row >= y && row < area.y() + area.height() - 1) {
                buffer.setString(x, row, section.title, HEADER_STYLE);
            }
            row++;

            for (Item item : section.items) {
                if (row >= y && row < area.y() + area.height() - 1) {
                    if (item.label.isEmpty()) {
                        buffer.setString(x + 1, row, truncate(item.value, maxWidth - 1), VALUE_STYLE);
                    } else {
                        String label = item.label + ": ";
                        buffer.setString(x + 1, row, label, LABEL_STYLE);
                        buffer.setString(x + 1 + label.length(), row,
                                truncate(item.value, maxWidth - label.length() - 1), VALUE_STYLE);
                    }
                }
                row++;
            }
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

    private static class Section {
        final String title;
        final List<Item> items = new ArrayList<>();

        Section(String title) {
            this.title = title;
        }
    }

    private record Item(String label, String value) {
    }
}
