package io.quarkus.resteasy.reactive.server.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.ListView;
import io.quarkus.resteasy.reactive.server.runtime.dev.ui.ResteasyReactiveJsonRPCService;
import io.quarkus.vertx.http.runtime.devmode.ResourceNotFoundData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Shell page for REST endpoints showing all registered routes,
 * endpoint scores, exception mappers, and parameter converter providers.
 */
public class EndpointsShellPage extends BaseExtensionPage {

    @Inject
    ResteasyReactiveJsonRPCService restService;

    @Inject
    ResourceNotFoundData resourceNotFoundData;

    private static final int TAB_ENDPOINTS = 0;
    private static final int TAB_SCORES = 1;
    private static final int TAB_EXCEPTION_MAPPERS = 2;
    private static final int TAB_PARAM_CONVERTERS = 3;

    // Endpoints tab
    private ListView<EndpointInfo> endpointList;
    private String filter = "";
    private boolean filterMode = false;
    private List<EndpointInfo> allEndpoints = new ArrayList<>();

    // Scores tab
    private ListView<ScoreInfo> scoreList;
    private List<ScoreInfo> allScores = new ArrayList<>();
    private int overallScore = 0;

    // Exception Mappers tab
    private ListView<MapperInfo> mapperList;
    private List<MapperInfo> allMappers = new ArrayList<>();

    // Param Converters tab
    private ListView<ConverterInfo> converterList;
    private List<ConverterInfo> allConverters = new ArrayList<>();

    // No-arg constructor for CDI
    public EndpointsShellPage() {
        setTabs("Endpoints", "Endpoint Scores", "Exception Mappers", "Param Converters");
        setTabArrowNavigation(true);

        this.endpointList = new ListView<>(ep -> {
            return padMethod(ep.httpMethod) + " " + ep.path;
        });

        this.scoreList = new ListView<>(
                score -> padMethod(score.httpMethod) + " " + score.path,
                score -> scoreStyle(score.score));

        this.mapperList = new ListView<>(mapper -> {
            return mapper.exceptionName + " \u2192 " + mapper.className + " (priority: " + mapper.priority + ")";
        });

        this.converterList = new ListView<>(converter -> {
            return converter.className + " (priority: " + converter.priority + ")";
        });
    }

    public EndpointsShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    // Method colors removed - buffer rendering uses Style objects instead

    private static Style scoreStyle(int score) {
        if (score >= 100) {
            return Style.create().green();
        }
        if (score >= 66) {
            return Style.create().yellow();
        }
        return Style.create().red();
    }

    private String padMethod(String method) {
        if (method == null)
            return "      ";
        return String.format("%-6s", method.toUpperCase());
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        allEndpoints.clear();
        allScores.clear();
        allMappers.clear();
        allConverters.clear();
        redraw();

        try {
            // Load endpoints from ResourceNotFoundData
            JsonObject allEndpointsJson = resourceNotFoundData.getAllEndpoints();
            if (allEndpointsJson != null) {
                JsonArray resourceEndpoints = allEndpointsJson.getJsonArray("Resource Endpoints");
                if (resourceEndpoints != null) {
                    for (int i = 0; i < resourceEndpoints.size(); i++) {
                        JsonObject endpoint = resourceEndpoints.getJsonObject(i);
                        String uri = endpoint.getString("uri", "");
                        String description = endpoint.getString("description", "");

                        String httpMethod = "GET";
                        String className = null;

                        if (description != null && !description.isEmpty()) {
                            String[] parts = description.split("[\\s(]", 2);
                            if (parts.length > 0 && isHttpMethod(parts[0])) {
                                httpMethod = parts[0].toUpperCase();
                            }

                            int javaIdx = description.indexOf("(java:");
                            if (javaIdx != -1) {
                                int closeIdx = description.indexOf(")", javaIdx);
                                if (closeIdx > javaIdx) {
                                    className = description.substring(javaIdx + 6, closeIdx);
                                }
                            }
                        }

                        if (!uri.isEmpty()) {
                            allEndpoints.add(new EndpointInfo(httpMethod, uri, className));
                        }
                    }
                }
                allEndpoints.sort((a, b) -> {
                    int cmp = a.path.compareToIgnoreCase(b.path);
                    if (cmp != 0)
                        return cmp;
                    return a.httpMethod.compareTo(b.httpMethod);
                });
            }

            // Load scores
            try {
                JsonObject scoresJson = restService.getEndpointScores();
                if (scoresJson != null) {
                    overallScore = scoresJson.getInteger("score", 0);
                    JsonArray endpoints = scoresJson.getJsonArray("endpoints");
                    if (endpoints != null) {
                        for (int i = 0; i < endpoints.size(); i++) {
                            JsonObject endpoint = endpoints.getJsonObject(i);
                            allScores.add(new ScoreInfo(
                                    endpoint.getString("httpMethod", "GET"),
                                    endpoint.getString("fullPath", ""),
                                    endpoint.getString("className", ""),
                                    endpoint.getInteger("score", 0)));
                        }
                    }
                    allScores.sort((a, b) -> Integer.compare(a.score, b.score));
                }
            } catch (Exception ignored) {
                // Scores might not be available yet
            }

            // Load exception mappers
            try {
                JsonArray mappersJson = restService.getExceptionMappers();
                if (mappersJson != null) {
                    for (int i = 0; i < mappersJson.size(); i++) {
                        JsonObject mapper = mappersJson.getJsonObject(i);
                        allMappers.add(new MapperInfo(
                                mapper.getString("name", ""),
                                mapper.getString("className", ""),
                                mapper.getInteger("priority", 0)));
                    }
                    allMappers.sort((a, b) -> Integer.compare(a.priority, b.priority));
                }
            } catch (Exception ignored) {
                // Continue
            }

            // Load param converters
            try {
                JsonArray convertersJson = restService.getParamConverterProviders();
                if (convertersJson != null) {
                    for (int i = 0; i < convertersJson.size(); i++) {
                        JsonObject converter = convertersJson.getJsonObject(i);
                        allConverters.add(new ConverterInfo(
                                converter.getString("className", ""),
                                converter.getInteger("priority", 0)));
                    }
                    allConverters.sort((a, b) -> Integer.compare(a.priority, b.priority));
                }
            } catch (Exception ignored) {
                // Continue
            }

            loading = false;
            updateLists();
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load endpoints: " + ex.getMessage();
            redraw();
        }
    }

