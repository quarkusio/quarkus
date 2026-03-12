package io.quarkus.liquibase.runtime.dev.shell;

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
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.liquibase.runtime.dev.ui.LiquibaseJsonRpcService;

/**
 * Shell page for Liquibase extension.
 * Displays datasources and allows running migrations.
 */
public class LiquibaseShellPage extends BaseExtensionPage {

    @Inject
    LiquibaseJsonRpcService liquibaseService;

    private static final int TAB_INFO = 0;
    private static final int TAB_DATASOURCES = 1;

    private ListView<LiquibaseDatasource> datasourceList;

    // Parsed data
    private List<LiquibaseDatasource> datasources = new ArrayList<>();
    private String lastActionResult = null;

    // No-arg constructor for CDI
    public LiquibaseShellPage() {
        setTabs("Info", "Datasources");
        setTabArrowNavigation(true);
        this.datasourceList = new ListView<>(ds -> {
            return ds.name +
                    (ds.changeLog != null ? " (" + ds.changeLog + ")" : "");
        });
    }

    public LiquibaseShellPage(ShellExtension extension) {
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
            var factories = liquibaseService.getLiquibaseFactories();
            loading = false;

            if (factories != null) {
                for (LiquibaseFactory lf : factories) {
                    String name = lf.getDataSourceName();
                    if (name != null) {
                        datasources.add(new LiquibaseDatasource(name, null));
                    }
                }
            }

            datasourceList.setItems(datasources);
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load Liquibase datasources: " + ex.getMessage();
            redraw();
        }
    }

    private void runMigrate(String datasourceName) {
        lastActionResult = null;
        loading = true;
        redraw();

        try {
            boolean result = liquibaseService.migrate(datasourceName);
            loading = false;
            if (result) {
                lastActionResult = "Migration completed successfully";
            } else {
                lastActionResult = "Migration failed";
            }
            redraw();
        } catch (Exception ex) {
            loading = false;
            lastActionResult = "Migration failed: " + ex.getMessage();
            redraw();
        }
    }

    private void runClear(String datasourceName) {
        lastActionResult = null;
        loading = true;
        redraw();

        try {
            boolean result = liquibaseService.clear(datasourceName);
            loading = false;
            if (result) {
                lastActionResult = "Database cleared successfully";
            } else {
                lastActionResult = "Clear failed";
            }
            redraw();
        } catch (Exception ex) {
            loading = false;
            lastActionResult = "Clear failed: " + ex.getMessage();
            redraw();
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

        renderFooter(buffer, "[M] Migrate  [C] Clear");
    }

    private void renderInfoTab(Buffer buffer, int row) {
        KeyValuePanel summary = new KeyValuePanel("Liquibase Database Migrations");
        summary.add("Datasources", String.valueOf(datasources.size()));
        row = summary.render(buffer, row, 2, width - 4);
        row++;

        // Actions legend
        KeyValuePanel actions = new KeyValuePanel("Actions");
        actions.add("[M] Migrate", "Run pending changesets");
        actions.add("[C] Clear", "Drop all database objects");
        row = actions.render(buffer, row, 2, width - 4);
        row++;

        // Show datasource list summary
        if (!datasources.isEmpty()) {
            row++;
            for (int i = 0; i < Math.min(datasources.size(), 5); i++) {
                buffer.setString(3, row, truncate(datasources.get(i).name, width - 8), Style.EMPTY);
                row++;
            }
            if (datasources.size() > 5) {
                buffer.setString(3, row, "+" + (datasources.size() - 5) + " more...", Style.create().gray());
            }
        }
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
                    } else {
                        runMigrate(datasources.get(0).name);
                    }
                }
                return true;
            case 'c':
            case 'C':
                // Clear selected datasource
                if (!datasources.isEmpty()) {
                    int idx = datasourceList.getSelectedIndex();
                    if (idx >= 0 && idx < datasources.size()) {
                        runClear(datasources.get(idx).name);
                    } else {
                        runClear(datasources.get(0).name);
                    }
                }
                return true;
        }

        // Let list handle navigation on the Datasources tab
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
    private static class LiquibaseDatasource {
        final String name;
        final String changeLog;

        LiquibaseDatasource(String name, String changeLog) {
            this.name = name;
            this.changeLog = changeLog;
        }
    }
}
