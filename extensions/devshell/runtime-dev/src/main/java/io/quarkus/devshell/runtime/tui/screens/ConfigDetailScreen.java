package io.quarkus.devshell.runtime.tui.screens;

import java.util.List;
import java.util.Map;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devshell.runtime.tui.AnsiRenderer;
import io.quarkus.devshell.runtime.tui.AppContext;
import io.quarkus.devshell.runtime.tui.BufferHelper;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.Screen;

/**
 * Screen for viewing details of a single configuration property.
 */
public class ConfigDetailScreen implements Screen {

    private AppContext ctx;
    private final ConfigurationScreen.ConfigItem config;
    private int scrollOffset = 0;

    // Edit mode state
    private boolean editMode = false;
    private boolean deleteConfirmMode = false;
    private StringBuilder editValue = new StringBuilder();
    private boolean saving = false;
    private String saveError = null;
    private String saveSuccess = null;

    // Mutable value for display updates
    private String currentValue;

    public ConfigDetailScreen(ConfigurationScreen.ConfigItem config) {
        this.config = config;
        this.currentValue = config.value;
    }

    /**
     * Start the screen in edit mode.
     */
    public void startInEditMode() {
        editMode = true;
        editValue.setLength(0);
        if (currentValue != null) {
            editValue.append(currentValue);
        }
    }

    @Override
    public String getTitle() {
        return "Config: " + config.name;
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        scrollOffset = 0;
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

        int row = 3;
        int contentWidth = width - 4;

        // Property name
        buffer.setString(2, row, "Property:", Style.create().cyan());
        row++;
        buffer.setString(2, row, config.name, Style.create().white().bold());

        row += 2;

        // Current value
        buffer.setString(2, row, "Current Value:", Style.create().cyan());
        if (isEditable()) {
            buffer.setString(18, row, " (editable)", Style.create().gray());
        }
        row++;

        if (editMode) {
            // Show edit input
            String displayValue = editValue.toString();
            if (displayValue.length() < 40) {
                displayValue = displayValue + " ".repeat(40 - displayValue.length());
            }

            int x = 2;
            x += buffer.setString(x, row, "> ", Style.create().yellow());
            buffer.setString(x, row, displayValue, Style.create().reversed());
        } else if (currentValue != null && !currentValue.isEmpty()) {
            buffer.setString(2, row, currentValue, Style.create().green());
        } else {
            buffer.setString(2, row, "(not set)", Style.create().gray());
        }
        row += 2;

        // Show save status
        if (saveSuccess != null) {
            buffer.setString(2, row, saveSuccess, Style.create().green());
            row++;
        } else if (saveError != null) {
            buffer.setString(2, row, saveError, Style.create().red());
            row++;
        } else if (saving) {
            buffer.setString(2, row, "Saving...", Style.create().yellow());
            row++;
        }

        // Default value
        buffer.setString(2, row, "Default Value:", Style.create().cyan());
        row++;

        if (config.defaultValue != null && !config.defaultValue.isEmpty()) {
            buffer.setString(2, row, config.defaultValue, Style.EMPTY);
        } else {
            buffer.setString(2, row, "(none)", Style.create().gray());
        }
        row += 2;

        // Config phase
        buffer.setString(2, row, "Config Phase:", Style.create().cyan());
        row++;
        buffer.setString(2, row, formatPhase(config.configPhase), Style.EMPTY);

        row += 2;

        // Source
        buffer.setString(2, row, "Source:", Style.create().cyan());
        row++;

        if (config.sourceName != null && !config.sourceName.isEmpty()) {
            buffer.setString(2, row, config.sourceName, Style.EMPTY);
        } else {
            buffer.setString(2, row, "(unknown)", Style.create().gray());
        }
        row += 2;

        // Description
        if (config.description != null && !config.description.isEmpty()) {
            buffer.setString(2, row, "Description:", Style.create().cyan());
            row++;

            // Wrap description
            List<String> lines = AnsiRenderer.wrapText(config.description, contentWidth);
            int visibleLines = Math.min(lines.size(), height - row - 3);

            for (int i = 0; i < visibleLines; i++) {
                int lineIdx = i + scrollOffset;
                if (lineIdx < lines.size()) {
                    buffer.setString(2, row, lines.get(lineIdx), Style.EMPTY);
                    row++;
                }
            }

            // Scroll indicator
            if (lines.size() > visibleLines) {
                if (scrollOffset > 0) {
                    buffer.setString(width - 5, row - visibleLines, " \u25B2 ", Style.create().gray());
                }
                if (scrollOffset + visibleLines < lines.size()) {
                    buffer.setString(width - 5, row, " \u25BC ", Style.create().gray());
                }
            }
        }

        // Footer
        if (deleteConfirmMode) {
            buffer.setString(1, height - 2, "Delete this property? [Y]es / [N]o", Style.create().red());
        } else if (editMode) {
            buffer.setString(1, height - 2, "[Enter] Save  [Esc] Cancel", Style.create().gray());
        } else if (isEditable()) {
            buffer.setString(1, height - 2, "[e] Edit  [d] Delete  [Esc] Back  [Up/Down] Scroll", Style.create().gray());
        } else {
            buffer.setString(1, height - 2, "[Esc] Back  [Up/Down] Scroll", Style.create().gray());
        }
    }

    /**
     * Check if this configuration property is editable.
     * All properties can be edited - they are saved to application.properties.
     */
    private boolean isEditable() {
        return true;
    }

