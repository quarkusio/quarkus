package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;
import tools.jackson.databind.JsonNode;

public class FaultToleranceShellPage extends BaseExtensionPage {

    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style STRATEGY_STYLE = Style.EMPTY.yellow();

    private List<GuardedMethod> methods = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public FaultToleranceShellPage() {
        super("quarkus-smallrye-fault-tolerance", "Fault Tolerance");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getGuardedMethods");
        List<GuardedMethod> entries = new ArrayList<>();
        JsonNode arr = result.isArray() ? result : result.path("_array");
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                List<String> strategies = new ArrayList<>();
                JsonNode strats = item.path("strategies");
                if (strats.isArray()) {
                    for (JsonNode s : strats) {
                        strategies.add(s.asText(""));
                    }
                } else {
                    // Try common individual strategy fields
                    for (String name : List.of("circuitBreaker", "retry", "timeout", "bulkhead", "fallback",
                            "rateLimit")) {
                        if (item.path(name).asBoolean(false) || item.has(name + "Config")) {
                            strategies.add(name);
                        }
                    }
                }
                entries.add(new GuardedMethod(
                        item.path("method").asText(item.path("name").asText("")),
                        item.path("beanClass").asText(item.path("clazz").asText("")),
                        strategies));
            }
        }
        methods = entries;
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (methods.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No guarded methods found", DIM_STYLE);
            return;
        }

        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < methods.size(); i++) {
            int idx = scrollOffset + i;
            GuardedMethod entry = methods.get(idx);
            boolean selected = idx == selectedIndex;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            int x = area.x() + 1;
            String methodName = entry.beanClass.isEmpty() ? entry.method
                    : shortClassName(entry.beanClass) + "." + entry.method;
            buffer.setString(x, area.y() + i, truncate(methodName, 45), selected ? SELECTED : VALUE_STYLE);
            x += 46;

            if (!entry.strategies.isEmpty()) {
                String strats = String.join(", ", entry.strategies);
                buffer.setString(x, area.y() + i, truncate(strats, area.width() - x + area.x()),
                        selected ? SELECTED : STRATEGY_STYLE);
            }
        }
    }

    private String shortClassName(String fqcn) {
        int dot = fqcn.lastIndexOf('.');
        return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                if (selectedIndex < methods.size() - 1) {
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

    @Override
    protected String getFooterText() {
        return "[j/k] Navigate  " + methods.size() + " guarded methods";
    }

    private record GuardedMethod(String method, String beanClass, List<String> strategies) {
    }
}
