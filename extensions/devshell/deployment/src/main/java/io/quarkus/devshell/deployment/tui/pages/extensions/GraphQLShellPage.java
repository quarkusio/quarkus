package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;
import tools.jackson.databind.JsonNode;

public class GraphQLShellPage extends BaseExtensionPage {

    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Pattern TYPE_PATTERN = Pattern.compile("^\\s*(?:type|input|enum|interface|union|scalar)\\s+(\\w+)");

    private List<String> schemaLines = List.of();
    private List<String> typeNames = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public GraphQLShellPage() {
        super("quarkus-smallrye-graphql", "GraphQL");
        setTabs("Schema", "Types");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getGraphQLSchema");
        String schema = result.isTextual() ? result.asText("") : result.path("_value").asText("");

        List<String> lines = new ArrayList<>();
        List<String> types = new ArrayList<>();
        if (!schema.isEmpty()) {
            for (String line : schema.split("\n")) {
                lines.add(line);
                Matcher m = TYPE_PATTERN.matcher(line);
                if (m.find()) {
                    types.add(m.group(1));
                }
            }
        }
        schemaLines = lines;
        typeNames = types;
    }

    @Override
    protected void onTabChanged(int newTab) {
        selectedIndex = 0;
        scrollOffset = 0;
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (getActiveTab() == 0) {
            renderSchema(area, buffer);
        } else {
            renderTypes(area, buffer);
        }
    }

    private void renderSchema(Rect area, Buffer buffer) {
        if (schemaLines.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No schema available", DIM_STYLE);
            return;
        }

        int visibleRows = area.height();
        if (scrollOffset > schemaLines.size() - visibleRows) {
            scrollOffset = Math.max(0, schemaLines.size() - visibleRows);
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < schemaLines.size(); i++) {
            int idx = scrollOffset + i;
            String line = schemaLines.get(idx);
            buffer.setString(area.x() + 1, area.y() + i, truncate(line, area.width() - 2), VALUE_STYLE);
        }
    }

    private void renderTypes(Rect area, Buffer buffer) {
        if (typeNames.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No types found", DIM_STYLE);
            return;
        }

        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < typeNames.size(); i++) {
            int idx = scrollOffset + i;
            boolean selected = idx == selectedIndex;
            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }
            buffer.setString(area.x() + 2, area.y() + i, truncate(typeNames.get(idx), area.width() - 3),
                    selected ? SELECTED : VALUE_STYLE);
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        if (getActiveTab() == 0) {
            // Schema tab: scroll only
            switch (key) {
                case 'j':
                case KeyCode.DOWN:
                    if (scrollOffset < schemaLines.size() - 1) {
                        scrollOffset++;
                        ctx.requestRedraw();
                    }
                    return true;
                case 'k':
                case KeyCode.UP:
                    if (scrollOffset > 0) {
                        scrollOffset--;
                        ctx.requestRedraw();
                    }
                    return true;
                default:
                    return false;
            }
        } else {
            // Types tab: selection
            switch (key) {
                case 'j':
                case KeyCode.DOWN:
                    if (selectedIndex < typeNames.size() - 1) {
                        selectedIndex++;
                        ctx.requestRedraw();
                    }
                    return true;
                case 'k':
                case KeyCode.UP:
                    if (selectedIndex > 0) {
                        selectedIndex--;
                        ctx.requestRedraw();
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    @Override
    protected String getFooterText() {
        if (getActiveTab() == 0) {
            return "[j/k] Scroll  " + schemaLines.size() + " lines";
        }
        return "[j/k] Navigate  " + typeNames.size() + " types";
    }
}
