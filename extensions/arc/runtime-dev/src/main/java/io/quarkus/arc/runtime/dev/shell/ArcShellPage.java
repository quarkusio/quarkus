package io.quarkus.arc.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.arc.runtime.dev.EventInfo;
import io.quarkus.arc.runtime.dev.ui.ArcJsonRPCService;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.TableView;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Shell page for the Arc (CDI) extension showing beans, observers, interceptors, and decorators in tables.
 */
public class ArcShellPage extends BaseExtensionPage {

    // Content type identifiers for tab-to-data mapping
    private static final String TAB_BEANS = "beans";
    private static final String TAB_OBSERVERS = "observers";
    private static final String TAB_INTERCEPTORS = "interceptors";
    private static final String TAB_DECORATORS = "decorators";
    private static final String TAB_EVENTS = "events";
    private static final String TAB_REMOVED = "removed";

    // Maps tab index to content type
    private final List<String> tabContentTypes = new ArrayList<>();

    // Tables for each view
    private TableView<BeanInfo> beanTable;
    private TableView<ObserverInfo> observerTable;
    private TableView<InterceptorInfo> interceptorTable;
    private TableView<DecoratorInfo> decoratorTable;
    private TableView<EventInfo> eventTable;
    private TableView<RemovedInfo> removedTable;

    // Counts for tab display
    private int beansCount = 0;
    private int observersCount = 0;
    private int interceptorsCount = 0;
    private int decoratorsCount = 0;
    private int removedCount = 0;

    @Inject
    ArcJsonRPCService arcService;

    // No-arg constructor for CDI
    public ArcShellPage() {
        // Beans table
        this.beanTable = new TableView<BeanInfo>()
                .addColumn("Bean Class", b -> b.providerTypeName, 40)
                .addColumn("Scope", b -> "@" + b.scopeSimpleName, 15)
                .addColumn("Kind", this::formatBeanKind, 12);

        // Observers table
        this.observerTable = new TableView<ObserverInfo>()
                .addColumn("Observed Type", o -> o.observedType, 50)
                .addColumn("Priority", o -> String.valueOf(o.priority), 10)
                .addColumn("Reception", o -> o.reception != null ? o.reception : "ALWAYS", 12);

        // Interceptors table
        this.interceptorTable = new TableView<InterceptorInfo>()
                .addColumn("Interceptor Class", i -> i.interceptorClass, 50)
                .addColumn("Priority", i -> String.valueOf(i.priority), 10);

        // Decorators table
        this.decoratorTable = new TableView<DecoratorInfo>()
                .addColumn("Decorator Class", d -> d.decoratorClass, 50)
                .addColumn("Delegate Type", d -> d.delegateType != null ? d.delegateType : "", 30);

        // Events table
        this.eventTable = new TableView<EventInfo>()
                .addColumn("Event Type", e -> e.getType(), 50)
                .addColumn("Qualifiers", e -> e.getQualifiers() != null ? String.join(", ", e.getQualifiers()) : "", 25)
                .addColumn("Context", e -> e.isIsContextEvent() ? "Yes" : "No", 8);

        // Removed components table
        this.removedTable = new TableView<RemovedInfo>()
                .addColumn("Description", r -> r.description, 60)
                .addColumn("Type", r -> r.type, 15);
    }

    public ArcShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    private String formatBeanKind(BeanInfo bean) {
        if (bean.kind == null)
            return "";
        return switch (bean.kind.toUpperCase()) {
            case "CLASS" -> "Class";
            case "METHOD" -> "Producer";
            case "FIELD" -> "Producer";
            case "SYNTHETIC" -> "Synthetic";
            default -> bean.kind;
        };
    }

    /**
     * Get the content type for the currently selected tab.
     */
    private String currentContentType() {
        int idx = getCurrentTabIndex();
        if (idx >= 0 && idx < tabContentTypes.size()) {
            return tabContentTypes.get(idx);
        }
        return TAB_BEANS;
    }

    /**
     * Build the dynamic tab list based on current data counts.
     */
    private void buildTabs() {
        // Remember current content type so we can restore position after rebuilding
        String previousContentType = tabContentTypes.isEmpty() ? null : currentContentType();

        tabContentTypes.clear();

        // Always include these tabs
        tabContentTypes.add(TAB_BEANS);
        tabContentTypes.add(TAB_OBSERVERS);
        tabContentTypes.add(TAB_INTERCEPTORS);

        // Conditionally include Decorators
        if (decoratorsCount > 0) {
            tabContentTypes.add(TAB_DECORATORS);
        }

        // Always include Events
        tabContentTypes.add(TAB_EVENTS);

        // Conditionally include Removed
        if (removedCount > 0) {
            tabContentTypes.add(TAB_REMOVED);
        }

        // Build tab display names with counts
        String[] names = new String[tabContentTypes.size()];
        for (int i = 0; i < tabContentTypes.size(); i++) {
            names[i] = tabDisplayName(tabContentTypes.get(i));
        }
        setTabs(names);

        // Restore tab position if possible
        if (previousContentType != null) {
            int newIdx = tabContentTypes.indexOf(previousContentType);
            if (newIdx >= 0) {
                setCurrentTabIndex(newIdx);
            }
        }
    }

