package io.quarkus.devshell.runtime.tui.screens;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.AppContext;
import io.quarkus.devshell.runtime.tui.BufferHelper;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.Screen;
import io.quarkus.devshell.runtime.tui.widgets.TableView;
import io.quarkus.vertx.http.runtime.devmode.ResourceNotFoundData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Screen for viewing all endpoints exposed by the application.
 */
public class EndpointsScreen implements Screen {

    private AppContext ctx;

    private enum Tab {
        REST("REST"),
        STATIC("Static"),
        ADDITIONAL("Additional");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private Tab currentTab = Tab.REST;
    private boolean loading = true;
    private String error = null;

    // Filter
    private boolean filterMode = false;
    private final StringBuilder filterInput = new StringBuilder();
    private String filterText = "";

    // Per-tab data
    private final Map<Tab, TabData> tabData = new EnumMap<>(Tab.class);

    @Inject
    ResourceNotFoundData service;

    public EndpointsScreen() {
        tabData.put(Tab.REST, new TabData(new TableView<EndpointItem>()
                .addColumn("Method", e -> getHttpMethod(e.description), 8)
                .addColumn("Path", e -> e.uri, 40)
                .addColumn("Details", e -> getDetails(e.description), 30)));

        tabData.put(Tab.STATIC, new TabData(new TableView<EndpointItem>()
                .addColumn("Path", e -> e.uri, 60)
                .addColumn("Type", e -> getFileType(e.uri), 10)));

        tabData.put(Tab.ADDITIONAL, new TabData(new TableView<EndpointItem>()
                .addColumn("Path", e -> e.uri, 40)
                .addColumn("Description", e -> e.description, 40)));
    }

    private static class TabData {
        final List<EndpointItem> allItems = new ArrayList<>();
        List<EndpointItem> filteredItems = new ArrayList<>();
        final TableView<EndpointItem> table;

        TabData(TableView<EndpointItem> table) {
            this.table = table;
        }
    }

    @Override
    public String getTitle() {
        return "Endpoints";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        loadEndpoints();
    }

    @Override
    public void onLeave() {
        // Nothing to clean up
    }

    private void loadEndpoints() {
        loading = true;
        error = null;
        this.ctx.requestRedraw();

        try {
            JsonObject json = service.getAllEndpoints();
            loading = false;
            parseEndpoints(json);
            applyFilter();
            this.ctx.requestRedraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load endpoints: " + ex.getMessage();
            this.ctx.requestRedraw();
        }
    }

    private void parseEndpoints(JsonObject json) {
        for (TabData td : tabData.values()) {
            td.allItems.clear();
        }

        try {
            if (json == null) {
                error = "Empty response from server";
                return;
            }

            List<EndpointItem> restItems = tabData.get(Tab.REST).allItems;

            // Resource Endpoints
            parseJsonArrayInto(json.getJsonArray("Resource Endpoints"), restItems);

            // Servlet mappings (add to REST endpoints)
            JsonArray servletMappings = json.getJsonArray("Servlet mappings");
            if (servletMappings != null) {
                for (int i = 0; i < servletMappings.size(); i++) {
                    JsonObject servlet = servletMappings.getJsonObject(i);
                    restItems.add(new EndpointItem(
                            servlet.getString("uri", ""),
                            "SERVLET"));
                }
            }

            // Static resources (from dedicated key)
            parseJsonArrayInto(json.getJsonArray("Static resources"), tabData.get(Tab.STATIC).allItems);

            // Additional endpoints — split into static resources and real additional endpoints.
            // Static files have no description; additional endpoints have descriptions like "Dev UI", "Health Check".
            JsonArray additionalArray = json.getJsonArray("Additional endpoints");
            if (additionalArray != null) {
                List<EndpointItem> staticItems = tabData.get(Tab.STATIC).allItems;
                List<EndpointItem> additionalItems = tabData.get(Tab.ADDITIONAL).allItems;
                for (int i = 0; i < additionalArray.size(); i++) {
                    JsonObject obj = additionalArray.getJsonObject(i);
                    String uri = obj.getString("uri", "");
                    String description = obj.getString("description");
                    if (description == null || description.isEmpty()) {
                        staticItems.add(new EndpointItem(uri, ""));
                    } else {
                        additionalItems.add(new EndpointItem(uri, description));
                    }
                }
            }
        } catch (Exception e) {
            error = "Failed to parse endpoints: " + e.getMessage();
        }
    }

    private void parseJsonArrayInto(JsonArray array, List<EndpointItem> target) {
        if (array != null) {
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.getJsonObject(i);
                target.add(new EndpointItem(
                        obj.getString("uri", ""),
                        obj.getString("description", "")));
            }
        }
    }

    private void applyFilter() {
        String filter = filterText.toLowerCase();

        for (TabData td : tabData.values()) {
            td.filteredItems = td.allItems.stream()
                    .filter(e -> matchesFilter(e, filter))
                    .toList();
            td.table.setItems(td.filteredItems);
        }
    }

