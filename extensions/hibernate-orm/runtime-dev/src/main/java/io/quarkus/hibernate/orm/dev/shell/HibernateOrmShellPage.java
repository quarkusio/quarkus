package io.quarkus.hibernate.orm.dev.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.runtime.tui.widgets.ListView;
import io.quarkus.devshell.runtime.tui.widgets.TableView;
import io.quarkus.hibernate.orm.dev.HibernateOrmDevInfo;
import io.quarkus.hibernate.orm.dev.ui.HibernateOrmDevJsonRpcService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Shell page for Hibernate ORM showing persistence units, entities, named queries, and HQL console.
 */
public class HibernateOrmShellPage extends BaseExtensionPage {

    @Inject
    HibernateOrmDevJsonRpcService hibernateService;

    private static final int TAB_PERSISTENCE_UNITS = 0;
    private static final int TAB_ENTITIES = 1;
    private static final int TAB_NAMED_QUERIES = 2;
    private static final int TAB_HQL_CONSOLE = 3;

    private enum ViewMode {
        TABS,
        HQL_RESULTS
    }

    private ViewMode viewMode = ViewMode.TABS;

    // Data holders
    private final List<PersistenceUnitInfo> persistenceUnits = new ArrayList<>();
    private final List<EntityInfo> entities = new ArrayList<>();
    private final List<QueryInfo> namedQueries = new ArrayList<>();

    // Tables
    private TableView<PersistenceUnitInfo> puTable;
    private TableView<EntityInfo> entityTable;
    private TableView<QueryInfo> queryTable;

    // HQL Console
    private ListView<PersistenceUnitInfo> puSelector;
    private StringBuilder hqlInput = new StringBuilder();
    private String selectedPuName = null;
    private List<String> resultColumns = new ArrayList<>();
    private List<Map<String, String>> resultRows = new ArrayList<>();
    private int currentPage = 1;
    private int pageSize = 10;
    private long totalRows = 0;
    private String lastQuery = null;
    private String queryMessage = null;
    private String queryError = null;
    private boolean hqlInputMode = false;

    // No-arg constructor for CDI
    public HibernateOrmShellPage() {
        setTabs("Persistence Units", "Entities", "Named Queries", "HQL Console");

        // Persistence Units table
        this.puTable = new TableView<PersistenceUnitInfo>()
                .addColumn("Name", pu -> pu.name(), 30)
                .addColumn("Entities", pu -> String.valueOf(pu.entityCount()), 10)
                .addColumn("Queries", pu -> String.valueOf(pu.queryCount()), 10)
                .addColumn("Reactive", pu -> pu.reactive() ? "Yes" : "No", 10);

        // Entities table
        this.entityTable = new TableView<EntityInfo>()
                .addColumn("Entity Name", e -> e.name(), 30)
                .addColumn("Class", e -> e.className(), 35)
                .addColumn("Table", e -> e.tableName(), 20)
                .addColumn("PU", e -> e.persistenceUnit(), 15);

        // Named Queries table
        this.queryTable = new TableView<QueryInfo>()
                .addColumn("Name", q -> q.name(), 25)
                .addColumn("Type", q -> q.type(), 8)
                .addColumn("Cacheable", q -> q.cacheable() ? "Yes" : "No", 10)
                .addColumn("Lock", q -> q.lockMode() != null ? q.lockMode() : "-", 10)
                .addColumn("Query", q -> truncate(q.query(), 40), 40);

        // PU selector for HQL console
        this.puSelector = new ListView<>(pu -> pu.name());
    }

    public HibernateOrmShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        persistenceUnits.clear();
        entities.clear();
        namedQueries.clear();
        redraw();

