package io.quarkus.devshell.runtime.tui.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.spi.ShellPageData;
import io.quarkus.devshell.runtime.spi.ShellPageProvider;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.widgets.ListView;

/**
 * Generic page that renders content from a ShellPageProvider.
 * This allows extensions to define their shell page content
 * without knowing about TUI rendering details.
 */
public class ProviderBasedPage extends BaseExtensionPage {

    private final ShellPageProvider provider;
    private ShellPageData data;
    private final ListView<ActionDisplay> actionList;
    private List<ActionDisplay> actions = new ArrayList<>();
    private int sectionScrollOffset = 0;

    public ProviderBasedPage(ShellExtension extension, ShellPageProvider provider) {
        super(extension);
        this.provider = provider;
        this.actionList = new ListView<>(a -> a.label);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        data = null;
        redraw();

        // Load data synchronously (provider should be fast)
        // If providers need async loading, they should cache their data
        try {
            data = provider.loadData();
            if (data != null && data.hasError()) {
                error = data.getError();
            }

            // Update action list
            actions = new ArrayList<>();
            for (ShellPageProvider.PageAction action : provider.getActions()) {
                actions.add(new ActionDisplay(action.name(), action.label(), action.shortcutKey()));
            }
            actionList.setItems(actions);
        } catch (Exception e) {
            error = "Failed to load data: " + e.getMessage();
        } finally {
            loading = false;
            redraw();
        }
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        int row = 3;

        if (loading) {
            renderLoading(buffer, row);
            renderFooter(buffer, "");
            return;
        }

        if (error != null) {
            renderError(buffer, row);
            renderFooter(buffer, "");
            return;
        }

        if (data == null || data.isEmpty()) {
            renderFooter(buffer, "");
            return;
        }

        // Render sections
        int maxRow = height - 4;
        int currentRow = row;

        List<ShellPageData.Section> sections = data.getSections();
        for (int s = sectionScrollOffset; s < sections.size() && currentRow < maxRow; s++) {
            ShellPageData.Section section = sections.get(s);

            // Section header
            buffer.setString(2, currentRow, section.getTitle(), Style.create().cyan().bold());
            currentRow++;

            // Separator line
            buffer.setString(2, currentRow, "\u2500".repeat(Math.max(0, width - 4)), Style.create().gray());
            currentRow++;

            // Section items
            for (ShellPageData.Item item : section.getItems()) {
                if (currentRow >= maxRow) {
                    break;
                }
                renderItem(buffer, item, currentRow, width - 6);
                currentRow++;
            }

            currentRow++; // Space between sections
        }

        // Scroll indicator
        if (sectionScrollOffset > 0 || sections.size() > 3) {
            buffer.setString(width - 15, height - 5, "[PgUp/PgDn]", Style.create().gray());
        }

        // Footer with actions
        StringBuilder footerText = new StringBuilder();
        for (ActionDisplay action : actions) {
            if (!footerText.isEmpty()) {
                footerText.append("  ");
            }
            footerText.append("[").append(action.shortcutKey).append("] ").append(action.label);
        }
        renderFooter(buffer, footerText.toString());
    }

    private void renderItem(Buffer buffer, ShellPageData.Item item, int row, int maxWidth) {
        String label = item.getLabel();
        String value = item.getValue();
        ShellPageProvider.ItemStyle style = item.getStyle();

        switch (style) {
            case HEADER:
                buffer.setString(4, row, label, Style.create().bold());
                break;

            case STATUS_OK:
                buffer.setString(4, row, label + ": ", Style.create().gray());
                buffer.setString(4 + label.length() + 2, row, value != null ? value : "OK", Style.create().green());
                break;

            case STATUS_WARNING:
                buffer.setString(4, row, label + ": ", Style.create().gray());
                buffer.setString(4 + label.length() + 2, row, value != null ? value : "Warning", Style.create().yellow());
                break;

            case STATUS_ERROR:
                buffer.setString(4, row, label + ": ", Style.create().gray());
                buffer.setString(4 + label.length() + 2, row, value != null ? value : "Error", Style.create().red());
                break;

            case CODE:
                buffer.setString(4, row, label, Style.create().cyan());
                break;

            case LINK:
                buffer.setString(4, row, label + ": ", Style.create().gray());
                buffer.setString(4 + label.length() + 2, row, value != null ? value : "", Style.create().blue());
                break;

            case TEXT:
            default:
                if (value != null && !value.isEmpty()) {
                    buffer.setString(4, row, label + ": ", Style.create().gray());
                    buffer.setString(4 + label.length() + 2, row, value, Style.EMPTY);
                } else {
                    buffer.setString(4, row, label, Style.EMPTY);
                }
                break;
        }
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        switch (key) {
            case KeyCode.PAGE_UP:
                sectionScrollOffset = Math.max(0, sectionScrollOffset - 1);
                redraw();
                return true;

            case KeyCode.PAGE_DOWN:
                if (data != null) {
                    sectionScrollOffset = Math.min(sectionScrollOffset + 1, Math.max(0, data.getSections().size() - 2));
                    redraw();
                }
                return true;

            default:
                // Check if it's an action shortcut
                for (ActionDisplay action : actions) {
                    if (Character.toLowerCase(key) == Character.toLowerCase(action.shortcutKey)) {
                        executeAction(action.name);
                        return true;
                    }
                }
                return super.handleKey(key);
        }
    }

    private void executeAction(String actionName) {
        loading = true;
        ctx.setStatus("Executing " + actionName + "...");
        redraw();

        try {
            String result = provider.executeAction(actionName, Map.of());
            if (result != null) {
                ctx.setStatus(result);
            }
            // Reload data after action
            loadData();
        } catch (Exception e) {
            error = "Action failed: " + e.getMessage();
            loading = false;
            redraw();
        }
    }

    @Override
    protected void renderPanelContent(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight) {
        int row = startRow;

        if (loading) {
            return;
        }

        if (error != null) {
            return;
        }

        if (data == null || data.isEmpty()) {
            return;
        }

        // Render first section only in panel mode
        ShellPageData.Section section = data.getSections().get(0);
        buffer.setString(startCol, row, section.getTitle(), Style.create().cyan().bold());
        row++;

        int maxItems = Math.min(section.getItems().size(), panelHeight - 3);
        for (int i = 0; i < maxItems; i++) {
            ShellPageData.Item item = section.getItems().get(i);
            renderPanelItem(buffer, item, startCol, row, panelWidth - 2);
            row++;
        }

        if (section.getItems().size() > maxItems) {
            buffer.setString(startCol, row, "+" + (section.getItems().size() - maxItems) + " more...",
                    Style.create().gray());
        }
    }

    private void renderPanelItem(Buffer buffer, ShellPageData.Item item, int col, int row, int maxWidth) {
        String label = item.getLabel();
        String value = item.getValue();

        if (value == null || value.isEmpty()) {
            buffer.setString(col, row, truncate(label, maxWidth), Style.EMPTY);
        } else {
            String prefix = label + ": ";
            buffer.setString(col, row, prefix, Style.create().gray());
            buffer.setString(col + prefix.length(), row, truncate(value, maxWidth - prefix.length()), Style.EMPTY);
        }
    }

    private record ActionDisplay(String name, String label, char shortcutKey) {
    }
}
