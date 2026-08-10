package io.quarkus.devshell.deployment.tui.screens;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import io.quarkus.devshell.deployment.tui.AppContext;
import io.quarkus.devshell.deployment.tui.Screen;

public class EndpointDetailScreen implements Screen {

    private static final Style LABEL = Style.EMPTY.cyan();
    private static final Style VALUE = Style.EMPTY.white();
    private static final Style DIM = Style.EMPTY.gray();

    private final EndpointsScreen.EndpointItem item;

    private String httpMethod = "";
    private String consumes = "";
    private String produces = "";
    private String javaMethod = "";

    public EndpointDetailScreen(EndpointsScreen.EndpointItem item) {
        this.item = item;
        parseDescription(item.description());
    }

    private void parseDescription(String description) {
        if (description == null || description.isEmpty()) {
            httpMethod = "GET";
            return;
        }

        // Format: "GET (consumes:application/json) (produces:text/html) (java:com.example.Resource#method)"
        int firstSpace = description.indexOf(' ');
        int firstParen = description.indexOf('(');
        int end = firstSpace > 0 ? firstSpace : (firstParen > 0 ? firstParen : description.length());
        String method = description.substring(0, Math.min(end, 10)).toUpperCase();
        if (method.matches("GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS")) {
            httpMethod = method;
        } else {
            httpMethod = "GET";
        }

        consumes = extractField(description, "consumes:");
        produces = extractField(description, "produces:");
        javaMethod = extractField(description, "java:");
    }

    private static String extractField(String description, String prefix) {
        int start = description.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        start += prefix.length();
        int end = description.indexOf(')', start);
        if (end < 0) {
            end = description.length();
        }
        return description.substring(start, end).trim();
    }

    @Override
    public String getTitle() {
        return "Endpoint: " + item.uri();
    }

    @Override
    public void onEnter(AppContext ctx) {
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        var areas = Layout.vertical()
                .constraints(Constraint.fill(), Constraint.length(1))
                .split(area);

        renderDetails(areas.get(0), buffer);
        renderFooter(areas.get(1), buffer);
    }

    private void renderDetails(Rect area, Buffer buffer) {
        int x = area.x() + 2;
        int y = area.y() + 1;

        buffer.setString(x, y, "Path:", LABEL);
        buffer.setString(x + 14, y, item.uri(), VALUE);
        y += 2;

        buffer.setString(x, y, "Method:", LABEL);
        buffer.setString(x + 14, y, httpMethod, methodStyle(httpMethod));
        y += 2;

        if (!consumes.isEmpty()) {
            buffer.setString(x, y, "Consumes:", LABEL);
            buffer.setString(x + 14, y, consumes, VALUE);
            y += 2;
        }

        if (!produces.isEmpty()) {
            buffer.setString(x, y, "Produces:", LABEL);
            buffer.setString(x + 14, y, produces, VALUE);
            y += 2;
        }

        if (!javaMethod.isEmpty()) {
            buffer.setString(x, y, "Java Method:", LABEL);
            buffer.setString(x + 14, y, javaMethod, VALUE);
        }
    }

    private void renderFooter(Rect area, Buffer buffer) {
        buffer.setLine(area.x(), area.y(), Line.from(
                Span.styled(" ESC", Style.EMPTY.cyan()), Span.styled(" Back", DIM)));
    }

    private static Style methodStyle(String method) {
        return switch (method) {
            case "GET" -> Style.EMPTY.green().bold();
            case "POST" -> Style.EMPTY.yellow().bold();
            case "PUT" -> Style.EMPTY.blue().bold();
            case "DELETE" -> Style.EMPTY.red().bold();
            default -> Style.EMPTY.magenta().bold();
        };
    }

    @Override
    public boolean handleKey(int[] keys) {
        return false;
    }
}