    private String tabDisplayName(String contentType) {
        return switch (contentType) {
            case TAB_BEANS -> "Beans (" + beansCount + ")";
            case TAB_OBSERVERS -> "Observers (" + observersCount + ")";
            case TAB_INTERCEPTORS -> "Interceptors (" + interceptorsCount + ")";
            case TAB_DECORATORS -> "Decorators (" + decoratorsCount + ")";
            case TAB_EVENTS -> "Events (" + eventTable.size() + ")";
            case TAB_REMOVED -> "Removed (" + removedCount + ")";
            default -> contentType;
        };
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        redraw();

        // Load build-time data for beans, observers, interceptors, decorators
        loadBuildTimeData();

        // Build tabs after counts are known
        buildTabs();

        // Load runtime data for events if the Events tab is selected
        if (TAB_EVENTS.equals(currentContentType())) {
            loadEvents();
        } else {
            loading = false;
            redraw();
        }
    }

    private void loadBuildTimeData() {
        // Load beans
        String beansJson = getBuildTimeData("quarkus-arc", "beans");
        if (beansJson != null) {
            parseBeans(beansJson);
        }

        // Load observers
        String observersJson = getBuildTimeData("quarkus-arc", "observers");
        if (observersJson != null) {
            parseObservers(observersJson);
        }

        // Load interceptors
        String interceptorsJson = getBuildTimeData("quarkus-arc", "interceptors");
        if (interceptorsJson != null) {
            parseInterceptors(interceptorsJson);
        }

        // Load decorators
        String decoratorsJson = getBuildTimeData("quarkus-arc", "decorators");
        if (decoratorsJson != null) {
            parseDecorators(decoratorsJson);
        }

        // Load removed beans
        String removedBeansJson = getBuildTimeData("quarkus-arc", "removedBeans");
        if (removedBeansJson != null) {
            parseRemovedBeans(removedBeansJson);
        }
    }

    private void loadEvents() {
        loading = true;
        redraw();
        try {
            List<EventInfo> events = arcService.getLastEvents();
            loading = false;
            eventTable.setItems(events != null ? events : List.of());
            buildTabs();
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load events: " + ex.getMessage();
            redraw();
        }
    }

    private void parseBeans(String json) {
        List<BeanInfo> beans = new ArrayList<>();
        try {
            if (json == null || json.equals("[]")) {
                beanTable.setItems(beans);
                beansCount = 0;
                return;
            }

            JsonArray array = new JsonArray(json);
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.getJsonObject(i);
                JsonObject providerType = obj.getJsonObject("providerType");
                String providerTypeName = providerType != null ? providerType.getString("name") : null;
                JsonObject scope = obj.getJsonObject("scope");
                String scopeSimpleName = scope != null ? scope.getString("simpleName") : null;
                String kind = obj.getString("kind");

                if (providerTypeName != null) {
                    beans.add(new BeanInfo(providerTypeName, scopeSimpleName != null ? scopeSimpleName : "?", kind));
                }
            }
        } catch (Exception e) {
            error = "Failed to parse beans: " + e.getMessage();
        }

