package io.quarkus.devshell.deployment.tui.screens;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.Screen;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public class ReadmeScreen implements Screen {

    private static final Style HEADING1 = Style.EMPTY.cyan().bold();
    private static final Style HEADING2 = Style.EMPTY.cyan();
    private static final Style HEADING3 = Style.EMPTY.white().bold();
    private static final Style NORMAL = Style.EMPTY.white();
    private static final Style DIM = Style.EMPTY.gray();
    private static final Style BLOCKQUOTE = Style.EMPTY.gray();
    private static final Style RULE = Style.EMPTY.gray();
    private static final Style YELLOW = Style.EMPTY.yellow();

    private AppContext ctx;
    private volatile boolean loading = true;
    private volatile String errorMessage;
    private List<String> lines = List.of();
    private int scrollOffset = 0;

    @Override
    public String getTitle() {
        return "README";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        loadDataAsync();
    }

    private void loadDataAsync() {
        loading = true;
        errorMessage = null;
        ctx.requestRedraw();

        CompletableFuture.runAsync(() -> {
            try {
                JsonNode result = ctx.getJsonRpcClient().call("devui-workspace", "getWorkspaceItems");
                JsonNode arr = result.isArray() ? result : result.path("items");

                String readmePath = null;
                if (arr.isArray()) {
                    for (JsonNode item : arr) {
                        String name = item.path("name").asText(item.path("fileName").asText(""));
                        if ("README.md".equalsIgnoreCase(name)) {
                            readmePath = item.path("path").asText(item.path("filePath").asText(""));
                            break;
                        }
                    }
                }

                if (readmePath == null || readmePath.isEmpty()) {
                    errorMessage = "No README.md found in workspace";
                    loading = false;
                    ctx.requestRedraw();
                    return;
                }

                ObjectMapper mapper = new ObjectMapper();
                ObjectNode params = mapper.createObjectNode();
                params.put("path", readmePath);
                JsonNode content = ctx.getJsonRpcClient().call("devui-workspace", "getWorkspaceItemContent", params);

                boolean isBinary = content.path("isBinary").asBoolean(false);
                if (isBinary) {
                    errorMessage = "README.md appears to be a binary file";
                    loading = false;
                    ctx.requestRedraw();
                    return;
                }

                String text = content.path("content").asText("");
                if (text.isEmpty()) {
                    errorMessage = "README.md is empty";
                } else {
                    lines = List.of(text.split("\n", -1));
                }
                loading = false;
            } catch (Exception e) {
                errorMessage = e.getMessage();
                loading = false;
            }
            ctx.requestRedraw();
        });
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (loading) {
            buffer.setString(area.x() + 2, area.y() + 1, "Loading README.md...", YELLOW);
            return;
        }
        if (errorMessage != null) {
            buffer.setString(area.x() + 2, area.y() + 1, errorMessage, Style.EMPTY.red());
            return;
        }

        var areas = Layout.vertical()
                .constraints(Constraint.fill(), Constraint.length(1))
                .split(area);

        renderContent(areas.get(0), buffer);
        renderFooter(areas.get(1), buffer);
    }

    private void renderContent(Rect area, Buffer buffer) {
        int visibleRows = area.height();
        int width = area.width() - 2;

        for (int i = 0; i < visibleRows && (scrollOffset + i) < lines.size(); i++) {
            String line = lines.get(scrollOffset + i);
            int y = area.y() + i;
            int x = area.x() + 1;

            renderMarkdownLine(buffer, x, y, line, width);
        }

        if (lines.size() > visibleRows) {
            int pct = (int) ((scrollOffset + visibleRows) * 100.0 / lines.size());
            buffer.setString(area.x() + area.width() - 5, area.y() + area.height() - 1,
                    Math.min(pct, 100) + "%", DIM);
        }
    }

    private void renderMarkdownLine(Buffer buffer, int x, int y, String line, int width) {
        if (line.startsWith("### ")) {
            buffer.setString(x, y, truncate(line.substring(4), width), HEADING3);
        } else if (line.startsWith("## ")) {
            buffer.setString(x, y, truncate(line.substring(3), width), HEADING2);
        } else if (line.startsWith("# ")) {
            buffer.setString(x, y, truncate(line.substring(2), width), HEADING1);
        } else if (line.startsWith("- ") || line.startsWith("* ")) {
            buffer.setString(x, y, "• " + truncate(line.substring(2), width - 2), NORMAL);
        } else if (line.startsWith("> ")) {
            buffer.setString(x, y, "│ " + truncate(line.substring(2), width - 2), BLOCKQUOTE);
        } else if (line.matches("^[-*_]{3,}$")) {
            String rule = "─".repeat(Math.max(1, Math.min(width, 40)));
            buffer.setString(x, y, rule, RULE);
        } else {
            String cleaned = line.replace("`", "");
            buffer.setString(x, y, truncate(cleaned, width), NORMAL);
        }
    }

    private void renderFooter(Rect area, Buffer buffer) {
        buffer.setLine(area.x(), area.y(), Line.from(
                Span.styled(" j/k", Style.EMPTY.cyan()), Span.styled(" Scroll  ", DIM),
                Span.styled("PgUp/PgDn", Style.EMPTY.cyan()), Span.styled(" Page  ", DIM),
                Span.styled("Home/End", Style.EMPTY.cyan()), Span.styled(" Jump  ", DIM),
                Span.styled("R", Style.EMPTY.cyan()), Span.styled(" Refresh  ", DIM),
                Span.styled("ESC", Style.EMPTY.cyan()), Span.styled(" Back", DIM)));
    }

    @Override
    public boolean handleKey(int[] keys) {
        if (loading) {
            return false;
        }

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
                scrollOffset = Math.min(scrollOffset + 20, Math.max(0, lines.size() - 1));
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_UP:
                scrollOffset = Math.max(scrollOffset - 20, 0);
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
            case 'r':
            case 'R':
                loadDataAsync();
                return true;
        }

        return false;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || maxLen <= 0)
            return "";
        if (s.length() <= maxLen)
            return s;
        return s.substring(0, maxLen - 1) + "~";
    }
}