    private boolean isHttpMethod(String s) {
        if (s == null)
            return false;
        switch (s.toUpperCase()) {
            case "GET":
            case "POST":
            case "PUT":
            case "DELETE":
            case "PATCH":
            case "HEAD":
            case "OPTIONS":
                return true;
            default:
                return false;
        }
    }

    private void updateLists() {
        applyFilter();
        scoreList.setItems(allScores);
        mapperList.setItems(allMappers);
        converterList.setItems(allConverters);
    }

    private void applyFilter() {
        if (filter.isEmpty()) {
            endpointList.setItems(allEndpoints);
        } else {
            String lowerFilter = filter.toLowerCase();
            List<EndpointInfo> filtered = new ArrayList<>();
            for (EndpointInfo ep : allEndpoints) {
                if (ep.path.toLowerCase().contains(lowerFilter) ||
                        ep.httpMethod.toLowerCase().contains(lowerFilter) ||
                        (ep.className != null && ep.className.toLowerCase().contains(lowerFilter))) {
                    filtered.add(ep);
                }
            }
            endpointList.setItems(filtered);
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
                case TAB_ENDPOINTS:
                    renderEndpointsTab(buffer, row);
                    break;
                case TAB_SCORES:
                    renderScoresTab(buffer, row);
                    break;
                case TAB_EXCEPTION_MAPPERS:
                    renderMappersTab(buffer, row);
                    break;
                case TAB_PARAM_CONVERTERS:
                    renderConvertersTab(buffer, row);
                    break;
            }
        }

