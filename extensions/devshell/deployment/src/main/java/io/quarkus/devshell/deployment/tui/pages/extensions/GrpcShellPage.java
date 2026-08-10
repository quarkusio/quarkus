package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.deployment.tui.widgets.ListView;
import tools.jackson.databind.JsonNode;

public class GrpcShellPage extends BaseExtensionPage {

    private final ListView<GrpcService> listView = new ListView<>(s -> s.name);
    private final List<GrpcService> services = new ArrayList<>();

    public GrpcShellPage() {
        super("quarkus-grpc", "gRPC Services");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getGrpcServices");
        services.clear();

        if (result.isArray()) {
            for (JsonNode svc : result) {
                String name = svc.path("name").asText(svc.path("serviceName").asText(""));
                String status = svc.path("status").asText("");
                List<String> methods = new ArrayList<>();
                JsonNode methodsNode = svc.path("methods");
                if (methodsNode.isArray()) {
                    for (JsonNode m : methodsNode) {
                        methods.add(m.asText(m.path("name").asText("")));
                    }
                }
                services.add(new GrpcService(name, status, methods));
            }
        }
        listView.setItems(services);
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (services.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No gRPC services found", DIM_STYLE);
            return;
        }

        var areas = Layout.horizontal()
                .constraints(Constraint.percentage(40), Constraint.percentage(60))
                .split(area);

        // Service list
        Rect listArea = areas.get(0);
        listView.setVisibleRows(listArea.height());
        listView.setWidth(listArea.width() - 1);
        listView.render(buffer, listArea.y(), listArea.x() + 1);

        // Detail panel
        Rect detailArea = areas.get(1);
        int x = detailArea.x() + 1;
        int y = detailArea.y();
        int w = detailArea.width() - 2;

        GrpcService selected = listView.getSelectedItem();
        if (selected != null) {
            buffer.setString(x, y, selected.name, HEADER_STYLE);
            y++;
            if (!selected.status.isEmpty()) {
                buffer.setString(x, y, "Status: ", LABEL_STYLE);
                buffer.setString(x + 8, y, selected.status, VALUE_STYLE);
                y++;
            }
            y++;
            if (!selected.methods.isEmpty()) {
                buffer.setString(x, y, "Methods:", LABEL_STYLE);
                y++;
                for (String method : selected.methods) {
                    if (y >= detailArea.y() + detailArea.height())
                        break;
                    buffer.setString(x + 1, y, "- " + truncate(method, w - 3), VALUE_STYLE);
                    y++;
                }
            }
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                listView.moveDown();
                ctx.requestRedraw();
                return true;
            case 'k':
            case KeyCode.UP:
                listView.moveUp();
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_DOWN:
                listView.pageDown();
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_UP:
                listView.pageUp();
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

    private static class GrpcService {
        final String name;
        final String status;
        final List<String> methods;

        GrpcService(String name, String status, List<String> methods) {
            this.name = name;
            this.status = status;
            this.methods = methods;
        }
    }
}
