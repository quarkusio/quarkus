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
import tools.jackson.databind.node.ObjectNode;

public class SchedulerShellPage extends BaseExtensionPage {

    private static final Style SELECTED = Style.EMPTY.onCyan().black().bold();
    private static final Style RUNNING_STYLE = Style.EMPTY.green();
    private static final Style PAUSED_STYLE = Style.EMPTY.yellow();

    private List<ScheduleEntry> schedules = List.of();
    private int selectedIndex = 0;
    private int scrollOffset = 0;

    public SchedulerShellPage() {
        super("quarkus-scheduler", "Scheduler");
    }

    @Override
    public void loadData() {
        JsonNode result = rpcCall("getData");
        List<ScheduleEntry> entries = new ArrayList<>();
        JsonNode arr = result.isArray() ? result : result.path("schedulerData");
        if (!arr.isArray()) {
            arr = result.path("_array");
        }
        if (arr.isArray()) {
            for (JsonNode item : arr) {
                entries.add(new ScheduleEntry(
                        item.path("identity").asText(item.path("triggerDescription").asText("")),
                        item.path("cron").asText(item.path("every").asText("")),
                        item.path("methodDescription").asText(""),
                        item.path("running").asBoolean(!item.path("paused").asBoolean(false))));
            }
        }
        schedules = entries;
    }

    @Override
    public void renderPanel(Rect area, Buffer buffer) {
        if (schedules.isEmpty()) {
            buffer.setString(area.x() + 1, area.y(), "No scheduled jobs found", DIM_STYLE);
            return;
        }

        int visibleRows = area.height();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRows) {
            scrollOffset = selectedIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows && (scrollOffset + i) < schedules.size(); i++) {
            int idx = scrollOffset + i;
            ScheduleEntry entry = schedules.get(idx);
            boolean selected = idx == selectedIndex;

            if (selected) {
                buffer.fill(new Rect(area.x(), area.y() + i, area.width(), 1), new Cell(" ", SELECTED));
            }

            int x = area.x() + 1;
            String status = entry.running ? "* " : "| ";
            buffer.setString(x, area.y() + i, status, selected ? SELECTED : (entry.running ? RUNNING_STYLE : PAUSED_STYLE));
            x += 2;

            buffer.setString(x, area.y() + i, truncate(entry.identity, 30), selected ? SELECTED : VALUE_STYLE);
            x += 31;

            buffer.setString(x, area.y() + i, truncate(entry.schedule, 20), selected ? SELECTED : DIM_STYLE);
            x += 21;

            if (!entry.description.isEmpty()) {
                buffer.setString(x, area.y() + i, truncate(entry.description, area.width() - x + area.x()),
                        selected ? SELECTED : DIM_STYLE);
            }
        }
    }

    @Override
    public boolean handlePanelKey(int[] keys) {
        int key = KeyCode.parse(keys);
        switch (key) {
            case 'j':
            case KeyCode.DOWN:
                if (selectedIndex < schedules.size() - 1) {
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
            case KeyCode.ENTER:
                togglePause();
                return true;
            case 't':
            case 'T':
                triggerJob();
                return true;
            default:
                return false;
        }
    }

    private void togglePause() {
        if (schedules.isEmpty())
            return;
        ScheduleEntry entry = schedules.get(selectedIndex);
        try {
            ObjectNode params = createParams();
            params.put("identity", entry.identity);
            if (entry.running) {
                rpcCall("pauseJob", params);
            } else {
                rpcCall("resumeJob", params);
            }
            loadDataAsync();
        } catch (Exception e) {
            errorMessage = e.getMessage();
            ctx.requestRedraw();
        }
    }

    private void triggerJob() {
        if (schedules.isEmpty())
            return;
        ScheduleEntry entry = schedules.get(selectedIndex);
        try {
            ObjectNode params = createParams();
            params.put("identity", entry.identity);
            rpcCall("executeJob", params);
            loadDataAsync();
        } catch (Exception e) {
            errorMessage = e.getMessage();
            ctx.requestRedraw();
        }
    }

    @Override
    protected String getFooterText() {
        return "[j/k] Navigate  [Enter] Pause/Resume  [T] Trigger  " + schedules.size() + " jobs";
    }

    private record ScheduleEntry(String identity, String schedule, String description, boolean running) {
    }
}