        String footer = getCurrentTabIndex() == TAB_ENDPOINTS ? "[/] Filter" : "";
        renderFooter(buffer, filterMode ? "[Enter] Apply  [Esc] Cancel" : footer);
    }

    private void renderEndpointsTab(Buffer buffer, int row) {
        // Filter indicator
        if (filterMode) {
            buffer.setString(1, row, "Filter: " + filter + "_", Style.create().yellow());
        } else if (!filter.isEmpty()) {
            buffer.setString(1, row, "Filtered: " + filter, Style.create().gray());
        }
        row++;

        row++;

        if (endpointList.isEmpty()) {
            buffer.setString(1, row, "No endpoints found", Style.create().gray());
        } else {
            endpointList.setVisibleRows(height - row - 5);
            endpointList.setWidth(width - 4);
            endpointList.render(buffer, row, 2);

        }
    }

    private void renderScoresTab(Buffer buffer, int row) {
        buffer.setString(1, row, "Overall Score: " + overallScore, Style.create().cyan());
        row++;

        row++;

        if (scoreList.isEmpty()) {
            buffer.setString(1, row, "No endpoint scores found", Style.create().gray());
        } else {
            scoreList.setVisibleRows(height - row - 5);
            scoreList.setWidth(width - 4);
            scoreList.render(buffer, row, 2);

        }
    }

    private void renderMappersTab(Buffer buffer, int row) {
        row++;

        if (mapperList.isEmpty()) {
            buffer.setString(1, row, "No exception mappers found", Style.create().gray());
        } else {
            mapperList.setVisibleRows(height - row - 5);
            mapperList.setWidth(width - 4);
            mapperList.render(buffer, row, 2);

        }
    }

    private void renderConvertersTab(Buffer buffer, int row) {
        row++;

        if (converterList.isEmpty()) {
            buffer.setString(1, row, "No parameter converters found", Style.create().gray());
        } else {
            converterList.setVisibleRows(height - row - 5);
            converterList.setWidth(width - 4);
            converterList.render(buffer, row, 2);

        }
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        // Filter mode intercepts all keys
        if (filterMode) {
            return handleFilterKey(key);
        }

        // Enter filter mode with '/'
        if (key == '/' && getCurrentTabIndex() == TAB_ENDPOINTS) {
            filterMode = true;
            redraw();
            return true;
        }

        // List navigation for current tab
        boolean handled = false;
        switch (getCurrentTabIndex()) {
            case TAB_ENDPOINTS:
                handled = endpointList.handleKey(key);
                break;
            case TAB_SCORES:
                handled = scoreList.handleKey(key);
                break;
            case TAB_EXCEPTION_MAPPERS:
                handled = mapperList.handleKey(key);
                break;
            case TAB_PARAM_CONVERTERS:
                handled = converterList.handleKey(key);
                break;
        }

        if (handled) {
            redraw();
            return true;
        }

        // Delegate tab switching, Escape, Refresh to base class
        return super.handleKey(key);
    }

    private boolean handleFilterKey(int key) {
        switch (key) {
            case KeyCode.ENTER:
                filterMode = false;
                applyFilter();
                redraw();
                return true;

            case KeyCode.ESCAPE:
                filterMode = false;
                filter = "";
                applyFilter();
                redraw();
                return true;

            case KeyCode.BACKSPACE:
                if (!filter.isEmpty()) {
                    filter = filter.substring(0, filter.length() - 1);
                    redraw();
                }
                return true;

            default:
                if (key >= 32 && key < 127) {
                    filter += (char) key;
                    redraw();
                }
                return true;
        }
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        int visibleRows = height - 12;
        endpointList.setVisibleRows(visibleRows);
        endpointList.setWidth(width - 4);
        scoreList.setVisibleRows(visibleRows);
        scoreList.setWidth(width - 4);
        mapperList.setVisibleRows(visibleRows);
        mapperList.setWidth(width - 4);
        converterList.setVisibleRows(visibleRows);
        converterList.setWidth(width - 4);
    }

    @Override
    protected void renderPanelContent(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight) {
        int row = startRow;

        if (loading) {
            return;
        }

        row++;

        if (allEndpoints.isEmpty()) {
            return;
        }

        int maxEndpoints = Math.min(allEndpoints.size(), panelHeight - 3);
        for (int i = 0; i < maxEndpoints; i++) {
            EndpointInfo ep = allEndpoints.get(i);
            buffer.setString(startCol, row, truncate(padMethod(ep.httpMethod) + " " + ep.path, panelWidth - 2), Style.EMPTY);
            row++;
        }

        if (allEndpoints.size() > maxEndpoints) {
            buffer.setString(startCol, row, "+" + (allEndpoints.size() - maxEndpoints) + " more...", Style.create().gray());
        }
    }

    // Data classes
    private static class EndpointInfo {
        final String httpMethod;
        final String path;
        final String className;

        EndpointInfo(String httpMethod, String path, String className) {
            this.httpMethod = httpMethod != null ? httpMethod : "GET";
            this.path = path != null ? path : "/";
            this.className = className;
        }
    }

    private static class ScoreInfo {
        final String httpMethod;
        final String path;
        final String className;
        final int score;

        ScoreInfo(String httpMethod, String path, String className, int score) {
            this.httpMethod = httpMethod != null ? httpMethod : "GET";
            this.path = path != null ? path : "/";
            this.className = className;
            this.score = score;
        }
    }

    private static class MapperInfo {
        final String exceptionName;
        final String className;
        final int priority;

        MapperInfo(String exceptionName, String className, int priority) {
            this.exceptionName = exceptionName != null ? exceptionName : "";
            this.className = className != null ? className : "";
            this.priority = priority;
        }
    }

    private static class ConverterInfo {
        final String className;
        final int priority;

        ConverterInfo(String className, int priority) {
            this.className = className != null ? className : "";
            this.priority = priority;
        }
    }
}