    private boolean matchesFilter(EndpointItem item, String filter) {
        if (filter.isEmpty()) {
            return true;
        }
        return item.uri.toLowerCase().contains(filter) ||
                item.description.toLowerCase().contains(filter);
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        int width = this.ctx.getWidth();
        int height = this.ctx.getHeight();

        // Header
        renderHeader(buffer, width);

        // Tabs
        renderTabs(buffer, width);

        if (loading) {
            buffer.setString(width / 2 - 5, height / 2, "Loading...", Style.create().yellow());
            return;
        }

        if (error != null) {
            buffer.setString(1, 4, error, Style.create().red());
            renderFooter(buffer, width, height);
            return;
        }

        // Content area
        int contentStartRow = 5;
        int contentHeight = height - 7;

        // Filter indicator
        if (filterMode) {
            String filterDisplay = filterInput.toString();
            if (filterDisplay.length() < 30) {
                filterDisplay = filterDisplay + " ".repeat(30 - filterDisplay.length());
            }
            contentStartRow++;
            contentHeight--;
        } else if (!filterText.isEmpty()) {
            contentStartRow++;
            contentHeight--;
        }

        // Render current tab's table
        TableView<EndpointItem> currentTable = getCurrentTable();
        currentTable.setVisibleRows(contentHeight);
        currentTable.setWidth(width - 4);
        currentTable.render(buffer, contentStartRow, 2);

        // Footer
        renderFooter(buffer, width, height);
    }

    private void renderHeader(Buffer buffer, int width) {
        BufferHelper.writeHeader(buffer, " Endpoints ", width);
    }

    private void renderTabs(Buffer buffer, int width) {
        int x = 1;
        for (Tab tab : Tab.values()) {
            String label = " " + tab.label + " (" + getTabCount(tab) + ") ";
            if (tab == currentTab) {
                x += buffer.setString(x, 2, label, Style.create().bold().reversed());
            } else {
                x += buffer.setString(x, 2, label, Style.create().gray());
            }
            x += buffer.setString(x, 2, " ", Style.EMPTY);
        }
    }

    private int getTabCount(Tab tab) {
        TabData td = tabData.get(tab);
        return filterText.isEmpty() ? td.allItems.size() : td.filteredItems.size();
    }

    private TableView<EndpointItem> getCurrentTable() {
        return tabData.get(currentTab).table;
    }

    private void renderFooter(Buffer buffer, int width, int height) {
        if (filterMode) {
            buffer.setString(1, height - 2, "[Enter] Apply  [Esc] Cancel", Style.create().gray());
        } else {
            buffer.setString(1, height - 2, "[Tab] Switch tab  [/] Filter  [Enter] Open  [R] Refresh  [Esc] Back",
                    Style.create().gray());
        }
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
                    filterText = "";
                    applyFilter();
                    this.ctx.requestRedraw();
                } else {
                    this.ctx.goBack();
                }
                return true;

            case KeyCode.TAB:
                // Switch to next tab
                Tab[] tabs = Tab.values();
                int current = currentTab.ordinal();
                currentTab = tabs[(current + 1) % tabs.length];
                this.ctx.requestRedraw();
                return true;

            case '/':
                filterMode = true;
                filterInput.setLength(0);
                filterInput.append(filterText);
                this.ctx.requestRedraw();
                return true;

            case 'r':
            case 'R':
                loadEndpoints();
                return true;

            case KeyCode.ENTER:
                EndpointItem selected = getCurrentTable().getSelectedItem();
                if (selected != null) {
                    this.ctx.navigateTo(new EndpointDetailScreen(selected));
                }
                return true;

            default:
                if (getCurrentTable().handleKey(key)) {
                    this.ctx.requestRedraw();
                    return true;
                }
                return false;
        }
    }

    private boolean handleFilterKey(int key) {
        switch (key) {
            case KeyCode.ESCAPE:
                filterMode = false;
                this.ctx.requestRedraw();
                return true;

            case KeyCode.ENTER:
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
        // Tables handle their own resizing
    }

    // Helper methods for parsing endpoint descriptions
    private String getHttpMethod(String description) {
        if (description == null || description.isEmpty()) {
            return "";
        }
        // Description format: "GET (consumes: ...) (produces: ...) (java: ...)"
        int spaceIdx = description.indexOf(' ');
        int parenIdx = description.indexOf('(');
        if (parenIdx > 0 && (spaceIdx < 0 || parenIdx < spaceIdx)) {
            return description.substring(0, parenIdx).trim();
        }
        if (spaceIdx > 0) {
            return description.substring(0, spaceIdx);
        }
        return description;
    }

    private String getDetails(String description) {
        if (description == null || description.isEmpty()) {
            return "";
        }
        // Extract java method if present
        int javaIdx = description.indexOf("(java:");
        if (javaIdx >= 0) {
            int endIdx = description.indexOf(')', javaIdx);
            if (endIdx > javaIdx) {
                return description.substring(javaIdx + 6, endIdx).trim();
            }
        }
        // Otherwise return consumes/produces
        int parenIdx = description.indexOf('(');
        if (parenIdx > 0) {
            return description.substring(parenIdx);
        }
        return "";
    }

    private String getFileType(String uri) {
        if (uri == null) {
            return "";
        }
        int dotIdx = uri.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < uri.length() - 1) {
            return uri.substring(dotIdx + 1).toUpperCase();
        }
        return "FILE";
    }

    /**
     * Data class for endpoint items.
     */
    public static class EndpointItem {
        public final String uri;
        public final String description;

        public EndpointItem(String uri, String description) {
            this.uri = uri;
            this.description = description != null ? description : "";
        }
    }
}
