package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.deployment.tui.widgets.TableView;
import tools.jackson.databind.JsonNode;

public class WebDependencyLocatorShellPage extends BaseExtensionPage {

    private final TableView<WebDep> tableView = new TableView<>();
    private final List<WebDep> deps = new ArrayList<>();

    public WebDependencyLocatorShellPage() {
        super("quarkus-web-dependency-locator", "Web Dependencies");
        tableView.addColumn("Name", d -> d.name, 20);
        tableView.addColumn("Version", d -> d.version, 15);
    }

    @Override
    public void loadData() {
        JsonNode result = getBuildTimeDataAsJson("webDependencyLibraries");
        deps.clear();

        if (result.isArray()) {
            for (JsonNode dep : result) {
                String name = dep.path("name").asText(dep.asText(""));
                String version = dep.path("version").asText("");
                deps.add(new WebDep(name, version));
            }
        } else if (result.isObject()) {
            var fields = result.properties().iterator();
            while (fields.hasNext()) {
                var entry = fields.next();
                deps.add(new WebDep(entry.getKey(), entry.getValue().asText("")));
            }
        }

        tableView.setItems(deps);
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (deps.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No web dependencies found", DIM_STYLE);
            return;
        }

        buffer.setString(area.x() + 1, area.y(), deps.size() + " dependencies", DIM_STYLE);
        tableView.setVisibleRows(area.height() - 3);
        tableView.setWidth(area.width() - 2);
        tableView.render(buffer, area.y() + 1, area.x() + 1);
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                tableView.moveDown();
                ctx.requestRedraw();
                return true;
            case 'k':
            case KeyCode.UP:
                tableView.moveUp();
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_DOWN:
                tableView.pageDown();
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_UP:
                tableView.pageUp();
                ctx.requestRedraw();
                return true;
            default:
                return false;
        }
    }

    @Override
    protected String getFooterText() {
        return "[j/k] Navigate";
    }

    private static class WebDep {
        final String name;
        final String version;

        WebDep(String name, String version) {
            this.name = name;
            this.version = version;
        }
    }
}
