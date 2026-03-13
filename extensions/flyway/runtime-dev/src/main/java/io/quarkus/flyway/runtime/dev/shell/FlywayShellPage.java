package io.quarkus.flyway.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.runtime.tui.widgets.ListView;
import io.quarkus.flyway.runtime.dev.ui.FlywayJsonRpcService;
import io.quarkus.flyway.runtime.dev.ui.FlywayJsonRpcService.FlywayActionResponse;

/**
 * Shell page for Flyway extension.
 * Displays datasources and migration status.
 */
public class FlywayShellPage extends BaseExtensionPage {

    private static final int TAB_INFO = 0;
    private static final int TAB_DATASOURCES = 1;

    private ListView<DatasourceInfo> datasourceList;

    // Parsed data
    private List<DatasourceInfo> datasources = new ArrayList<>();
    private boolean cleanDisabled = true;
    private String lastActionResult = null;

    @Inject
    FlywayJsonRpcService flywayService;

    // No-arg constructor for CDI
    public FlywayShellPage() {
        setTabs("Info", "Datasources");
        setTabArrowNavigation(true);
        this.datasourceList = new ListView<>(ds -> {
            String statusIcon = ds.hasMigrations ? "[OK]" : "[--]";
            return statusIcon + " " + ds.name +
                    (ds.createPossible ? " (can create)" : "");
        });
    }

    public FlywayShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        datasources.clear();
        lastActionResult = null;
        redraw();

        try {
            var flywayDatasources = flywayService.getDatasources();
            if (flywayDatasources != null) {
                for (var fd : flywayDatasources) {
                    datasources.add(new DatasourceInfo(fd.name, fd.hasMigrations, fd.createPossible));
                }
            }
            datasourceList.setItems(datasources);
            cleanDisabled = flywayService.isCleanDisabled();
            loading = false;
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load Flyway datasources: " + ex.getMessage();
            redraw();
        }
    }

    private void runMigrate(String datasourceName) {
        lastActionResult = null;
        loading = true;
        redraw();

        try {
            FlywayActionResponse result = flywayService.migrate(datasourceName);
            loading = false;
            formatActionResult(result, "Migrate");
            loadData();
        } catch (Exception ex) {
            loading = false;
            lastActionResult = "Migration failed: " + ex.getMessage();
            redraw();
        }
    }

    private void runClean(String datasourceName) {
        if (cleanDisabled) {
            lastActionResult = "Clean is disabled in configuration";
            redraw();
            return;
        }

        lastActionResult = null;
        loading = true;
        redraw();

        try {
            FlywayActionResponse result = flywayService.clean(datasourceName);
            loading = false;
            formatActionResult(result, "Clean");
            loadData();
        } catch (Exception ex) {
            loading = false;
            lastActionResult = "Clean failed: " + ex.getMessage();
            redraw();
        }
    }

    private void formatActionResult(FlywayActionResponse result, String action) {
        if (result == null) {
            lastActionResult = action + " failed: no response";
            return;
        }
        if ("success".equals(result.type)) {
            lastActionResult = action + ": " + result.message;
        } else if ("warning".equals(result.type)) {
            lastActionResult = action + ": " + result.message;
        } else {
            lastActionResult = action + " failed: " + result.message;
        }
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        int row = renderTabBar(buffer, 3);

        if (loading) {
            renderLoading(buffer, row);
        } else if (error != null) {
            renderError(buffer, row);
        } else {
            switch (getCurrentTabIndex()) {
                case TAB_INFO:
                    renderInfoTab(buffer, row);
                    break;
                case TAB_DATASOURCES:
                    renderDatasourcesTab(buffer, row);
                    break;
            }
        }

        // Show action result if any
        if (lastActionResult != null && !loading) {
            buffer.setString(1, height - 4, lastActionResult, Style.create().green());
        }

        renderFooter(buffer, "[M] Migrate  [C] Clean");
    }

    private void renderInfoTab(Buffer buffer, int row) {
        // Count by status
        int withMigrations = 0, canCreate = 0;
        for (DatasourceInfo ds : datasources) {
            if (ds.hasMigrations) {
                withMigrations++;
            }
            if (ds.createPossible) {
                canCreate++;
            }
        }

        KeyValuePanel summary = new KeyValuePanel("Flyway Database Migrations");
        summary.add("Datasources", String.valueOf(datasources.size()));
        summary.addStyled("With migrations", String.valueOf(withMigrations), Style.create().green());
        if (canCreate > 0) {
            summary.addStyled("Can create initial", String.valueOf(canCreate), Style.create().yellow());
        }
        summary.addBlank();
        summary.addStyled("Clean", cleanDisabled ? "Disabled" : "Enabled",
                cleanDisabled ? Style.create().red() : Style.create().green());
        row = summary.render(buffer, row, 2, width - 4);

        row += 2;

        // Legend
        KeyValuePanel actions = new KeyValuePanel("Actions");
        actions.add("[M] Migrate", "Run pending migrations");
        actions.add("[C] Clean", "Drop all database objects" + (cleanDisabled ? " (disabled)" : ""));
        actions.render(buffer, row, 2, width - 4);
    }

    private void renderDatasourcesTab(Buffer buffer, int row) {
        row++;

        if (datasources.isEmpty()) {
            buffer.setString(1, row, "No datasources found", Style.create().gray());
        } else {
            datasourceList.setVisibleRows(height - row - 6);
            datasourceList.setWidth(width - 4);
            datasourceList.render(buffer, row, 2);
        }
    }

    @Override
    public boolean handleKey(int key) {
        switch (key) {
            case 'm':
            case 'M':
                // Migrate selected datasource
                if (!datasources.isEmpty()) {
                    int idx = datasourceList.getSelectedIndex();
                    if (idx >= 0 && idx < datasources.size()) {
                        runMigrate(datasources.get(idx).name);
                    } else if (!datasources.isEmpty()) {
                        runMigrate(datasources.get(0).name);
                    }
                }
                return true;
            case 'c':
            case 'C':
                // Clean selected datasource
                if (!datasources.isEmpty()) {
                    int idx = datasourceList.getSelectedIndex();
                    if (idx >= 0 && idx < datasources.size()) {
                        runClean(datasources.get(idx).name);
                    } else if (!datasources.isEmpty()) {
                        runClean(datasources.get(0).name);
                    }
                }
                return true;
        }

        // Let list handle navigation when on datasources tab
        if (getCurrentTabIndex() == TAB_DATASOURCES && datasourceList.handleKey(key)) {
            redraw();
            return true;
        }

        return super.handleKey(key);
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        datasourceList.setVisibleRows(height - 10);
        datasourceList.setWidth(width - 4);
    }

    // Data class
    private static class DatasourceInfo {
        final String name;
        final boolean hasMigrations;
        final boolean createPossible;

        DatasourceInfo(String name, boolean hasMigrations, boolean createPossible) {
            this.name = name;
            this.hasMigrations = hasMigrations;
            this.createPossible = createPossible;
        }
    }
}