        try {
            HibernateOrmDevInfo info = hibernateService.getInfo();
            loading = false;

            if (info != null) {
                for (HibernateOrmDevInfo.PersistenceUnit pu : info.getPersistenceUnits()) {
                    String puName = pu.getName();

                    List<EntityInfo> puEntities = new ArrayList<>();
                    for (HibernateOrmDevInfo.Entity entity : pu.getManagedEntities()) {
                        if (entity.getName() != null) {
                            puEntities.add(new EntityInfo(entity.getName(), entity.getClassName(),
                                    entity.getTableName(), puName));
                        }
                    }

                    List<QueryInfo> puQueries = new ArrayList<>();
                    for (HibernateOrmDevInfo.Query query : pu.getNamedQueries()) {
                        if (query.getName() != null) {
                            puQueries.add(new QueryInfo(query.getName(), query.getQuery(),
                                    query.getType() != null ? query.getType() : "JPQL",
                                    query.isCacheable(), query.getLockMode(), puName));
                        }
                    }
                    for (HibernateOrmDevInfo.Query query : pu.getNamedNativeQueries()) {
                        if (query.getName() != null) {
                            puQueries.add(new QueryInfo(query.getName(), query.getQuery(),
                                    "Native", query.isCacheable(), query.getLockMode(), puName));
                        }
                    }

                    if (puName != null) {
                        persistenceUnits.add(new PersistenceUnitInfo(puName, puEntities.size(),
                                puQueries.size(), pu.isReactive()));
                        entities.addAll(puEntities);
                        namedQueries.addAll(puQueries);
                    }
                }
            }

            puTable.setItems(persistenceUnits);
            entityTable.setItems(entities);
            queryTable.setItems(namedQueries);
            puSelector.setItems(persistenceUnits);
            if (!persistenceUnits.isEmpty() && selectedPuName == null) {
                selectedPuName = persistenceUnits.get(0).name();
            }
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load Hibernate ORM info: " + ex.getMessage();
            redraw();
        }
    }

    private void executeHQL(String hql, int page) {
        if (selectedPuName == null || selectedPuName.isEmpty()) {
            queryError = "No persistence unit selected";
            redraw();
            return;
        }

        loading = true;
        queryError = null;
        queryMessage = null;
        lastQuery = hql;
        currentPage = page;
        resultColumns.clear();
        resultRows.clear();
        redraw();

        try {
            Map<String, String> result = hibernateService.executeHQL(
                    selectedPuName, hql, page, pageSize, false, false)
                    .toCompletableFuture().get();
            loading = false;

            // Parse the response map
            String responseStr = result.get("response");
            if (responseStr != null) {
                JsonObject responseObj = new JsonObject(responseStr);

                String errorMsg = responseObj.getString("error");
                if (errorMsg != null && !errorMsg.isEmpty()) {
                    queryError = errorMsg;
                } else {
                    String message = responseObj.getString("message");
                    if (message != null && !message.isEmpty()) {
                        queryMessage = message;
                    } else {
                        totalRows = responseObj.getLong("resultCount", 0L);
                        JsonArray dataArray = responseObj.getJsonArray("data");
                        if (dataArray != null && !dataArray.isEmpty()) {
                            JsonObject firstObj = dataArray.getJsonObject(0);
                            resultColumns = new ArrayList<>(firstObj.fieldNames());
                            for (int i = 0; i < dataArray.size(); i++) {
                                JsonObject rowObj = dataArray.getJsonObject(i);
                                Map<String, String> row = new HashMap<>();
                                for (String col : resultColumns) {
                                    Object value = rowObj.getValue(col);
                                    row.put(col, value != null ? value.toString() : "null");
                                }
                                resultRows.add(row);
                            }
                        }
                    }
                }
            } else {
                queryError = "No response data";
            }

            viewMode = ViewMode.HQL_RESULTS;
            redraw();
        } catch (Exception ex) {
            loading = false;
            queryError = "Failed to execute HQL: " + ex.getMessage();
            redraw();
        }
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        if (viewMode == ViewMode.HQL_RESULTS) {
            renderHQLResults(buffer);
        } else {
            renderTabsContent(buffer);
        }

        if (error != null) {
            renderError(buffer, height - 3);
        }
    }

    private void renderTabsContent(Buffer buffer) {
        int row = renderTabBar(buffer, 3);

        // Horizontal separator below tabs

        // Content based on current tab
        switch (getCurrentTabIndex()) {
            case TAB_PERSISTENCE_UNITS -> renderPersistenceUnitsTab(buffer, row);
            case TAB_ENTITIES -> renderEntitiesTab(buffer, row);
            case TAB_NAMED_QUERIES -> renderNamedQueriesTab(buffer, row);
            case TAB_HQL_CONSOLE -> renderHQLConsoleTab(buffer, row);
        }
    }

    private void renderPersistenceUnitsTab(Buffer buffer, int startRow) {
        if (loading && puTable.isEmpty()) {
            renderLoading(buffer, startRow);
            return;
        }

        int row = startRow;
        buffer.setString(1, row - 1, "Persistence Units (" + persistenceUnits.size() + ")", Style.create().cyan().bold());
        row++;

        if (puTable.isEmpty()) {
            buffer.setString(1, row - 1, "No persistence units found", Style.create().gray());
        } else {
            puTable.setVisibleRows(height - row - 4);
            puTable.setWidth(width - 4);
            puTable.render(buffer, row, 2);
        }

        renderFooter(buffer, "");
    }

    private void renderEntitiesTab(Buffer buffer, int startRow) {
        if (loading && entityTable.isEmpty()) {
            renderLoading(buffer, startRow);
            return;
        }

        int row = startRow;
        buffer.setString(1, row - 1, "Entities (" + entities.size() + ")", Style.create().cyan().bold());
        row++;

        if (entityTable.isEmpty()) {
            buffer.setString(1, row - 1, "No entities found", Style.create().gray());
        } else {
            entityTable.setVisibleRows(height - row - 4);
            entityTable.setWidth(width - 4);
            entityTable.render(buffer, row, 2);
        }

        renderFooter(buffer, "");
    }

    private void renderNamedQueriesTab(Buffer buffer, int startRow) {
        if (loading && queryTable.isEmpty()) {
            renderLoading(buffer, startRow);
            return;
        }

        int row = startRow;
        buffer.setString(1, row - 1, "Named Queries (" + namedQueries.size() + ")", Style.create().cyan().bold());
        row++;

        if (queryTable.isEmpty()) {
            buffer.setString(1, row - 1, "No named queries found", Style.create().gray());
        } else {
            queryTable.setVisibleRows(height - row - 4);
            queryTable.setWidth(width - 4);
            queryTable.render(buffer, row, 2);
        }

        renderFooter(buffer, "[Enter] Execute query");
    }

    private void renderHQLConsoleTab(Buffer buffer, int startRow) {
        int row = startRow;

        // Persistence unit selector
        KeyValuePanel puPanel = new KeyValuePanel();
        puPanel.add("Persistence Unit", selectedPuName != null ? selectedPuName : "(none)");
        row = puPanel.render(buffer, row, 2, width - 4);
        row++;

        // HQL input
        buffer.setString(1, row - 1, "HQL> ", Style.create().cyan());
        if (hqlInputMode) {
            buffer.setString(6, row - 1, hqlInput.toString(), Style.EMPTY);
            buffer.setString(6 + hqlInput.length(), row - 1, "_", Style.create().gray());
        } else if (lastQuery != null) {
            buffer.setString(6, row - 1, lastQuery, Style.create().gray());
        }
        row += 2;

        // Instructions
        if (hqlInputMode) {
            buffer.setString(1, row - 1, "Type HQL query. Press Enter to execute, Esc to cancel.", Style.create().gray());
        } else {
            buffer.setString(1, row - 1, "Press / or S to enter a query.", Style.create().gray());
        }
        row += 2;

        // Last query result info
        if (queryError != null) {
            buffer.setString(1, row - 1, "Error: " + queryError, Style.create().red());
            row++;
        }
        if (queryMessage != null) {
            buffer.setString(1, row - 1, queryMessage, Style.create().green());
            row++;
        }

        if (hqlInputMode) {
            buffer.setString(1, height - 2, "[Enter] Execute  [Esc] Cancel  [Backspace] Delete", Style.create().gray());
        } else {
            renderFooter(buffer, "[/] Query  [P] PU");
        }
    }

    private void renderHQLResults(Buffer buffer) {
        int row = 3;

        // Header
        buffer.setString(1, row - 1, "HQL Results", Style.create().cyan().bold());
        if (totalRows > 0) {
            int totalPages = (int) ((totalRows + pageSize - 1) / pageSize);
            String pageInfo = "Page " + currentPage + "/" + Math.max(1, totalPages) + " (" + totalRows + " rows)";
            buffer.setString(width - pageInfo.length() - 2, row - 1, pageInfo, Style.create().gray());
        }
        row++;

        buffer.setString(1, row - 1, "─".repeat(width - 4), Style.create().gray());
        row++;

        // Show query
        if (lastQuery != null) {
            buffer.setString(1, row - 1, "Query: " + truncate(lastQuery, width - 12), Style.create().gray());
            row += 2;
        }

        // Error or message
        if (queryError != null) {
            buffer.setString(1, row - 1, "Error: " + queryError, Style.create().red());
            row += 2;
        }

        if (queryMessage != null) {
            buffer.setString(1, row - 1, queryMessage, Style.create().green());
            row += 2;
        }

        // Results table
        if (!resultColumns.isEmpty() && !resultRows.isEmpty()) {
            renderResultsTable(buffer, row);
        } else if (queryError == null && queryMessage == null && !loading) {
            buffer.setString(1, row - 1, "No results", Style.create().gray());
        }

        if (loading) {
            buffer.setString(1, row - 1, "Executing query...", Style.create().yellow());
        }

        // Footer for results mode
        buffer.setString(1, height - 2, "[Esc] Back  [</>] Page  [/] New query", Style.create().gray());
    }

    private void renderResultsTable(Buffer buffer, int startRow) {
        int row = startRow;
        int availableWidth = width - 4;
        int numCols = resultColumns.size();
        int colWidth = Math.max(12, availableWidth / Math.max(numCols, 1));

        // Header row
        int x = 1;
        for (int i = 0; i < numCols && (i + 1) * colWidth < availableWidth; i++) {
            String colName = truncate(resultColumns.get(i), colWidth - 1);
            buffer.setString(x, row - 1, String.format(Locale.ROOT, "%-" + (colWidth - 1) + "s", colName),
                    Style.create().cyan().bold());
            x += colWidth;
        }
        row++;

        // Separator
        x = 1;
        for (int i = 0; i < numCols && (i + 1) * colWidth < availableWidth; i++) {
            buffer.setString(x, row - 1, "─".repeat(colWidth - 1), Style.create().gray());
            x += colWidth;
        }
        row++;

        // Data rows
        int maxRows = height - row - 4;
        for (int r = 0; r < Math.min(resultRows.size(), maxRows); r++) {
            Map<String, String> rowData = resultRows.get(r);
            x = 1;
            for (int i = 0; i < numCols && (i + 1) * colWidth < availableWidth; i++) {
                String value = rowData.getOrDefault(resultColumns.get(i), "null");
                value = truncate(value, colWidth - 1);
                buffer.setString(x, row - 1, String.format(Locale.ROOT, "%-" + (colWidth - 1) + "s", value), Style.EMPTY);
                x += colWidth;
            }
            row++;
        }
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        if (viewMode == ViewMode.HQL_RESULTS) {
            return handleHQLResultsKey(key);
        }

        if (hqlInputMode) {
            return handleHQLInputKey(key);
        }

        // Tab-specific keys (before base class tab handling)
        switch (getCurrentTabIndex()) {
            case TAB_PERSISTENCE_UNITS -> {
                if (puTable.handleKey(key)) {
                    redraw();
                    return true;
                }
            }
            case TAB_ENTITIES -> {
                if (entityTable.handleKey(key)) {
                    redraw();
                    return true;
                }
            }
            case TAB_NAMED_QUERIES -> {
                if (queryTable.handleKey(key)) {
                    redraw();
                    return true;
                }
                if (key == KeyCode.ENTER) {
                    // Execute selected named query
                    QueryInfo selected = queryTable.getSelectedItem();
                    if (selected != null) {
                        selectedPuName = selected.persistenceUnit();
                        hqlInput.setLength(0);
                        hqlInput.append(selected.query());
                        executeHQL(selected.query(), 1);
                    }
                    return true;
                }
            }
            case TAB_HQL_CONSOLE -> {
                if (key == '/' || key == 's' || key == 'S') {
                    hqlInputMode = true;
                    redraw();
                    return true;
                }
                if (key == 'p' || key == 'P') {
                    // Cycle through persistence units
                    if (!persistenceUnits.isEmpty()) {
                        int currentIdx = 0;
                        for (int i = 0; i < persistenceUnits.size(); i++) {
                            if (persistenceUnits.get(i).name().equals(selectedPuName)) {
                                currentIdx = i;
                                break;
                            }
                        }
                        currentIdx = (currentIdx + 1) % persistenceUnits.size();
                        selectedPuName = persistenceUnits.get(currentIdx).name();
                        redraw();
                    }
                    return true;
                }
            }
        }

        // Let base class handle Tab key switching + R/Esc
        return super.handleKey(key);
    }

    private boolean handleHQLInputKey(int key) {
        switch (key) {
            case KeyCode.ESCAPE:
                hqlInputMode = false;
                redraw();
                return true;

            case KeyCode.ENTER:
                if (hqlInput.length() > 0) {
                    String hql = hqlInput.toString().trim();
                    hqlInputMode = false;
                    executeHQL(hql, 1);
                }
                return true;

            case KeyCode.BACKSPACE:
                if (hqlInput.length() > 0) {
                    hqlInput.deleteCharAt(hqlInput.length() - 1);
                    redraw();
                }
                return true;

            default:
                if (key >= 32 && key < 127) {
                    hqlInput.append((char) key);
                    redraw();
                    return true;
                }
                return false;
        }
    }

    private boolean handleHQLResultsKey(int key) {
        switch (key) {
            case KeyCode.ESCAPE:
                viewMode = ViewMode.TABS;
                setCurrentTabIndex(TAB_HQL_CONSOLE);
                redraw();
                return true;

            case '<':
            case ',':
            case KeyCode.PAGE_UP:
                if (currentPage > 1 && lastQuery != null) {
                    executeHQL(lastQuery, currentPage - 1);
                }
                return true;

            case '>':
            case '.':
            case KeyCode.PAGE_DOWN:
                int totalPages = (int) ((totalRows + pageSize - 1) / pageSize);
                if (currentPage < totalPages && lastQuery != null) {
                    executeHQL(lastQuery, currentPage + 1);
                }
                return true;

            case '/':
                viewMode = ViewMode.TABS;
                setCurrentTabIndex(TAB_HQL_CONSOLE);
                hqlInputMode = true;
                redraw();
                return true;

            default:
                return super.handleKey(key);
        }
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        int visibleRows = height - 10;
        int tableWidth = width - 4;
        puTable.setVisibleRows(visibleRows);
        puTable.setWidth(tableWidth);
        entityTable.setVisibleRows(visibleRows);
        entityTable.setWidth(tableWidth);
        queryTable.setVisibleRows(visibleRows);
        queryTable.setWidth(tableWidth);
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

        // Summary using KeyValuePanel
        KeyValuePanel summary = new KeyValuePanel();
        summary.add("PUs", String.valueOf(persistenceUnits.size()));
        summary.add("Entities", String.valueOf(entities.size()));
        summary.add("Queries", String.valueOf(namedQueries.size()));
        summary.render(buffer, row, startCol, panelWidth);
    }

    // Data records

    private record PersistenceUnitInfo(String name, int entityCount, int queryCount, boolean reactive) {
        PersistenceUnitInfo {
            name = name != null ? name : "default";
        }
    }

    private record EntityInfo(String name, String className, String tableName, String persistenceUnit) {
        EntityInfo {
            name = name != null ? name : "unknown";
            className = className != null ? className : "";
            tableName = tableName != null ? tableName : "";
        }
    }

    private record QueryInfo(String name, String query, String type, boolean cacheable, String lockMode,
            String persistenceUnit) {
        QueryInfo {
            name = name != null ? name : "unknown";
            query = query != null ? query : "";
            type = type != null ? type : "JPQL";
        }
    }
}
