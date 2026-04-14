package io.quarkus.devshell.runtime.tui.pages;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.ShellExtension;

/**
 * Generic fallback page for extensions without custom shell pages.
 * Shows basic extension information.
 */
public class GenericExtensionPage extends BaseExtensionPage {

    public GenericExtensionPage(ShellExtension extension) {
        super(extension);
    }

    @Override
    public void loadData() {
        // Nothing to load for generic page
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        int row = 3;

        // Name
        buffer.setString(2, row, "Name: ", Style.create().cyan());
        buffer.setString(8, row, extension.getDisplayName(), Style.EMPTY);
        row++;

        // Namespace
        if (extension.namespace() != null && !extension.namespace().isEmpty()) {
            buffer.setString(2, row, "Namespace: ", Style.create().cyan());
            buffer.setString(13, row, extension.namespace(), Style.EMPTY);
            row++;
        }

        // Status
        buffer.setString(2, row, "Status: ", Style.create().cyan());
        if (extension.isActive()) {
            buffer.setString(10, row, "Active", Style.create().green());
        } else {
            buffer.setString(10, row, "Inactive", Style.create().gray());
        }
        row += 2;

        // Description
        if (extension.description() != null && !extension.description().isEmpty()) {
            buffer.setString(2, row, "Description:", Style.create().cyan());
            row++;

            String desc = extension.description();
            int maxWidth = width - 6;
            while (!desc.isEmpty() && row < height - 5) {
                int lineLen = Math.min(desc.length(), maxWidth);
                if (lineLen < desc.length()) {
                    int lastSpace = desc.lastIndexOf(' ', lineLen);
                    if (lastSpace > 0) {
                        lineLen = lastSpace;
                    }
                }
                buffer.setString(4, row, desc.substring(0, lineLen), Style.EMPTY);
                desc = desc.substring(lineLen).trim();
                row++;
            }
        }

        // Note about shell page
        buffer.setString(2, height - 4, "No custom shell page registered for this extension.", Style.create().gray());

        // Footer
        renderFooter(buffer, "");
    }

    @Override
    protected void renderPanelContent(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight) {
        int row = startRow;

        if (extension.isActive()) {
            buffer.setString(startCol, row, "Active", Style.create().green());
        } else {
            buffer.setString(startCol, row, "Inactive", Style.create().gray());
        }
        row++;

        if (extension.description() != null && !extension.description().isEmpty()) {
            String desc = extension.description();
            buffer.setString(startCol, row, truncate(desc, panelWidth - 2), Style.EMPTY);
        }
    }
}
