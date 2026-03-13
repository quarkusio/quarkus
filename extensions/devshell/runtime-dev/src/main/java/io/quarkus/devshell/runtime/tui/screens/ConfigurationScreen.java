package io.quarkus.devshell.runtime.tui.screens;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.AppContext;
import io.quarkus.devshell.runtime.tui.BufferHelper;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.Screen;
import io.quarkus.devshell.runtime.tui.widgets.TableView;
import io.quarkus.devui.runtime.config.ConfigDescriptionBean;
import io.quarkus.vertx.http.runtime.devmode.ConfigDescription;

/**
 * Screen for viewing and searching Quarkus configuration properties.
 */
public class ConfigurationScreen implements Screen {

    private AppContext ctx;
    private final List<ConfigItem> allConfigs = new ArrayList<>();
    private final List<ConfigItem> filteredConfigs = new ArrayList<>();
    private final TableView<ConfigItem> configTable;

    private boolean loading = false;
    private String error = null;
    private String filterText = "";
    private boolean filterMode = false;
    private StringBuilder filterInput = new StringBuilder();

    // Filter by config phase
    private enum PhaseFilter {
        ALL("All"),
        BUILD_TIME("Build Time"),
        BUILD_AND_RUN_TIME("Build & Run Time"),
        RUN_TIME("Run Time");

        final String label;

        PhaseFilter(String label) {
            this.label = label;
        }
    }

    private PhaseFilter phaseFilter = PhaseFilter.ALL;

    @Inject
    ConfigDescriptionBean configBean;

    public ConfigurationScreen() {
        configTable = new TableView<>();
        configTable.addColumn("Phase", this::formatPhase, 6);
        configTable.addColumn("Name", c -> c.name, 50);
        configTable.addColumn("Value", c -> formatValue(c), 30);
    }

    private String formatPhase(ConfigItem config) {
        if (config.configPhase == null) {
            return " ";
        }
        switch (config.configPhase) {
            case "BUILD_TIME":
                return "\u25CF"; // Build time
            case "BUILD_AND_RUN_TIME_FIXED":
                return "\u25CF"; // Build & run time
            case "RUN_TIME":
                return "\u25CF"; // Runtime
            default:
                return " ";
        }
    }

    private String formatValue(ConfigItem config) {
        String value = config.value;
        if (value == null || value.isEmpty()) {
            if (config.defaultValue != null && !config.defaultValue.isEmpty()) {
                return config.defaultValue + " (default)";
            }
            return "(not set)";
        }
        // Truncate long values
        if (value.length() > 40) {
            return value.substring(0, 37) + "...";
        }
        return value;
    }

    @Override
    public String getTitle() {
        return "Configuration";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        loadConfigurations();
    }

    @Override
    public void onLeave() {
        // Nothing to clean up
    }

    private void loadConfigurations() {
        loading = true;
        error = null;
        this.ctx.requestRedraw();

        try {
            loading = false;
            allConfigs.clear();
            for (ConfigDescription cd : configBean.getAllConfig()) {
                String name = cd.getName();
                if (name != null) {
                    String value = null;
                    String sourceName = null;
                    if (cd.getConfigValue() != null) {
                        value = cd.getConfigValue().getValue();
                        sourceName = cd.getConfigValue().getSourceName();
                    }
                    allConfigs.add(new ConfigItem(name, cd.getDescription(), cd.getDefaultValue(),
                            value, cd.getConfigPhase(), sourceName));
                }
            }
            applyFilter();
            this.ctx.requestRedraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load configuration: " + ex.getMessage();
            this.ctx.requestRedraw();
        }
    }

    private void applyFilter() {
        filteredConfigs.clear();

        for (ConfigItem config : allConfigs) {
            // Apply text filter
            if (!filterText.isEmpty()) {
                String lowerFilter = filterText.toLowerCase();
                boolean matches = config.name.toLowerCase().contains(lowerFilter);
                if (config.value != null) {
                    matches = matches || config.value.toLowerCase().contains(lowerFilter);
                }
                if (config.description != null) {
                    matches = matches || config.description.toLowerCase().contains(lowerFilter);
                }
                if (!matches) {
                    continue;
                }
            }

            // Apply phase filter
            if (phaseFilter != PhaseFilter.ALL) {
                if (config.configPhase == null) {
                    continue;
                }
                switch (phaseFilter) {
                    case BUILD_TIME:
                        if (!config.configPhase.equals("BUILD_TIME")) {
                            continue;
                        }
                        break;
                    case BUILD_AND_RUN_TIME:
                        if (!config.configPhase.equals("BUILD_AND_RUN_TIME_FIXED")) {
                            continue;
                        }
                        break;
                    case RUN_TIME:
                        if (!config.configPhase.equals("RUN_TIME")) {
                            continue;
                        }
                        break;
                    default:
                        break;
                }
            }

            filteredConfigs.add(config);
        }

        configTable.setItems(filteredConfigs);
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        int width = this.ctx.getWidth();
        int height = this.ctx.getHeight();

        // Header
        renderHeader(buffer, width);

        int row = 3;

        // Filter bar
        renderFilterBar(buffer, row, width);
        row += 2;

        // Phase legend
        renderPhaseLegend(buffer, row);
        row += 2;

        // Separator
        row++;

        // Content
        if (loading) {
            buffer.setString(1, row, "Loading configuration...", Style.create().yellow());
        } else if (error != null) {
            buffer.setString(1, row, error, Style.create().red());
        } else if (filteredConfigs.isEmpty()) {
            if (allConfigs.isEmpty()) {
                buffer.setString(1, row, "No configuration properties found.", Style.create().gray());
            } else {
                buffer.setString(1, row, "No properties match the current filter.", Style.create().gray());
            }
        } else {
            int visibleRows = height - row - 4;
            configTable.setVisibleRows(visibleRows);
            configTable.setWidth(width - 4);
            configTable.render(buffer, row, 2);
        }

        // Footer
        if (filterMode) {
            buffer.setString(1, height - 2, "[Enter] Apply  [Esc] Cancel", Style.create().gray());
        } else {
            buffer.setString(1, height - 2, "[/] Filter  [Tab] Phase  [Enter] Details  [E] Edit  [R] Refresh  [Esc] Back",
                    Style.create().gray());
        }
    }

