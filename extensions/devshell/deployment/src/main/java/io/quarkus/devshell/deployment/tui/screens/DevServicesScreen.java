package io.quarkus.devshell.deployment.tui.screens;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.paragraph.Paragraph;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.Screen;
import tools.jackson.databind.JsonNode;

public class DevServicesScreen implements Screen {

    private static final Style GREEN = Style.EMPTY.green();
    private static final Style YELLOW = Style.EMPTY.yellow();
    private static final Style DIM = Style.EMPTY.gray();
    private static final Style NORMAL = Style.EMPTY.white();
    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style LABEL = Style.EMPTY.cyan();

    private AppContext ctx;
    private List<DevService> services = List.of();
    private int selectedIndex = 0;

    @Override
    public String getTitle() {
        return "Dev Services";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        loadData();
    }

    private void loadData() {
        try {
            JsonNode result = ctx.getJsonRpcClient().call("devui-dev-services", "getDevServices");
            services = parseServices(result);
        } catch (Exception e) {
            services = List.of();
        }
    }

    private List<DevService> parseServices(JsonNode result) {
        List<DevService> list = new ArrayList<>();
        JsonNode arr = result.isArray() ? result : null;
        if (arr == null) {
            return list;
        }

        for (JsonNode ds : arr) {
            list.add(new DevService(
                    ds.path("name").asText(""),
                    ds.path("description").asText(""),
                    ds.path("containerInfo"),
                    ds.path("configs")));
        }
        return list;
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (services.isEmpty()) {
            Paragraph p = Paragraph.builder()
                    .text(Text.from(Line.from(Span.styled(" No dev services running", DIM))))
                    .build();
            p.render(area, buffer);
            return;
        }

        var areas = Layout.horizontal()
                .constraints(Constraint.percentage(35), Constraint.percentage(65))
                .split(area);

        renderServiceList(areas.get(0), buffer);
        renderServiceDetail(areas.get(1), buffer);
    }

    private void renderServiceList(Rect area, Buffer buffer) {
        Block listBlock = Block.builder()
                .title(" Services (" + services.size() + ") ")
                .borders(Borders.ALL)
                .build();
        Rect inner = listBlock.inner(area);
        listBlock.render(area, buffer);

        for (int i = 0; i < services.size() && i < inner.height(); i++) {
            DevService svc = services.get(i);
            boolean selected = i == selectedIndex;
            boolean hasContainer = svc.containerInfo != null && !svc.containerInfo.isMissingNode();

            if (selected) {
                buffer.fill(new Rect(inner.x(), inner.y() + i, inner.width(), 1), new Cell(" ", SELECTED));
            }

            Style iconStyle = selected ? SELECTED : (hasContainer ? GREEN : YELLOW);
            String icon = hasContainer ? "# " : "o ";
            buffer.setString(inner.x(), inner.y() + i, icon, iconStyle);
            buffer.setString(inner.x() + 2, inner.y() + i,
                    truncate(svc.name, inner.width() - 2), selected ? SELECTED : NORMAL);
        }
    }

    private void renderServiceDetail(Rect area, Buffer buffer) {
        if (selectedIndex >= services.size())
            return;
        DevService svc = services.get(selectedIndex);

        Block detailBlock = Block.builder()
                .title(" " + svc.name + " ")
                .borders(Borders.ALL)
                .build();
        Rect inner = detailBlock.inner(area);
        detailBlock.render(area, buffer);

        int y = inner.y();

        if (!svc.description.isEmpty()) {
            buffer.setString(inner.x(), y++, svc.description, NORMAL);
            y++;
        }

        if (svc.containerInfo != null && !svc.containerInfo.isMissingNode()) {
            buffer.setString(inner.x(), y++, "Container", LABEL.bold());
            renderField(buffer, inner.x(), y++, "Image", svc.containerInfo.path("imageName").asText(""), inner.width());
            renderField(buffer, inner.x(), y++, "ID",
                    truncate(svc.containerInfo.path("id").asText(""), 12), inner.width());
            renderField(buffer, inner.x(), y++, "Status", svc.containerInfo.path("status").asText(""), inner.width());

            JsonNode ports = svc.containerInfo.path("exposedPorts");
            if (ports.isArray() && ports.size() > 0) {
                StringBuilder portStr = new StringBuilder();
                for (int i = 0; i < ports.size(); i++) {
                    JsonNode p = ports.get(i);
                    if (i > 0)
                        portStr.append(", ");
                    portStr.append(p.path("publicPort").asInt(0))
                            .append("->").append(p.path("privatePort").asInt(0))
                            .append("/").append(p.path("type").asText("tcp"));
                }
                renderField(buffer, inner.x(), y++, "Ports", portStr.toString(), inner.width());
            }
            y++;
        }

        if (svc.configs != null && !svc.configs.isMissingNode() && svc.configs.isObject()) {
            buffer.setString(inner.x(), y++, "Configuration", LABEL.bold());
            Iterator<String> fields = svc.configs.propertyNames().iterator();
            while (fields.hasNext() && y < inner.y() + inner.height()) {
                String key = fields.next();
                String value = svc.configs.path(key).asText("");
                buffer.setString(inner.x(), y, truncate(key, inner.width() / 2), LABEL);
                buffer.setString(inner.x() + inner.width() / 2, y, truncate(value, inner.width() / 2), DIM);
                y++;
            }
        }
    }

    private void renderField(Buffer buffer, int x, int y, String label, String value, int maxWidth) {
        buffer.setString(x, y, "  " + label + ": ", LABEL);
        buffer.setString(x + label.length() + 4, y, truncate(value, maxWidth - label.length() - 4), NORMAL);
    }

    @Override
    public boolean handleKey(int[] keys) {
        if (keys.length == 1) {
            switch (keys[0]) {
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
        if (selectedIndex < services.size() - 1) {
            selectedIndex++;
            ctx.requestRedraw();
            return true;
        }
        return false;
    }

    private static String truncate(String s, int maxLen) {
        if (maxLen <= 0)
            return "";
        if (s.length() <= maxLen)
            return s;
        return s.substring(0, maxLen - 1) + "~";
    }

    private record DevService(String name, String description, JsonNode containerInfo, JsonNode configs) {
    }
}
