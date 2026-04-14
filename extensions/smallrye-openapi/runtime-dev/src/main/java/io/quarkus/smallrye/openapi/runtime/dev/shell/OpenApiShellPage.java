package io.quarkus.smallrye.openapi.runtime.dev.shell;

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
import io.quarkus.smallrye.openapi.runtime.dev.OpenApiJsonRpcService;
import io.vertx.core.json.JsonObject;

/**
 * Shell page for SmallRye OpenAPI extension.
 * Displays API info, endpoints, and schemas from the OpenAPI specification.
 */
public class OpenApiShellPage extends BaseExtensionPage {

    @Inject
    OpenApiJsonRpcService openApiService;

    private static final int TAB_INFO = 0;
    private static final int TAB_ENDPOINTS = 1;
    private static final int TAB_SCHEMAS = 2;

    private ListView<Endpoint> endpointList;
    private ListView<String> schemaList;
    private final KeyValuePanel infoPanel = new KeyValuePanel("API Info");

    // Parsed OpenAPI data
    private String apiTitle = "";
    private String apiVersion = "";
    private String apiDescription = "";
    private List<Endpoint> endpoints = new ArrayList<>();
    private List<String> schemas = new ArrayList<>();

    // No-arg constructor for CDI
    public OpenApiShellPage() {
        setTabs("INFO", "ENDPOINTS", "SCHEMAS");
        setTabArrowNavigation(true);
        this.endpointList = new ListView<>(ep -> {
            String label = String.format("%-7s", ep.method) + " " + ep.path;
            if (ep.summary != null && !ep.summary.isEmpty()) {
                label += " - " + ep.summary;
            }
            return label;
        });
        this.schemaList = new ListView<>(s -> s);
    }

    public OpenApiShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        endpoints.clear();
        schemas.clear();
        redraw();

        try {
            String schemaJson = openApiService.getOpenAPISchema();
            loading = false;

            if (schemaJson == null || schemaJson.isEmpty()) {
                error = "Empty OpenAPI schema";
                redraw();
                return;
            }

            JsonObject root = new JsonObject(schemaJson);

            // Parse info section
            JsonObject info = root.getJsonObject("info");
            if (info != null) {
                apiTitle = info.getString("title");
                apiVersion = info.getString("version");
                apiDescription = info.getString("description");
            }

            // Parse paths
            JsonObject paths = root.getJsonObject("paths");
            if (paths != null) {
                parsePaths(paths);
            }

            // Parse schemas (components/schemas)
            JsonObject components = root.getJsonObject("components");
            if (components != null) {
                JsonObject schemasObj = components.getJsonObject("schemas");
                if (schemasObj != null) {
                    parseSchemas(schemasObj);
                }
            }

            endpointList.setItems(endpoints);
            schemaList.setItems(schemas);
            buildInfoPanel();
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load OpenAPI schema: " + ex.getMessage();
            redraw();
        }
    }

    private void parsePaths(JsonObject pathsObj) {
        for (String path : pathsObj.fieldNames()) {
            JsonObject pathItem = pathsObj.getJsonObject(path);
            if (pathItem == null) {
                continue;
            }

            // Extract HTTP methods from the path object
            for (String method : new String[] { "get", "post", "put", "delete", "patch", "options", "head" }) {
                JsonObject methodObj = pathItem.getJsonObject(method);
                if (methodObj != null) {
                    String summary = methodObj.getString("summary");
                    String operationId = methodObj.getString("operationId");
                    endpoints.add(new Endpoint(
                            method.toUpperCase(),
                            path,
                            summary != null ? summary : (operationId != null ? operationId : "")));
                }
            }
        }
    }

    private void parseSchemas(JsonObject schemasObj) {
        for (String schemaName : schemasObj.fieldNames()) {
            schemas.add(schemaName);
        }
    }

    private void buildInfoPanel() {
        infoPanel.clear();
        infoPanel.add("Title", apiTitle != null ? apiTitle : "N/A");
        infoPanel.add("Version", apiVersion != null ? apiVersion : "N/A");
        infoPanel.addIfPresent("Description", apiDescription);
        infoPanel.addBlank();
        infoPanel.addStyled("Endpoints", String.valueOf(endpoints.size()), Style.create().gray());
        infoPanel.addStyled("Schemas", String.valueOf(schemas.size()), Style.create().gray());
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
                case TAB_ENDPOINTS:
                    renderEndpointsTab(buffer, row);
                    break;
                case TAB_SCHEMAS:
                    renderSchemasTab(buffer, row);
                    break;
            }
        }

        renderFooter(buffer, "");
    }

    private void renderInfoTab(Buffer buffer, int row) {
        infoPanel.render(buffer, row, 2, width - 4);
    }

    private void renderEndpointsTab(Buffer buffer, int row) {
        row++;

        if (endpoints.isEmpty()) {
            buffer.setString(1, row, "No endpoints found", Style.create().gray());
        } else {
            endpointList.setVisibleRows(height - row - 4);
            endpointList.setWidth(width - 4);
            endpointList.render(buffer, row, 2);
        }
    }

    private void renderSchemasTab(Buffer buffer, int row) {
        row++;

        if (schemas.isEmpty()) {
            buffer.setString(1, row, "No schemas found", Style.create().gray());
        } else {
            schemaList.setVisibleRows(height - row - 4);
            schemaList.setWidth(width - 4);
            schemaList.render(buffer, row, 2);
        }
    }

    @Override
    public boolean handleKey(int key) {
        // Let lists handle navigation on their respective tabs
        if (getCurrentTabIndex() == TAB_ENDPOINTS && endpointList.handleKey(key)) {
            redraw();
            return true;
        }
        if (getCurrentTabIndex() == TAB_SCHEMAS && schemaList.handleKey(key)) {
            redraw();
            return true;
        }

        return super.handleKey(key);
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        endpointList.setVisibleRows(height - 10);
        endpointList.setWidth(width - 4);
        schemaList.setVisibleRows(height - 10);
        schemaList.setWidth(width - 4);
    }

    // Data class for endpoints
    private static class Endpoint {
        final String method;
        final String path;
        final String summary;

        Endpoint(String method, String path, String summary) {
            this.method = method;
            this.path = path;
            this.summary = summary;
        }
    }
}
