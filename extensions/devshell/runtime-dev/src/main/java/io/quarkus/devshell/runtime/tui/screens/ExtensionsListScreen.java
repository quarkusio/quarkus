package io.quarkus.devshell.runtime.tui.screens;

import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.AnsiRenderer;
import io.quarkus.devshell.runtime.tui.AppContext;
import io.quarkus.devshell.runtime.tui.BufferHelper;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.Screen;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.ExtensionPage;
import io.quarkus.devshell.runtime.tui.pages.ExtensionPageFactory;
import io.quarkus.devshell.runtime.tui.widgets.ListView;

/**
 * Screen showing list of extensions with details panel.
 * Extensions are ordered: active first, then inactive.
 */
public class ExtensionsListScreen implements Screen {

    private AppContext ctx;
    private final List<ShellExtension> extensions;
    private final ListView<ShellExtension> extensionList;

    private int leftPanelWidth = 30;
    private int rightPanelStart = 32;

    public ExtensionsListScreen(List<ShellExtension> extensions) {
        this.extensions = extensions;
        this.extensionList = new ListView<>(ext -> {
            String prefix = ext.isActive() ? "● " : "○ ";
            return prefix + ext.getDisplayName();
        });
        this.extensionList.setItems(extensions);
    }

    @Override
    public String getTitle() {
        return "Extensions";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        int width = ctx.getWidth();
        leftPanelWidth = Math.max(25, width / 3);
        rightPanelStart = leftPanelWidth + 2;
        extensionList.setWidth(leftPanelWidth - 2);
        extensionList.setVisibleRows(ctx.getHeight() - 6);
    }

    @Override
    public void onLeave() {
        // Nothing to clean up
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        int width = this.ctx.getWidth();
        int height = this.ctx.getHeight();

        // Header
        renderHeader(buffer, width);

        // Left panel: Extensions list
        renderLeftPanel(buffer, height);

        // Right panel: Extension details
        renderRightPanel(buffer, width, height);

        // Divider
        renderDivider(buffer, height);

        // Footer
        renderFooter(buffer, height);
    }

    private void renderHeader(Buffer buffer, int width) {
        BufferHelper.writeHeader(buffer, " EXTENSIONS ", width);

        long activeCount = extensions.stream().filter(ShellExtension::isActive).count();
        buffer.setString(0, 1, " " + activeCount + " active, " + (extensions.size() - activeCount) + " inactive",
                Style.create().gray());
    }

    private void renderLeftPanel(Buffer buffer, int height) {
        buffer.setString(0, 3, " Extensions", Style.create().cyan().bold());
        buffer.setString(1, 4, "\u2500".repeat(leftPanelWidth - 2), Style.create().gray());

        extensionList.render(buffer, 6, 1);
    }

    private void renderRightPanel(Buffer buffer, int width, int height) {
        ShellExtension selected = extensionList.getSelectedItem();
        if (selected == null) {
            return;
        }

        int panelWidth = width - rightPanelStart - 1;
        int row = 4;

        // Extension name header
        buffer.setString(rightPanelStart, row, selected.getDisplayName(), Style.create().cyan().bold());
        row++;

        // Divider
        buffer.setString(rightPanelStart, row, "\u2550".repeat(Math.min(selected.getDisplayName().length(), panelWidth)),
                Style.create().gray());
        row += 2;

        // Namespace
        int x = rightPanelStart;
        x += buffer.setString(x, row, "Namespace: ", Style.create().cyan());
        buffer.setString(x, row, truncate(selected.namespace(), panelWidth - 12), Style.EMPTY);
        row += 2;

        // Description
        if (selected.description() != null && !selected.description().isEmpty()) {
            row++;

            String desc = selected.description();
            int maxDescWidth = panelWidth - 2;
            List<String> lines = AnsiRenderer.wrapText(desc, maxDescWidth);
            for (int i = 0; i < Math.min(lines.size(), 3); i++) {
                buffer.setString(rightPanelStart, row, lines.get(i), Style.create().white());
                row++;
            }
            if (lines.size() > 3) {
                buffer.setString(rightPanelStart, row, "...", Style.create().gray());
                row++;
            }
            row++;
        }

        // Show instruction to view extension
        if (selected.isActive()) {
            buffer.setString(rightPanelStart, row, "Press Enter to view details", Style.create().gray());
        }
    }

    private void renderDivider(Buffer buffer, int height) {
        int col = leftPanelWidth;
        for (int row = 4; row < height - 1; row++) {
            buffer.setString(col, row, "\u2502", Style.create().gray());
        }
    }

    private void renderFooter(Buffer buffer, int height) {
        buffer.setString(1, height - 2, "", Style.create().gray());
    }

    @Override
    public boolean handleKey(int key) {
        // Let list handle navigation
        if (extensionList.handleKey(key)) {
            this.ctx.requestRedraw();
            return true;
        }

        switch (key) {
            case KeyCode.ENTER:
                ShellExtension selected = extensionList.getSelectedItem();
                if (selected != null && selected.isActive()) {
                    // Use the factory to create the appropriate page for this extension
                    ExtensionPage page = ExtensionPageFactory.createPage(selected, this.ctx.getShellPages());
                    this.ctx.navigateTo(page);
                } else if (selected != null && !selected.isActive()) {
                    this.ctx.setStatus("This extension is inactive");
                }
                return true;

            case KeyCode.ESCAPE:
                this.ctx.goBack();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onResize(int width, int height) {
        leftPanelWidth = Math.max(25, width / 3);
        rightPanelStart = leftPanelWidth + 2;
        extensionList.setWidth(leftPanelWidth - 2);
        extensionList.setVisibleRows(height - 6);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 1) + "…";
    }

}
