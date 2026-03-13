package io.quarkus.agroal.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.agroal.runtime.dev.ui.DatabaseInspector;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.runtime.tui.widgets.ListView;

/**
 * Shell page for the Agroal extension showing datasources, tables, and table data.
 */
public class AgroalShellPage extends BaseExtensionPage {

    private enum ViewMode {
        TABLES,
        DATA,
        SQL_INPUT
    }

    private static final int PANEL_DATASOURCES = 0;
    private static final int PANEL_TABLES = 1;

    private ListView<DataSourceInfo> dataSourceList;
    private ListView<TableInfo> tableList;
    private ViewMode viewMode = ViewMode.TABLES;
    private int focusedPanel = PANEL_DATASOURCES;

    // Table data
    private List<String> columns = new ArrayList<>();
    private List<Map<String, String>> rows = new ArrayList<>();
    private int currentPage = 1;
    private int pageSize = 10;
    private int totalRows = 0;
    private String selectedTableName = null;

    // SQL input
    private StringBuilder sqlInput = new StringBuilder();
    private String lastExecutedSql = null;

    @Inject
    DatabaseInspector databaseInspector;

    // No-arg constructor for CDI
    public AgroalShellPage() {
        this.dataSourceList = new ListView<>(ds -> {
            String prefix = ds.isDefault ? "\u2605 " : "  ";
            return prefix + ds.name;
        });
        this.tableList = new ListView<>(table -> {
            return table.tableName + " (" + table.columnCount + " cols)";
        });
    }

    public AgroalShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        viewMode = ViewMode.TABLES;
        redraw();

