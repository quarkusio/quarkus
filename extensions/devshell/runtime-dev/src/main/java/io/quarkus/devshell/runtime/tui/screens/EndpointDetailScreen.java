package io.quarkus.devshell.runtime.tui.screens;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.AppContext;
import io.quarkus.devshell.runtime.tui.BufferHelper;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.Screen;

/**
 * Screen for viewing details of a single endpoint.
 */
public class EndpointDetailScreen implements Screen {

    private AppContext ctx;
    private final EndpointsScreen.EndpointItem endpoint;

    public EndpointDetailScreen(EndpointsScreen.EndpointItem endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String getTitle() {
        return "Endpoint: " + endpoint.uri;
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        // Nothing to initialize
    }

    @Override
    public void onLeave() {
        // Nothing to clean up
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        int width = this.ctx.getWidth();
        int height = this.ctx.getHeight();

        // Header
        renderHeader(buffer, width);

        Style labelStyle = Style.create().cyan().bold();
        Style valueStyle = Style.EMPTY;
        int row = 3;

        // URI
        buffer.setString(2, row, "Path:", labelStyle);
        buffer.setString(14, row, endpoint.uri, valueStyle);
        row += 2;

        // Parse and display description components
        String description = endpoint.description;
        if (description != null && !description.isEmpty()) {
            // HTTP Method
            String httpMethod = extractHttpMethod(description);
            if (!httpMethod.isEmpty()) {
                buffer.setString(2, row, "Method:", labelStyle);
                buffer.setString(14, row, formatHttpMethod(httpMethod), valueStyle);
                row += 2;
            }

            // Consumes
            String consumes = extractPart(description, "consumes:");
            if (!consumes.isEmpty()) {
                buffer.setString(2, row, "Consumes:", labelStyle);
                buffer.setString(14, row, consumes, valueStyle);
                row += 2;
            }

            // Produces
            String produces = extractPart(description, "produces:");
            if (!produces.isEmpty()) {
                buffer.setString(2, row, "Produces:", labelStyle);
                buffer.setString(14, row, produces, valueStyle);
                row += 2;
            }

            // Java Method
            String javaMethod = extractPart(description, "java:");
            if (!javaMethod.isEmpty()) {
                buffer.setString(2, row, "Java:", labelStyle);
                buffer.setString(14, row, javaMethod, valueStyle);
                row += 2;
            }

            // If no parts extracted, show raw description
            if (httpMethod.isEmpty() && consumes.isEmpty() && produces.isEmpty() && javaMethod.isEmpty()) {
                buffer.setString(2, row, "Description:", labelStyle);
                buffer.setString(14, row, description, valueStyle);
                row += 2;
            }
        }

        // Footer
        buffer.setString(1, height - 2, "[Esc] Back", Style.create().gray());
    }

    private void renderHeader(Buffer buffer, int width) {
        BufferHelper.writeHeader(buffer, " Endpoint Detail ", width);
    }

    private String extractHttpMethod(String description) {
        if (description == null || description.isEmpty()) {
            return "";
        }
        int spaceIdx = description.indexOf(' ');
        int parenIdx = description.indexOf('(');
        if (parenIdx > 0 && (spaceIdx < 0 || parenIdx < spaceIdx)) {
            return description.substring(0, parenIdx).trim();
        }
        if (spaceIdx > 0) {
            return description.substring(0, spaceIdx);
        }
        // Check if it's just an HTTP method
        String upper = description.toUpperCase();
        if (upper.equals("GET") || upper.equals("POST") || upper.equals("PUT") ||
                upper.equals("DELETE") || upper.equals("PATCH") || upper.equals("HEAD") ||
                upper.equals("OPTIONS") || upper.equals("SERVLET")) {
            return description;
        }
        return "";
    }

    private String extractPart(String description, String key) {
        int startIdx = description.indexOf("(" + key);
        if (startIdx < 0) {
            return "";
        }
        int valueStart = startIdx + key.length() + 2; // +2 for '(' and ':'
        int endIdx = description.indexOf(')', valueStart);
        if (endIdx > valueStart) {
            return description.substring(valueStart, endIdx).trim();
        }
        return "";
    }

    private String formatHttpMethod(String method) {
        return method.toUpperCase();
    }

    @Override
    public boolean handleKey(int key) {
        switch (key) {
            case KeyCode.ESCAPE:
                this.ctx.goBack();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onResize(int width, int height) {
        // Nothing to resize
    }
}