    /**
     * Get the appropriate message after saving based on config phase.
     */
    private String getSaveSuccessMessage() {
        if ("BUILD_TIME".equals(config.configPhase)) {
            return "Value saved. Rebuild required for changes to take effect.";
        } else if ("BUILD_AND_RUN_TIME_FIXED".equals(config.configPhase)) {
            return "Value saved. Restart required for changes to take effect.";
        } else {
            return "Value saved. Restart may be required.";
        }
    }

    private void renderHeader(Buffer buffer, int width) {
        BufferHelper.writeHeader(buffer, " Configuration Detail ", width);
    }

    private String formatPhase(String phase) {
        if (phase == null) {
            return "(unknown)";
        }
        switch (phase) {
            case "BUILD_TIME":
                return "\u25CF Build Time - Fixed at build time, requires rebuild to change";
            case "BUILD_AND_RUN_TIME_FIXED":
                return "\u25CF Build & Run Time Fixed - Set at build time, available at runtime";
            case "RUN_TIME":
                return "\u25CF Run Time - Can be changed at runtime";
            default:
                return phase;
        }
    }

    @Override
    public boolean handleKey(int key) {
        if (deleteConfirmMode) {
            return handleDeleteConfirmKey(key);
        }
        if (editMode) {
            return handleEditKey(key);
        }

        switch (key) {
            case KeyCode.ESCAPE:
                this.ctx.goBack();
                return true;

            case 'e':
            case 'E':
                if (isEditable()) {
                    enterEditMode();
                    this.ctx.requestRedraw();
                } else {
                    this.ctx.setStatus("This property cannot be edited (not a runtime property)");
                }
                return true;

            case 'd':
            case 'D':
                // Enter delete confirmation mode
                if (currentValue != null && !currentValue.isEmpty()) {
                    deleteConfirmMode = true;
                    saveError = null;
                    saveSuccess = null;
                    this.ctx.requestRedraw();
                } else {
                    this.ctx.setStatus("Property has no value to delete");
                }
                return true;

            case KeyCode.UP:
            case 'k':
                if (scrollOffset > 0) {
                    scrollOffset--;
                    this.ctx.requestRedraw();
                }
                return true;

            case KeyCode.DOWN:
            case 'j':
                scrollOffset++;
                this.ctx.requestRedraw();
                return true;

            default:
                return false;
        }
    }

    private boolean handleEditKey(int key) {
        switch (key) {
            case KeyCode.ESCAPE:
                // Cancel edit mode
                editMode = false;
                this.ctx.requestRedraw();
                return true;

            case KeyCode.ENTER:
                // Save the value
                saveValue();
                return true;

            case KeyCode.BACKSPACE:
                if (editValue.length() > 0) {
                    editValue.deleteCharAt(editValue.length() - 1);
                    this.ctx.requestRedraw();
                }
                return true;

            default:
                if (key >= 32 && key < 127) {
                    editValue.append((char) key);
                    this.ctx.requestRedraw();
                    return true;
                }
                return false;
        }
    }

    private boolean handleDeleteConfirmKey(int key) {
        switch (key) {
            case 'y':
            case 'Y':
                deleteProperty();
                return true;

            case 'n':
            case 'N':
            case KeyCode.ESCAPE:
                deleteConfirmMode = false;
                this.ctx.requestRedraw();
                return true;

            default:
                return true; // Consume all keys in confirm mode
        }
    }

    private void enterEditMode() {
        editMode = true;
        editValue.setLength(0);
        if (currentValue != null) {
            editValue.append(currentValue);
        }
        saveError = null;
        saveSuccess = null;
    }

    private void deleteProperty() {
        saving = true;
        deleteConfirmMode = false;
        saveError = null;
        saveSuccess = null;
        this.ctx.requestRedraw();

        try {
            DevConsoleManager.invoke("devui-configuration_removeProperty", Map.of(
                    "name", config.name,
                    "profile", "",
                    "target", "application.properties"));
            saving = false;
            currentValue = null;
            saveSuccess = "Property removed. " + getRestartMessage();
            this.ctx.requestRedraw();
        } catch (Exception ex) {
            saving = false;
            saveError = "Failed to remove: " + ex.getMessage();
            this.ctx.requestRedraw();
        }
    }

    private String getRestartMessage() {
        if ("BUILD_TIME".equals(config.configPhase)) {
            return "Rebuild required for changes to take effect.";
        } else if ("BUILD_AND_RUN_TIME_FIXED".equals(config.configPhase)) {
            return "Restart required for changes to take effect.";
        } else {
            return "Restart may be required.";
        }
    }

    private void saveValue() {
        String newValue = editValue.toString();

        // If the value is empty, prompt for deletion instead
        if (newValue.isEmpty()) {
            editMode = false;
            deleteConfirmMode = true;
            this.ctx.requestRedraw();
            return;
        }

        saving = true;
        saveError = null;
        saveSuccess = null;
        this.ctx.requestRedraw();

        try {
            DevConsoleManager.invoke("devui-configuration_updateProperty", Map.of(
                    "name", config.name,
                    "value", newValue,
                    "profile", "",
                    "target", "application.properties"));
            saving = false;
            editMode = false;
            currentValue = newValue;
            saveSuccess = getSaveSuccessMessage();
            this.ctx.requestRedraw();
        } catch (Exception ex) {
            saving = false;
            saveError = "Failed to save: " + ex.getMessage();
            this.ctx.requestRedraw();
        }
    }

    @Override
    public void onResize(int width, int height) {
        // Nothing to resize
    }
}
