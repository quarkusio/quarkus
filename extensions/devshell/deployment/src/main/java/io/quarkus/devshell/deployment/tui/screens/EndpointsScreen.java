package io.quarkus.devshell.deployment.tui.screens;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.Screen;
import tools.jackson.databind.JsonNode;

public class EndpointsScreen implements Screen {

    private static final Style METHOD_GET = Style.EMPTY.green().bold();
    private static final Style METHOD_POST = Style.EMPTY.yellow().bold();
    private static final Style METHOD_PUT = Style.EMPTY.blue().bold();
    private static final Style METHOD_DELETE = Style.EMPTY.red().bold();
    private static final Style METHOD_OTHER = Style.EMPTY.magenta().bold();
    private static final Style DIM = Style.EMPTY.gray();
    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style NORMAL = Style.EMPTY.white();
    private static final Style TAB_ACTIVE = Style.EMPTY.cyan().bold();
    private static final Style TAB_INACTIVE = Style.EMPTY.gray();

    private AppContext ctx;
    private final List<EndpointItem> restEndpoints = new ArrayList<>();
    private final List<EndpointItem> staticResources = new ArrayList<>();
    private final List<EndpointItem> additionalEndpoints = new ArrayList<>();
    private int activeTab = 0;
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    @Override
    public String getTitle() {
        return "Endpoints";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        loadData();
    }

    private void loadData() {
        restEndpoints.clear();
        staticResources.clear();
        additionalEndpoints.clear();

        try {
            JsonNode result = ctx.getJsonRpcClient().call("devui-endpoints", "getAllEndpoints");
            parseEndpoints(result);
        } catch (Exception e) {
            // no endpoint data available
        }
    }

    private void parseEndpoints(JsonNode result) {
        addEndpoints(result, "Resource Endpoints", restEndpoints);
        addEndpoints(result, "Servlet mappings", restEndpoints);
        addEndpoints(result, "Static resources", staticResources);
        addEndpoints(result, "Additional endpoints", additionalEndpoints);
    }

    private void addEndpoints(JsonNode result, String key, List<EndpointItem> target) {
        JsonNode arr = result.path(key);
        if (arr.isArray()) {
            for (JsonNode ep : arr) {
                target.add(new EndpointItem(
                        ep.path("uri").asText(""),
                        ep.path("description").asText("")));
            }
        }
    }

    private List<EndpointItem> activeList() {
        return switch (activeTab) {
            case 1 -> staticResources;
            case 2 -> additionalEndpoints;
            default -> restEndpoints;
        };
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        var areas = Layout.vertical()
                .constraints(Constraint.length(1), Constraint.fill(), Constraint.length(1))
                .split(area);

        renderTabs(areas.get(0), buffer);
        renderList(areas.get(1), buffer);
        renderFooter(areas.get(2), buffer);
    }

    private void renderTabs(Rect area, Buffer buffer) {
        buffer.setLine(area.x(), area.y(), Line.from(
                Span.styled(" REST", activeTab == 0 ? TAB_ACTIVE : TAB_INACTIVE),
                Span.styled(" (" + restEndpoints.size() + ") ", activeTab == 0 ? TAB_ACTIVE : TAB_INACTIVE),
                Span.styled("| Static", activeTab == 1 ? TAB_ACTIVE : TAB_INACTIVE),
                Span.styled(" (" + staticResources.size() + ") ", activeTab == 1 ? TAB_ACTIVE : TAB_INACTIVE),
                Span.styled("| Additional", activeTab == 2 ? TAB_ACTIVE : TAB_INACTIVE),
                Span.styled(" (" + additionalEndpoints.size() + ") ", activeTab == 2 ? TAB_ACTIVE : TAB_INACTIVE)));
    }

    private void renderList(Rect area, Buffer buffer) {
        List<EndpointItem> list = activeList();
        int visibleRows = area.height();

        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < list.size(); i++) {
            int idx = scrollOffset + i;
            EndpointItem item = list.get(idx);
            boolean selected = idx == selectedIndex;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            int x = area.x() + 1;
            if (activeTab == 0) {
                String method = extractMethod(item.description);
                Style methodStyle = methodColor(method);
                String paddedMethod = String.format("%-7s", method);
                buffer.setString(x, area.y() + i, paddedMethod, selected ? SELECTED : methodStyle);
                x += 8;
            }

            Style pathStyle = selected ? SELECTED : NORMAL;
            buffer.setString(x, area.y() + i, truncate(item.uri, area.width() - (x - area.x())), pathStyle);
        }

        if (list.isEmpty()) {
            buffer.setString(area.x() + 2, area.y() + 1, "No endpoints found", DIM);
        }
    }

    private void renderFooter(Rect area, Buffer buffer) {
        buffer.setLine(area.x(), area.y(), Line.from(
                Span.styled(" Enter", Style.EMPTY.cyan()), Span.styled(" Details  ", DIM),
                Span.styled("Tab", Style.EMPTY.cyan()), Span.styled(" Switch  ", DIM),
                Span.styled("R", Style.EMPTY.cyan()), Span.styled(" Refresh  ", DIM),
                Span.styled("ESC", Style.EMPTY.cyan()), Span.styled(" Back", DIM)));
    }

    @Override
    public boolean handleKey(int[] keys) {
        if (keys.length == 1) {
            switch (keys[0]) {
                case '\t':
                    activeTab = (activeTab + 1) % 3;
                    selectedIndex = 0;
                    scrollOffset = 0;
                    ctx.requestRedraw();
                    return true;
                case '\r':
                case '\n':
                    List<EndpointItem> list = activeList();
                    if (!list.isEmpty() && selectedIndex < list.size()) {
                        ctx.navigateTo(new EndpointDetailScreen(list.get(selectedIndex)));
                    }
                    return true;
                case 'j':
                    return moveDown();
                case 'k':
                    return moveUp();
                case 'r':
                case 'R':
                    loadData();
                    ctx.requestRedraw();
                    return true;
            }
        }
        if (keys.length == 3 && keys[0] == 27 && keys[1] == '[') {
            if (keys[2] == 'A')
                return moveUp();
            if (keys[2] == 'B')
                return moveDown();
        }
        return false;
    }

    private boolean moveUp() {
        if (selectedIndex > 0) {
            selectedIndex--;
            ctx.requestRedraw();
            return true;
        }
        return false;
    }

    private boolean moveDown() {
        if (selectedIndex < activeList().size() - 1) {
            selectedIndex++;
            ctx.requestRedraw();
            return true;
        }
        return false;
    }

    private static String extractMethod(String description) {
        if (description == null || description.isEmpty())
            return "GET";
        int space = description.indexOf(' ');
        int paren = description.indexOf('(');
        int end = space > 0 ? space : (paren > 0 ? paren : description.length());
        String method = description.substring(0, Math.min(end, 10)).toUpperCase();
        if (method.matches("GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS"))
            return method;
        return "GET";
    }

    private static Style methodColor(String method) {
        return switch (method) {
            case "GET" -> METHOD_GET;
            case "POST" -> METHOD_POST;
            case "PUT" -> METHOD_PUT;
            case "DELETE" -> METHOD_DELETE;
            default -> METHOD_OTHER;
        };
    }

    private static String truncate(String s, int maxLen) {
        if (maxLen <= 0)
            return "";
        if (s.length() <= maxLen)
            return s;
        return s.substring(0, maxLen - 1) + "~";
    }

    record EndpointItem(String uri, String description) {
    }
}