        try {
            var datasources = databaseInspector.getDataSources();
            loading = false;
            List<DataSourceInfo> dsInfos = new ArrayList<>();
            for (var ds : datasources) {
                dsInfos.add(new DataSourceInfo(ds.name(), ds.jdbcUrl(), ds.isDefault()));
            }
            dataSourceList.setItems(dsInfos);
            redraw();

            // Auto-load tables for first/default datasource
            DataSourceInfo selected = dataSourceList.getSelectedItem();
            if (selected != null) {
                loadTablesForDataSource(selected.name);
            }
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load datasources: " + ex.getMessage();
            redraw();
        }
    }

    private void loadTablesForDataSource(String datasourceName) {
        loading = true;
        tableList.setItems(List.of());
        redraw();

        try {
            var tables = databaseInspector.getTables(datasourceName);
            loading = false;
            List<TableInfo> tableInfos = new ArrayList<>();
            for (var t : tables) {
                tableInfos.add(new TableInfo(t.tableSchema(), t.tableName(),
                        t.columns() != null ? t.columns().size() : 0));
            }
            tableList.setItems(tableInfos);
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load tables: " + ex.getMessage();
            redraw();
        }
    }

    private void loadTableData(String tableName, int page) {
        String sql = "SELECT * FROM " + tableName;
        selectedTableName = tableName;
        lastExecutedSql = sql;
        executeSQL(sql, page);
    }

    private void executeSQL(String sql, int page) {
        loading = true;
        currentPage = page;
        columns.clear();
        rows.clear();
        error = null;
        redraw();

        DataSourceInfo ds = dataSourceList.getSelectedItem();
        if (ds == null) {
            loading = false;
            error = "No datasource selected";
            redraw();
            return;
        }

        lastExecutedSql = sql;

        try {
            var result = databaseInspector.executeSQL(ds.name, sql, page, pageSize);
            loading = false;

            if (result.error() != null && !result.error().equals("null")) {
                error = result.error();
            } else {
                totalRows = result.totalNumberOfElements();
                if (result.cols() != null) {
                    columns.addAll(result.cols());
                }
                if (result.data() != null) {
                    rows.addAll(result.data());
                }
            }
            viewMode = ViewMode.DATA;
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to execute SQL: " + ex.getMessage();
            redraw();
        }
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        if (viewMode == ViewMode.SQL_INPUT) {
            renderSqlInputView(buffer);
        } else if (viewMode == ViewMode.DATA) {
            renderDataView(buffer);
        } else {
            renderTablesView(buffer);
        }

        // Error
        if (error != null) {
            renderError(buffer, height - 3);
        }
    }

    private void renderTablesView(Buffer buffer) {
        int row = 3;
        int leftPanelWidth = Math.max(25, width / 3);
        int rightPanelStart = leftPanelWidth + 2;

        // Left panel: Datasources
        Style dsHeaderStyle = focusedPanel == PANEL_DATASOURCES ? Style.create().cyan().bold().underlined()
                : Style.create().cyan().bold();
        buffer.setString(1, row - 1, "Datasources", dsHeaderStyle);
        row++;
        buffer.setString(1, row - 1, "─".repeat(leftPanelWidth - 2), Style.create().gray());
        row++;

        if (loading && dataSourceList.isEmpty()) {
            renderLoading(buffer, row);
        } else if (dataSourceList.isEmpty()) {
            buffer.setString(1, row - 1, "No datasources found", Style.create().gray());
        } else {
            dataSourceList.setVisibleRows(height - 8);
            dataSourceList.setWidth(leftPanelWidth - 3);
            dataSourceList.render(buffer, row, 2);
        }

        // Divider
        for (int r = 3; r < height - 2; r++) {
            buffer.setString(leftPanelWidth - 1, r - 1, "│", Style.create().gray());
        }

        // Right panel: Tables
        row = 3;
        Style tblHeaderStyle = focusedPanel == PANEL_TABLES ? Style.create().cyan().bold().underlined()
                : Style.create().cyan().bold();
        buffer.setString(rightPanelStart - 1, row - 1, "Tables", tblHeaderStyle);
        row++;
        buffer.setString(rightPanelStart - 1, row - 1, "─".repeat(width - rightPanelStart - 2), Style.create().gray());
        row++;

        if (loading && tableList.isEmpty()) {
            buffer.setString(rightPanelStart - 1, row - 1, "Loading tables...", Style.create().yellow());
        } else if (tableList.isEmpty()) {
            buffer.setString(rightPanelStart - 1, row - 1, "No tables found", Style.create().gray());
        } else {
            tableList.setVisibleRows(height - row - 3);
            tableList.setWidth(width - rightPanelStart - 2);
            tableList.render(buffer, row, rightPanelStart);
        }

        // Footer
        renderFooter(buffer,
                "[Tab] Switch panel  [Enter] " + (focusedPanel == PANEL_TABLES ? "View table data" : "Go to tables"));
    }

    private void renderDataView(Buffer buffer) {
        int row = 3;

        // Header with table name and pagination info
        buffer.setString(1, row - 1, "Table: " + selectedTableName, Style.create().cyan().bold());
        int totalPages = (totalRows + pageSize - 1) / pageSize;
        String pageInfo = "Page " + currentPage + "/" + Math.max(1, totalPages) + " (" + totalRows + " rows)";
        buffer.setString(width - pageInfo.length() - 2, row - 1, pageInfo, Style.create().gray());
        row++;

        buffer.setString(1, row - 1, "─".repeat(width - 4), Style.create().gray());
        row++;

        if (loading) {
            buffer.setString(1, row - 1, "Loading data...", Style.create().yellow());
        } else if (columns.isEmpty()) {
            buffer.setString(1, row - 1, "No data", Style.create().gray());
        } else {
            // Calculate column widths
            int availableWidth = width - 4;
            int numCols = columns.size();
            int colWidth = Math.max(10, availableWidth / numCols);

            // Render header row
            int x = 1;
            for (int i = 0; i < numCols && (i + 1) * colWidth < availableWidth; i++) {
                String colName = truncateColumn(columns.get(i), colWidth - 1);
                buffer.setString(x, row - 1, String.format("%-" + (colWidth - 1) + "s", colName), Style.create().cyan().bold());
                x += colWidth;
            }
            row++;

            // Render separator
            x = 1;
            for (int i = 0; i < numCols && (i + 1) * colWidth < availableWidth; i++) {
                buffer.setString(x, row - 1, "─".repeat(colWidth - 1), Style.create().gray());
                x += colWidth;
            }
            row++;

            // Render data rows
            int maxRows = height - row - 4;
            for (int r = 0; r < Math.min(rows.size(), maxRows); r++) {
                Map<String, String> rowData = rows.get(r);
                x = 1;
                for (int i = 0; i < numCols && (i + 1) * colWidth < availableWidth; i++) {
                    String value = rowData.getOrDefault(columns.get(i), "null");
                    value = truncateColumn(value, colWidth - 1);
                    buffer.setString(x, row - 1, String.format("%-" + (colWidth - 1) + "s", value), Style.EMPTY);
                    x += colWidth;
                }
                row++;
            }
        }

        // Footer
        renderFooter(buffer, "[</>] Page  [S] SQL query");
    }

    private void renderSqlInputView(Buffer buffer) {
        int row = 3;

        // Header
        buffer.setString(1, row - 1, "SQL Query", Style.create().cyan().bold());
        row++;

        buffer.setString(1, row - 1, "─".repeat(width - 4), Style.create().gray());
        row += 2;

        // Instructions
        buffer.setString(1, row - 1, "Enter your SQL query below. Press Enter to execute, Esc to cancel.",
                Style.create().gray());
        row += 2;

        // SQL input area
        buffer.setString(1, row - 1, "SQL> ", Style.create().cyan());
        buffer.setString(6, row - 1, sqlInput.toString(), Style.EMPTY);
        buffer.setString(6 + sqlInput.length(), row - 1, "_", Style.create().gray());
        row += 2;

        // Show last executed SQL if any
        if (lastExecutedSql != null && !lastExecutedSql.isEmpty()) {
            row++;
            buffer.setString(1, row - 1, "Last query: " + truncate(lastExecutedSql, width - 15), Style.create().gray());
        }

        // Footer
        renderFooter(buffer, "[Enter] Execute  [Esc] Cancel  [Backspace] Delete");
    }

    private String truncateColumn(String text, int maxLen) {
        if (text == null)
            return "null";
        if (text.length() <= maxLen)
            return text;
        return text.substring(0, maxLen - 1) + "~";
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        if (viewMode == ViewMode.SQL_INPUT) {
            return handleSqlInputKey(key);
        } else if (viewMode == ViewMode.DATA) {
            return handleDataViewKey(key);
        } else {
            return handleTablesViewKey(key);
        }
    }

    private boolean handleTablesViewKey(int key) {
        // Tab to switch between datasource and table list
        if (key == '\t' || key == KeyCode.TAB) {
            focusedPanel = (focusedPanel == PANEL_DATASOURCES) ? PANEL_TABLES : PANEL_DATASOURCES;
            redraw();
            return true;
        }

        if (focusedPanel == PANEL_DATASOURCES) {
            if (dataSourceList.handleKey(key)) {
                DataSourceInfo selected = dataSourceList.getSelectedItem();
                if (selected != null) {
                    loadTablesForDataSource(selected.name);
                }
                redraw();
                return true;
            }
            if (key == KeyCode.ENTER) {
                // Enter on datasource switches focus to tables
                focusedPanel = PANEL_TABLES;
                redraw();
                return true;
            }
        } else {
            if (tableList.handleKey(key)) {
                redraw();
                return true;
            }
            if (key == KeyCode.ENTER) {
                TableInfo selectedTable = tableList.getSelectedItem();
                if (selectedTable != null) {
                    loadTableData(selectedTable.tableName, 1);
                }
                return true;
            }
        }

        return super.handleKey(key);
    }

    private boolean handleDataViewKey(int key) {
        switch (key) {
            case KeyCode.ESCAPE:
                viewMode = ViewMode.TABLES;
                columns.clear();
                rows.clear();
                error = null;
                redraw();
                return true;

            case '<':
            case ',':
            case KeyCode.PAGE_UP:
                if (currentPage > 1 && lastExecutedSql != null) {
                    executeSQL(lastExecutedSql, currentPage - 1);
                }
                return true;

            case '>':
            case '.':
            case KeyCode.PAGE_DOWN:
                int totalPages = (totalRows + pageSize - 1) / pageSize;
                if (currentPage < totalPages && lastExecutedSql != null) {
                    executeSQL(lastExecutedSql, currentPage + 1);
                }
                return true;

            case 's':
            case 'S':
            case '/':
                // Enter SQL input mode
                sqlInput.setLength(0);
                error = null;
                viewMode = ViewMode.SQL_INPUT;
                redraw();
                return true;

            case 'r':
            case 'R':
                if (lastExecutedSql != null) {
                    executeSQL(lastExecutedSql, currentPage);
                }
                return true;

            default:
                return super.handleKey(key);
        }
    }

    private boolean handleSqlInputKey(int key) {
        switch (key) {
            case KeyCode.ESCAPE:
                // Cancel SQL input, go back to data view
                viewMode = ViewMode.DATA;
                sqlInput.setLength(0);
                redraw();
                return true;

            case KeyCode.ENTER:
                // Execute the SQL
                if (sqlInput.length() > 0) {
                    String sql = sqlInput.toString().trim();
                    sqlInput.setLength(0);
                    selectedTableName = "Custom Query";
                    executeSQL(sql, 1);
                }
                return true;

            case KeyCode.BACKSPACE:
                if (sqlInput.length() > 0) {
                    sqlInput.deleteCharAt(sqlInput.length() - 1);
                    redraw();
                }
                return true;

            default:
                // Add printable characters to the SQL input
                if (key >= 32 && key < 127) {
                    sqlInput.append((char) key);
                    redraw();
                    return true;
                }
                return false;
        }
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        dataSourceList.setVisibleRows(height - 8);
        dataSourceList.setWidth(Math.max(20, width / 3) - 3);
        tableList.setVisibleRows(height - 8);
        tableList.setWidth(width - Math.max(25, width / 3) - 4);
    }

    @Override
    protected void renderPanelContent(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight) {
        // Simplified panel content for split view
        int row = startRow;

        if (loading && dataSourceList.isEmpty()) {
            return;
        }

        if (error != null) {
            return;
        }

        // Datasource detail using KeyValuePanel
        if (!dataSourceList.isEmpty()) {
            DataSourceInfo ds = dataSourceList.getSelectedItem();
            if (ds != null) {
                KeyValuePanel dsPanel = new KeyValuePanel("Datasource");
                dsPanel.add("Name", ds.name);
                dsPanel.addIfPresent("JDBC URL", ds.jdbcUrl);
                dsPanel.addStyled("Default", ds.isDefault ? "Yes" : "No",
                        ds.isDefault ? Style.create().green() : null);
                row = dsPanel.render(buffer, row, startCol, panelWidth - 2);
                row++;
            }
        }

        row++;

        if (!tableList.isEmpty()) {
            int maxTables = Math.min(tableList.size(), panelHeight - row + startRow - 1);
            tableList.setVisibleRows(maxTables);
            tableList.setWidth(panelWidth - 2);
            tableList.render(buffer, row, startCol);
        }
    }

    @Override
    public boolean handlePanelKey(int key) {
        if (loading) {
            return false;
        }

        if (key == '\t' || key == KeyCode.TAB) {
            focusedPanel = (focusedPanel == PANEL_DATASOURCES) ? PANEL_TABLES : PANEL_DATASOURCES;
            return true;
        }

        if (focusedPanel == PANEL_TABLES) {
            if (tableList.handleKey(key)) {
                return true;
            }
            if (key == KeyCode.ENTER) {
                TableInfo selectedTable = tableList.getSelectedItem();
                if (selectedTable != null) {
                    loadTableData(selectedTable.tableName, 1);
                }
                return true;
            }
        } else {
            if (dataSourceList.handleKey(key)) {
                DataSourceInfo selected = dataSourceList.getSelectedItem();
                if (selected != null) {
                    loadTablesForDataSource(selected.name);
                }
                return true;
            }
            if (key == KeyCode.ENTER) {
                focusedPanel = PANEL_TABLES;
                return true;
            }
        }

        return super.handlePanelKey(key);
    }

    private static class DataSourceInfo {
        final String name;
        final String jdbcUrl;
        final boolean isDefault;

        DataSourceInfo(String name, String jdbcUrl, boolean isDefault) {
            this.name = name;
            this.jdbcUrl = jdbcUrl;
            this.isDefault = isDefault;
        }
    }

    private static class TableInfo {
        final String tableSchema;
        final String tableName;
        final int columnCount;

        TableInfo(String tableSchema, String tableName, int columnCount) {
            this.tableSchema = tableSchema;
            this.tableName = tableName;
            this.columnCount = columnCount;
        }
    }
}
