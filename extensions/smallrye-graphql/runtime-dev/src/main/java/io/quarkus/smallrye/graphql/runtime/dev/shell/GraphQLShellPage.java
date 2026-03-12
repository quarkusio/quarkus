package io.quarkus.smallrye.graphql.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.runtime.tui.widgets.ListView;
import io.quarkus.smallrye.graphql.runtime.dev.GraphQLJsonRpcService;

/**
 * Shell page for SmallRye GraphQL extension.
 * Displays schema information, types, and operations.
 */
public class GraphQLShellPage extends BaseExtensionPage {

    @Inject
    GraphQLJsonRpcService graphQLService;

    private static final int TAB_INFO = 0;
    private static final int TAB_TYPES = 1;
    private static final int TAB_OPERATIONS = 2;

    private ListView<SchemaType> typeList;
    private ListView<Operation> operationList;
    private final KeyValuePanel infoPanel = new KeyValuePanel("GraphQL Schema");

    // Parsed schema data
    private List<SchemaType> types = new ArrayList<>();
    private List<Operation> operations = new ArrayList<>();

    // No-arg constructor for CDI
    public GraphQLShellPage() {
        setTabs("Info", "Types", "Operations");
        setTabArrowNavigation(true);

        this.typeList = new ListView<>(t -> {
            return String.format("%-10s", t.kind) + " " + t.name;
        });
        this.operationList = new ListView<>(op -> {
            return String.format("%-12s", op.operationType) + " " + op.name;
        });
    }

    public GraphQLShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        types.clear();
        operations.clear();
        redraw();

        try {
            String schema = graphQLService.getGraphQLSchema();
            loading = false;

            if (schema == null || schema.isEmpty()) {
                error = "Empty GraphQL schema";
                redraw();
                return;
            }

            parseTypes(schema);
            parseOperations(schema);

            // Build info panel
            int queryCount = 0, mutationCount = 0, subscriptionCount = 0;
            for (Operation op : operations) {
                switch (op.operationType) {
                    case "Query":
                        queryCount++;
                        break;
                    case "Mutation":
                        mutationCount++;
                        break;
                    case "Subscription":
                        subscriptionCount++;
                        break;
                }
            }

            int typeCount = 0, inputCount = 0, enumCount = 0, interfaceCount = 0;
            for (SchemaType t : types) {
                switch (t.kind) {
                    case "type":
                        typeCount++;
                        break;
                    case "input":
                        inputCount++;
                        break;
                    case "enum":
                        enumCount++;
                        break;
                    case "interface":
                        interfaceCount++;
                        break;
                }
            }

            infoPanel.clear();
            infoPanel.add("Queries", String.valueOf(queryCount));
            infoPanel.add("Mutations", String.valueOf(mutationCount));
            infoPanel.add("Subscriptions", String.valueOf(subscriptionCount));
            infoPanel.addBlank();
            infoPanel.add("Total Types", String.valueOf(types.size()));
            if (typeCount > 0) {
                infoPanel.addStyled("type", String.valueOf(typeCount), Style.create().cyan());
            }
            if (inputCount > 0) {
                infoPanel.addStyled("input", String.valueOf(inputCount), Style.create().yellow());
            }
            if (enumCount > 0) {
                infoPanel.addStyled("enum", String.valueOf(enumCount), Style.create().magenta());
            }
            if (interfaceCount > 0) {
                infoPanel.addStyled("interface", String.valueOf(interfaceCount), Style.create().green());
            }

            typeList.setItems(types);
            operationList.setItems(operations);
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load GraphQL schema: " + ex.getMessage();
            redraw();
        }
    }

    private void parseTypes(String schema) {
        // Parse type definitions: type, input, enum, interface, union, scalar
        // Pattern: (type|input|enum|interface|union|scalar) TypeName
        Pattern typePattern = Pattern.compile(
                "^\\s*(type|input|enum|interface|union|scalar)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*[{@\\(]?",
                Pattern.MULTILINE);

        Matcher matcher = typePattern.matcher(schema);
        while (matcher.find()) {
            String kind = matcher.group(1);
            String name = matcher.group(2);

            // Skip built-in types and operation types
            if (name.startsWith("__") || name.equals("Query") ||
                    name.equals("Mutation") || name.equals("Subscription")) {
                continue;
            }

            types.add(new SchemaType(kind, name));
        }
    }

    private void parseOperations(String schema) {
        // Find Query, Mutation, Subscription types and extract their fields
        parseOperationType(schema, "Query");
        parseOperationType(schema, "Mutation");
        parseOperationType(schema, "Subscription");
    }

    private void parseOperationType(String schema, String typeName) {
        // Find the type block: type Query { ... }
        Pattern blockPattern = Pattern.compile(
                "type\\s+" + typeName + "\\s*\\{([^}]*)\\}",
                Pattern.DOTALL);

        Matcher blockMatcher = blockPattern.matcher(schema);
        if (blockMatcher.find()) {
            String block = blockMatcher.group(1);

            // Extract field names (operations): fieldName or fieldName(args): ReturnType
            Pattern fieldPattern = Pattern.compile(
                    "^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*[\\(:]",
                    Pattern.MULTILINE);

            Matcher fieldMatcher = fieldPattern.matcher(block);
            while (fieldMatcher.find()) {
                String fieldName = fieldMatcher.group(1);
                operations.add(new Operation(typeName, fieldName));
            }
        }
    }

    // Color methods removed - buffer rendering uses Style objects instead

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
                case TAB_TYPES:
                    renderTypesTab(buffer, row);
                    break;
                case TAB_OPERATIONS:
                    renderOperationsTab(buffer, row);
                    break;
            }
        }

        renderFooter(buffer, "");
    }

    private void renderInfoTab(Buffer buffer, int row) {
        infoPanel.render(buffer, row, 2, width - 4);
    }

    private void renderTypesTab(Buffer buffer, int row) {
        row++;

        if (types.isEmpty()) {
            buffer.setString(1, row, "No types found", Style.create().gray());
        } else {
            typeList.setVisibleRows(height - row - 4);
            typeList.setWidth(width - 4);
            typeList.render(buffer, row, 2);
        }
    }

    private void renderOperationsTab(Buffer buffer, int row) {
        row++;

        if (operations.isEmpty()) {
            buffer.setString(1, row, "No operations found", Style.create().gray());
        } else {
            operationList.setVisibleRows(height - row - 4);
            operationList.setWidth(width - 4);
            operationList.render(buffer, row, 2);
        }
    }

    @Override
    public boolean handleKey(int key) {
        // Let base class handle tab navigation, refresh, escape
        if (super.handleKey(key)) {
            return true;
        }

        // Let lists handle navigation on their respective tabs
        if (getCurrentTabIndex() == TAB_TYPES && typeList.handleKey(key)) {
            redraw();
            return true;
        }
        if (getCurrentTabIndex() == TAB_OPERATIONS && operationList.handleKey(key)) {
            redraw();
            return true;
        }

        return false;
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        typeList.setVisibleRows(height - 10);
        typeList.setWidth(width - 4);
        operationList.setVisibleRows(height - 10);
        operationList.setWidth(width - 4);
    }

    // Data classes
    private static class SchemaType {
        final String kind;
        final String name;

        SchemaType(String kind, String name) {
            this.kind = kind;
            this.name = name;
        }
    }

    private static class Operation {
        final String operationType;
        final String name;

        Operation(String operationType, String name) {
            this.operationType = operationType;
            this.name = name;
        }
    }
}
