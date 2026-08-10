package io.quarkus.devshell.deployment.tui.pages.extensions;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import io.quarkus.devshell.deployment.tui.KeyCode;
import io.quarkus.devshell.deployment.tui.pages.BaseExtensionPage;

public class MicrometerShellPage extends BaseExtensionPage {

    private final List<String> lines = new ArrayList<>();
    private int scrollOffset = 0;

    public MicrometerShellPage() {
        super("quarkus-micrometer", "Micrometer Metrics");
    }

    @Override
    public void loadData() {
        lines.clear();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/q/metrics"))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            for (String line : response.body().split("\n", -1)) {
                lines.add(line);
            }
        } catch (Exception e) {
            lines.add("Failed to fetch metrics: " + e.getMessage());
        }
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (lines.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No metrics available", DIM_STYLE);
            return;
        }

        int x = area.x() + 1;
        int w = area.width() - 2;
        int visibleRows = area.height();

        for (int i = 0; i < visibleRows; i++) {
            int lineIdx = scrollOffset + i;
            if (lineIdx >= lines.size())
                break;
            buffer.setString(x, area.y() + i, truncate(lines.get(lineIdx), w), VALUE_STYLE);
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
                scrollOffset = Math.min(scrollOffset + 20, Math.max(0, lines.size() - 1));
                ctx.requestRedraw();
                return true;
            case KeyCode.PAGE_UP:
                scrollOffset = Math.max(scrollOffset - 20, 0);
                ctx.requestRedraw();
                return true;
            default:
                return false;
        }
    }

    @Override
    protected String getFooterText() {
        return "[j/k] Scroll";
    }
}
