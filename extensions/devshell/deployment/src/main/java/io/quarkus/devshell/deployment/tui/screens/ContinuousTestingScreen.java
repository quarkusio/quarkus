package io.quarkus.devshell.deployment.tui.screens;

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

public class ContinuousTestingScreen implements Screen {

    private static final Style GREEN = Style.EMPTY.green();
    private static final Style RED = Style.EMPTY.red();
    private static final Style YELLOW = Style.EMPTY.yellow();
    private static final Style DIM = Style.EMPTY.gray();
    private static final Style NORMAL = Style.EMPTY.white();
    private static final String NAMESPACE = "devui-continuous-testing";

    private AppContext ctx;
    private boolean running;
    private boolean inProgress;
    private long passed;
    private long failed;
    private long skipped;
    private String statusMessage = "Loading...";

    @Override
    public String getTitle() {
        return "Continuous Testing";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        loadState();
    }

    private void loadState() {
        try {
            JsonNode result = ctx.getJsonRpcClient().call(NAMESPACE, "currentState");
            parseState(result);
        } catch (Exception e) {
            statusMessage = "Continuous testing not available";
        }
    }

    private void parseState(JsonNode state) {
        JsonNode config = state.path("config");
        running = config.path("enabled").asBoolean(false);
        inProgress = state.path("inProgress").asBoolean(false);

        JsonNode result = state.path("result");
        JsonNode counts = result.path("counts");
        passed = counts.path("passed").asLong(0);
        failed = counts.path("failed").asLong(0);
        skipped = counts.path("skipped").asLong(0);

        statusMessage = running ? "Running" : "Stopped";
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        var areas = Layout.vertical()
                .constraints(Constraint.length(3), Constraint.fill(), Constraint.length(1))
                .split(area);

        renderStatus(areas.get(0), buffer);
        renderResults(areas.get(1), buffer);
        renderFooter(areas.get(2), buffer);
    }

    private void renderStatus(Rect area, Buffer buffer) {
        Style statusColor = running ? GREEN : RED;
        String statusIcon = running ? "*" : "o";

        buffer.setLine(area.x() + 1, area.y(), Line.from(
                Span.styled(statusIcon + " ", statusColor),
                Span.styled(statusMessage, statusColor.bold())));

        if (inProgress) {
            buffer.setString(area.x() + 1, area.y() + 1, ">> Testing in progress...", YELLOW);
        }

        buffer.setLine(area.x() + 1, area.y() + 2, Line.from(
                Span.styled("+ ", GREEN), Span.styled(passed + " passed  ", NORMAL),
                Span.styled("x ", RED), Span.styled(failed + " failed  ", NORMAL),
                Span.styled("- ", YELLOW), Span.styled(skipped + " skipped", NORMAL)));
    }

    private void renderResults(Rect area, Buffer buffer) {
        if (!running) {
            buffer.setString(area.x() + 2, area.y() + 1, "Press [S] to start continuous testing", DIM);
            return;
        }

        long total = passed + failed + skipped;
        if (total == 0) {
            buffer.setString(area.x() + 2, area.y() + 1, "No test results yet", DIM);
            return;
        }

        int barWidth = Math.max(1, area.width() - 4);
        int passedWidth = total > 0 ? (int) (passed * barWidth / total) : 0;
        int failedWidth = total > 0 ? (int) (failed * barWidth / total) : 0;
        int skippedWidth = barWidth - passedWidth - failedWidth;

        int x = area.x() + 2;
        int y = area.y() + 1;
        for (int i = 0; i < passedWidth; i++) {
            buffer.set(x + i, y, new Cell("#", GREEN));
        }
        for (int i = 0; i < failedWidth; i++) {
            buffer.set(x + passedWidth + i, y, new Cell("#", RED));
        }
        for (int i = 0; i < skippedWidth; i++) {
            buffer.set(x + passedWidth + failedWidth + i, y, new Cell(".", YELLOW));
        }
    }

    private void renderFooter(Rect area, Buffer buffer) {
        if (running) {
            buffer.setLine(area.x(), area.y(), Line.from(
                    Span.styled(" S", Style.EMPTY.cyan()), Span.styled(" Stop  ", DIM),
                    Span.styled("A", Style.EMPTY.cyan()), Span.styled(" Run All  ", DIM),
                    Span.styled("F", Style.EMPTY.cyan()), Span.styled(" Run Failed  ", DIM),
                    Span.styled("R", Style.EMPTY.cyan()), Span.styled(" Refresh  ", DIM),
                    Span.styled("ESC", Style.EMPTY.cyan()), Span.styled(" Back", DIM)));
        } else {
            buffer.setLine(area.x(), area.y(), Line.from(
                    Span.styled(" S", Style.EMPTY.cyan()), Span.styled(" Start  ", DIM),
                    Span.styled("R", Style.EMPTY.cyan()), Span.styled(" Refresh  ", DIM),
                    Span.styled("ESC", Style.EMPTY.cyan()), Span.styled(" Back", DIM)));
        }
    }

    @Override
    public boolean handleKey(int[] keys) {
        if (keys.length != 1)
            return false;
        switch (keys[0]) {
            case 's':
            case 'S':
                invokeAction(running ? "stop" : "start");
                return true;
            case 'a':
            case 'A':
                if (running)
                    invokeAction("runAll");
                return true;
            case 'f':
            case 'F':
                if (running)
                    invokeAction("runFailed");
                return true;
            case 'r':
            case 'R':
                loadState();
                ctx.requestRedraw();
                return true;
        }
        return false;
    }

    private void invokeAction(String action) {
        try {
            ctx.getJsonRpcClient().call(NAMESPACE, action);
        } catch (Exception e) {
            statusMessage = "Action failed: " + e.getMessage();
        }
        loadState();
        ctx.requestRedraw();
    }
}
