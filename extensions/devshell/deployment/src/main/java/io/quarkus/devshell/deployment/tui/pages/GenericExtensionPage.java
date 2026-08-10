package io.quarkus.devshell.deployment.tui.pages;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import io.quarkus.devshell.deployment.tui.screens.ExtensionsListScreen;

public class GenericExtensionPage extends BaseExtensionPage {

    private final ExtensionsListScreen.ExtensionInfo extensionInfo;

    public GenericExtensionPage(ExtensionsListScreen.ExtensionInfo ext) {
        super(ext.namespace(), ext.name());
        this.extensionInfo = ext;
    }

    @Override
    public void loadData() {
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        int x = area.x() + 2;
        int y = area.y() + 1;

        buffer.setString(x, y, extensionInfo.name(), HEADER_STYLE);
        y += 2;

        buffer.setString(x, y, "Namespace:", LABEL_STYLE);
        buffer.setString(x + 11, y, extensionInfo.namespace(), DIM_STYLE);
        y += 1;

        buffer.setString(x, y, "Status:", LABEL_STYLE);
        buffer.setString(x + 11, y, extensionInfo.active() ? "Active" : "Inactive",
                extensionInfo.active() ? Style.EMPTY.green() : Style.EMPTY.gray());
        y += 2;

        if (extensionInfo.description() != null && !extensionInfo.description().isEmpty()) {
            buffer.setString(x, y, "Description:", LABEL_STYLE);
            y += 1;
            String desc = truncate(extensionInfo.description(), area.width() - x - 2);
            buffer.setString(x, y, desc, VALUE_STYLE);
            y += 2;
        }

        if (extensionInfo.keywords() != null && !extensionInfo.keywords().isEmpty()) {
            buffer.setString(x, y, "Keywords:", LABEL_STYLE);
            y += 1;
            buffer.setString(x, y, truncate(String.join(", ", extensionInfo.keywords()), area.width() - x - 2),
                    DIM_STYLE);
            y += 2;
        }

        buffer.setString(x, y, "No custom shell page available for this extension.", DIM_STYLE);
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        return false;
    }
}