        beanTable.setItems(beans);
        beansCount = beans.size();
    }

    private void parseObservers(String json) {
        List<ObserverInfo> observers = new ArrayList<>();
        try {
            if (json == null || json.equals("[]")) {
                observerTable.setItems(observers);
                observersCount = 0;
                return;
            }

            JsonArray array = new JsonArray(json);
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.getJsonObject(i);
                JsonObject observedType = obj.getJsonObject("observedType");
                String observedTypeName = observedType != null ? observedType.getString("name") : null;
                int priority = obj.getInteger("priority", 0);
                String reception = obj.getString("reception");

                if (observedTypeName != null) {
                    observers.add(new ObserverInfo(observedTypeName, priority, reception));
                }
            }
        } catch (Exception e) {
            error = "Failed to parse observers: " + e.getMessage();
        }

        observerTable.setItems(observers);
        observersCount = observers.size();
    }

    private void parseInterceptors(String json) {
        List<InterceptorInfo> interceptors = new ArrayList<>();
        try {
            if (json == null || json.equals("[]")) {
                interceptorTable.setItems(interceptors);
                interceptorsCount = 0;
                return;
            }

            JsonArray array = new JsonArray(json);
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.getJsonObject(i);
                JsonObject interceptorClassObj = obj.getJsonObject("interceptorClass");
                String interceptorClass = interceptorClassObj != null ? interceptorClassObj.getString("name") : null;
                int priority = obj.getInteger("priority", 0);

                if (interceptorClass != null) {
                    interceptors.add(new InterceptorInfo(interceptorClass, priority));
                }
            }
        } catch (Exception e) {
            error = "Failed to parse interceptors: " + e.getMessage();
        }

        interceptorTable.setItems(interceptors);
        interceptorsCount = interceptors.size();
    }

    private void parseDecorators(String json) {
        List<DecoratorInfo> decorators = new ArrayList<>();
        try {
            if (json == null || json.equals("[]")) {
                decoratorTable.setItems(decorators);
                decoratorsCount = 0;
                return;
            }

            JsonArray array = new JsonArray(json);
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.getJsonObject(i);
                JsonObject decoratorClassObj = obj.getJsonObject("decoratorClass");
                String decoratorClass = decoratorClassObj != null ? decoratorClassObj.getString("name") : null;
                JsonObject delegateTypeObj = obj.getJsonObject("delegateType");
                String delegateType = delegateTypeObj != null ? delegateTypeObj.getString("name") : null;

                if (decoratorClass != null) {
                    decorators.add(new DecoratorInfo(decoratorClass, delegateType));
                }
            }
        } catch (Exception e) {
            error = "Failed to parse decorators: " + e.getMessage();
        }

        decoratorTable.setItems(decorators);
        decoratorsCount = decorators.size();
    }

    private void parseRemovedBeans(String json) {
        List<RemovedInfo> removed = new ArrayList<>();
        try {
            if (json == null || json.equals("[]")) {
                removedTable.setItems(removed);
                removedCount = 0;
                return;
            }

            JsonArray array = new JsonArray(json);
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.getJsonObject(i);
                String description = obj.getString("description");
                String kind = obj.getString("kind");

                if (description != null) {
                    removed.add(new RemovedInfo(description, kind != null ? kind : "Bean"));
                }
            }
        } catch (Exception e) {
            error = "Failed to parse removed beans: " + e.getMessage();
        }

        removedTable.setItems(removed);
        removedCount = removed.size();
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        // Render the tab bar starting at row 3
        int row = renderTabBar(buffer, 3);

        // Draw separator line below the tab bar

        // Content based on current tab
        String contentType = currentContentType();
        switch (contentType) {
            case TAB_BEANS -> renderTableView(buffer, row, beanTable, "CDI Beans", beansCount);
            case TAB_OBSERVERS -> renderTableView(buffer, row, observerTable, "Observers", observersCount);
            case TAB_INTERCEPTORS -> renderTableView(buffer, row, interceptorTable, "Interceptors", interceptorsCount);
            case TAB_DECORATORS -> renderTableView(buffer, row, decoratorTable, "Decorators", decoratorsCount);
            case TAB_EVENTS -> renderEventsView(buffer, row);
            case TAB_REMOVED -> renderTableView(buffer, row, removedTable, "Removed Components", removedCount);
        }

        // Error
        if (error != null) {
            renderError(buffer, height - 3);
        }
    }

    private <T> void renderTableView(Buffer buffer, int startRow, TableView<T> table, String title,
            int count) {
        if (loading && table.isEmpty()) {
            renderLoading(buffer, startRow);
            return;
        }

        int row = startRow;
        int x = 1;
        x += buffer.setString(x, row, title, Style.create().cyan().bold());
        buffer.setString(x, row, " (" + count + ")", Style.create().gray());
        row++;

        if (table.isEmpty()) {
            buffer.setString(1, row + 1, "No items found.", Style.create().gray());
        } else {
            table.setVisibleRows(height - row - 6);
            table.setWidth(width - 4);
            table.render(buffer, row, 2);
        }

        renderFooter(buffer, "");
    }

    private void renderEventsView(Buffer buffer, int startRow) {
        if (loading && eventTable.isEmpty()) {
            renderLoading(buffer, startRow);
            return;
        }

        int row = startRow;
        int x = 1;
        x += buffer.setString(x, row, "Fired Events", Style.create().cyan().bold());
        buffer.setString(x, row, " (" + eventTable.size() + " events)", Style.create().gray());
        row++;

        if (eventTable.isEmpty()) {
            buffer.setString(1, row + 1, "No events captured yet. Fire some CDI events in your application.",
                    Style.create().gray());
        } else {
            eventTable.setVisibleRows(height - row - 6);
            eventTable.setWidth(width - 4);
            eventTable.render(buffer, row, 2);
        }

        renderFooter(buffer, "[C] Clear  [T] Toggle context");
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        // Detect tab change: capture content type before base class processes the key
        String contentBefore = currentContentType();

        // Let base class handle Tab key (and number keys for tab switching)
        if (key == KeyCode.TAB || (key >= '1' && key <= '9')) {
            boolean handled = super.handleKey(key);
            if (handled) {
                String contentAfter = currentContentType();
                // If we just switched to the Events tab, load events lazily
                if (TAB_EVENTS.equals(contentAfter) && !TAB_EVENTS.equals(contentBefore)) {
                    loadEvents();
                }
                return true;
            }
        }

        // View-specific keys based on current tab
        String contentType = currentContentType();
        switch (contentType) {
            case TAB_BEANS -> {
                if (beanTable.handleKey(key)) {
                    redraw();
                    return true;
                }
            }
            case TAB_OBSERVERS -> {
                if (observerTable.handleKey(key)) {
                    redraw();
                    return true;
                }
            }
            case TAB_INTERCEPTORS -> {
                if (interceptorTable.handleKey(key)) {
                    redraw();
                    return true;
                }
            }
            case TAB_DECORATORS -> {
                if (decoratorTable.handleKey(key)) {
                    redraw();
                    return true;
                }
            }
            case TAB_EVENTS -> {
                if (key == 'c' || key == 'C') {
                    clearEvents();
                    return true;
                }
                if (key == 't' || key == 'T') {
                    toggleSkipContextEvents();
                    return true;
                }
                if (eventTable.handleKey(key)) {
                    redraw();
                    return true;
                }
            }
            case TAB_REMOVED -> {
                if (removedTable.handleKey(key)) {
                    redraw();
                    return true;
                }
            }
        }

        return super.handleKey(key);
    }

    private void clearEvents() {
        loading = true;
        redraw();
        try {
            List<EventInfo> events = arcService.clearLastEvents();
            loading = false;
            eventTable.setItems(events != null ? events : List.of());
            buildTabs();
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to clear events: " + ex.getMessage();
            redraw();
        }
    }

    private void toggleSkipContextEvents() {
        loading = true;
        redraw();
        try {
            List<EventInfo> events = arcService.toggleSkipContextEvents();
            loading = false;
            eventTable.setItems(events != null ? events : List.of());
            buildTabs();
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to toggle context events: " + ex.getMessage();
            redraw();
        }
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        int visibleRows = height - 12;
        int tableWidth = width - 4;
        beanTable.setVisibleRows(visibleRows);
        beanTable.setWidth(tableWidth);
        observerTable.setVisibleRows(visibleRows);
        observerTable.setWidth(tableWidth);
        interceptorTable.setVisibleRows(visibleRows);
        interceptorTable.setWidth(tableWidth);
        decoratorTable.setVisibleRows(visibleRows);
        decoratorTable.setWidth(tableWidth);
        eventTable.setVisibleRows(visibleRows);
        eventTable.setWidth(tableWidth);
        removedTable.setVisibleRows(visibleRows);
        removedTable.setWidth(tableWidth);
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

        // Show summary in panel mode
        buffer.setString(startCol, row, "Beans: " + beansCount, Style.EMPTY);
        row++;
        buffer.setString(startCol, row, "Observers: " + observersCount, Style.EMPTY);
        row++;
        if (decoratorsCount > 0) {
            buffer.setString(startCol, row, "Decorators: " + decoratorsCount, Style.EMPTY);
            row++;
        }
    }

    // Data classes
    private static class BeanInfo {
        final String providerTypeName;
        final String scopeSimpleName;
        final String kind;

        BeanInfo(String providerTypeName, String scopeSimpleName, String kind) {
            this.providerTypeName = providerTypeName;
            this.scopeSimpleName = scopeSimpleName;
            this.kind = kind;
        }
    }

    private static class ObserverInfo {
        final String observedType;
        final int priority;
        final String reception;

        ObserverInfo(String observedType, int priority, String reception) {
            this.observedType = observedType;
            this.priority = priority;
            this.reception = reception;
        }
    }

    private static class InterceptorInfo {
        final String interceptorClass;
        final int priority;

        InterceptorInfo(String interceptorClass, int priority) {
            this.interceptorClass = interceptorClass;
            this.priority = priority;
        }
    }

    private static class DecoratorInfo {
        final String decoratorClass;
        final String delegateType;

        DecoratorInfo(String decoratorClass, String delegateType) {
            this.decoratorClass = decoratorClass;
            this.delegateType = delegateType;
        }
    }

    private static class RemovedInfo {
        final String description;
        final String type;

        RemovedInfo(String description, String type) {
            this.description = description;
            this.type = type;
        }
    }
}
