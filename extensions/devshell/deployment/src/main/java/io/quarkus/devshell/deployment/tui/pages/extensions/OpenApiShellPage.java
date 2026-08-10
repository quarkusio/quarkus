package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;
import tools.jackson.databind.JsonNode;

public class OpenApiShellPage extends BaseExtensionPage {

    private final List<String> lines = new ArrayList<>();
    private int scrollOffset = 0;

    public OpenApiShellPage() {
        super("quarkus-smallrye-openapi", "OpenAPI");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getOpenAPISchema");
        lines.clear();

        String text = result.asText("");
        if (text.isEmpty() && result.isObject()) {
            text = result.toPrettyString();
        }
        for (String line : text.split("\n", -1)) {
            lines.add(line);
        }
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (lines.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No OpenAPI document available", DIM_STYLE);
            return;
        }

        int x = area.x() + 1;
        int w = area.width() - 2;
        int visibleRows = area.height();
        int lineNumWidth = String.valueOf(lines.size()).length() + 1;

        for (int i = 0; i < visibleRows; i++) {
            int lineIdx = scrollOffset + i;
            if (lineIdx >= lines.size())
                break;

            String lineNum = String.format("%" + lineNumWidth + "d ", lineIdx + 1);
            buffer.setString(x, area.y() + i, lineNum, DIM_STYLE);
            buffer.setString(x + lineNum.length(), area.y() + i,
                    truncate(lines.get(lineIdx), w - lineNum.length()), VALUE_STYLE);
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                if (scrollOffset < lines.size() - 1) {
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
            case KeyCode.PAGE_DOWN:
                scrollOffset = Math.min(lines.size() - 1, scrollOffset + 20);
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_UP:
                scrollOffset = Math.max(0, scrollOffset - 20);
                ctx.requestRedraw();
                return true;
            case KeyCode.HOME:
                scrollOffset = 0;
                ctx.requestRedraw();
                return true;
            case KeyCode.END:
                scrollOffset = Math.max(0, lines.size() - 1);
                ctx.requestRedraw();
                return true;
            default:
                return false;
        }
    }

    @Override
    protected String getFooterText() {
        return "[j/k] Scroll  [PgUp/PgDn] Page";
    }
}