    private void renderHeader(Buffer buffer, int width) {
        BufferHelper.writeHeader(buffer, " Configuration ", width);
    }

    private void renderFilterBar(Buffer buffer, int row, int width) {

        if (filterMode) {
            String displayText = filterInput.toString();
            if (displayText.length() < 30) {
                displayText = displayText + " ".repeat(30 - displayText.length());
            }

            int x = 1;
            x += buffer.setString(x, row, "Filter: ", Style.create().yellow());
            x += buffer.setString(x, row, displayText, Style.create().reversed());
            buffer.setString(x, row, "_", Style.create().yellow());
        } else if (filterText.isEmpty()) {
            buffer.setString(1, row, "Press / to filter", Style.create().gray());
        } else {
            int x = 1;
            x += buffer.setString(x, row, "Filter: ", Style.create().cyan());
            buffer.setString(x, row, filterText, Style.EMPTY);
        }

        // Phase filter indicator
        buffer.setString(1, row + 1, "Phase: " + phaseFilter.label, Style.create().gray());
    }

    private void renderPhaseLegend(Buffer buffer, int row) {
        int x = 1;
        x += buffer.setString(x, row, "\u25CF", Style.create().red());
        x += buffer.setString(x, row, " Build Time  ", Style.EMPTY);
        x += buffer.setString(x, row, "\u25CF", Style.create().yellow());
        x += buffer.setString(x, row, " Build & Run Time  ", Style.EMPTY);
        x += buffer.setString(x, row, "\u25CF", Style.create().green());
        buffer.setString(x, row, " Run Time", Style.EMPTY);
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        if (filterMode) {
            return handleFilterKey(key);
        }

        switch (key) {
            case KeyCode.ESCAPE:
                if (!filterText.isEmpty()) {
                    // Clear filter first
                    filterText = "";
                    applyFilter();
                    this.ctx.requestRedraw();
                } else {
                    this.ctx.goBack();
                }
                return true;

            case '/':
                // Enter filter mode
                filterMode = true;
                filterInput.setLength(0);
                filterInput.append(filterText);
                this.ctx.requestRedraw();
                return true;

            case KeyCode.TAB:
                // Cycle through phase filters
                PhaseFilter[] phases = PhaseFilter.values();
                int current = phaseFilter.ordinal();
                phaseFilter = phases[(current + 1) % phases.length];
                applyFilter();
                this.ctx.requestRedraw();
                return true;

            case 'r':
            case 'R':
                loadConfigurations();
                return true;

            case 'e':
            case 'E':
                // Edit the selected config - go to detail screen in edit mode
                ConfigItem editItem = configTable.getSelectedItem();
                if (editItem != null) {
                    ConfigDetailScreen detailScreen = new ConfigDetailScreen(editItem);
                    detailScreen.startInEditMode();
                    this.ctx.navigateTo(detailScreen);
                }
                return true;

            case KeyCode.ENTER:
                ConfigItem selected = configTable.getSelectedItem();
                if (selected != null) {
                    this.ctx.navigateTo(new ConfigDetailScreen(selected));
                }
                return true;

            default:
                if (configTable.handleKey(key)) {
                    this.ctx.requestRedraw();
                    return true;
                }
                return false;
        }
    }

    private boolean handleFilterKey(int key) {
        switch (key) {
            case KeyCode.ESCAPE:
                // Cancel filter mode
                filterMode = false;
                this.ctx.requestRedraw();
                return true;

            case KeyCode.ENTER:
                // Apply filter
                filterMode = false;
                filterText = filterInput.toString();
                applyFilter();
                this.ctx.requestRedraw();
                return true;

            case KeyCode.BACKSPACE:
                if (filterInput.length() > 0) {
                    filterInput.deleteCharAt(filterInput.length() - 1);
                    this.ctx.requestRedraw();
                }
                return true;

            default:
                if (key >= 32 && key < 127) {
                    filterInput.append((char) key);
                    this.ctx.requestRedraw();
                    return true;
                }
                return false;
        }
    }

    @Override
    public void onResize(int width, int height) {
        configTable.setWidth(width - 4);
        configTable.setVisibleRows(height - 12);
    }

    // Data class for configuration items
    static class ConfigItem {
        final String name;
        final String description;
        final String defaultValue;
        final String value;
        final String configPhase;
        final String sourceName;

        ConfigItem(String name, String description, String defaultValue, String value, String configPhase,
                String sourceName) {
            this.name = name;
            this.description = description;
            this.defaultValue = defaultValue;
            this.value = value;
            this.configPhase = configPhase;
            this.sourceName = sourceName;
        }
    }
}
