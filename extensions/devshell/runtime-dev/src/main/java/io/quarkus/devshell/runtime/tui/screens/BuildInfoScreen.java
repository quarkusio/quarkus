package io.quarkus.devshell.runtime.tui.screens;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.AppContext;
import io.quarkus.devshell.runtime.tui.BufferHelper;
import io.quarkus.devshell.runtime.tui.KeyCode;
import io.quarkus.devshell.runtime.tui.Screen;
import io.quarkus.devshell.runtime.tui.widgets.TableView;
import io.quarkus.devui.runtime.build.BuildMetricsJsonRPCService;
import io.vertx.core.json.JsonObject;

/**
 * Screen for viewing build metrics and information.
 */
public class BuildInfoScreen implements Screen {

    private AppContext ctx;

    private enum Tab {
        BUILD_STEPS("Build Steps"),
        BUILD_ITEMS("Build Items");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private Tab currentTab = Tab.BUILD_STEPS;
    private boolean loading = true;
    private String error = null;

    // Build metrics data
    private boolean metricsEnabled = false;
    private int numberOfThreads = 0;
    private long duration = 0;
    private final List<BuildStep> buildSteps = new ArrayList<>();
    private final List<BuildItem> buildItems = new ArrayList<>();

    // Tables
    private final TableView<BuildStep> stepsTable;
    private final TableView<BuildItem> itemsTable;

    @Inject
    BuildMetricsJsonRPCService service;

    public BuildInfoScreen() {
        this.stepsTable = new TableView<BuildStep>()
                .addColumn("Build Step", this::formatStepName, 50)
                .addColumn("Duration", s -> formatDuration(s.duration), 12)
                .addColumn("Thread", s -> s.thread, 10);

        this.itemsTable = new TableView<BuildItem>()
                .addColumn("Build Item", this::formatItemName, 60)
                .addColumn("Count", i -> String.valueOf(i.count), 10);
    }

    @Override
    public String getTitle() {
        return "Build Metrics";
    }

    @Override
    public void onEnter(AppContext ctx) {
        this.ctx = ctx;
        loadBuildMetrics();
    }

    @Override
    public void onLeave() {
        // Nothing to clean up
    }

    private void loadBuildMetrics() {
        loading = true;
        error = null;
        this.ctx.requestRedraw();

        try {
            // Load build metrics
            loadMetricsFromService(service);

            // Load build items
            loadItemsFromService(service);

            loading = false;
            this.ctx.requestRedraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load build metrics: " + ex.getMessage();
            this.ctx.requestRedraw();
        }
    }

    private void loadMetricsFromService(BuildMetricsJsonRPCService svc) {
        buildSteps.clear();

        try {
            // BuildMetrics has public fields: enabled, numberOfThreads, duration, records (JsonArray)
            var metrics = svc.getBuildMetrics();
            if (metrics == null) {
                return;
            }

            metricsEnabled = metrics.enabled;
            if (!metricsEnabled) {
                return;
            }

            numberOfThreads = metrics.numberOfThreads;
            duration = metrics.duration != null ? metrics.duration : 0L;

            if (metrics.records != null) {
                for (int i = 0; i < metrics.records.size(); i++) {
                    JsonObject record = metrics.records.getJsonObject(i);
                    BuildStep step = new BuildStep();
                    step.stepId = record.getString("stepId");
                    step.duration = toLong(record.getValue("duration"), 0L);
                    step.thread = record.getString("thread");
                    step.started = toLong(record.getValue("started"), 0L);

                    if (step.stepId != null && !step.stepId.isEmpty()) {
                        buildSteps.add(step);
                    }
                }
            }

            // Sort by duration descending
            buildSteps.sort((a, b) -> Long.compare(b.duration, a.duration));
            stepsTable.setItems(buildSteps);

        } catch (Exception e) {
            error = "Failed to parse build metrics: " + e.getMessage();
        }
    }

    private void loadItemsFromService(BuildMetricsJsonRPCService svc) {
        buildItems.clear();

        try {
            // BuildItems has public fields: enabled, itemsCount, items (JsonArray)
            var items = svc.getBuildItems();
            if (items == null || !items.enabled) {
                return;
            }

            if (items.items != null) {
                for (int i = 0; i < items.items.size(); i++) {
                    JsonObject itemObj = items.items.getJsonObject(i);
                    BuildItem bi = new BuildItem();
                    bi.className = itemObj.getString("class");
                    bi.count = (int) toLong(itemObj.getValue("count"), 0L);

                    if (bi.className != null && !bi.className.isEmpty()) {
                        buildItems.add(bi);
                    }
                }
            }

            // Sort by count descending
            buildItems.sort((a, b) -> Integer.compare(b.count, a.count));
            itemsTable.setItems(buildItems);

        } catch (Exception e) {
            // Build items parsing failed, but we can still show build steps
        }
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        int width = this.ctx.getWidth();
        int height = this.ctx.getHeight();

        // Header
        renderHeader(buffer, width);

        if (loading) {
            buffer.setString(width / 2 - 5, height / 2, "Loading...", Style.create().yellow());
            return;
        }

        if (error != null) {
            buffer.setString(1, 4, error, Style.create().red());
            renderFooter(buffer, width, height);
            return;
        }

        if (!metricsEnabled) {
            buffer.setString(1, 4, "Build metrics are not enabled.", Style.create().gray());
            buffer.setString(1, 6, "Add this to your application.properties to enable:", Style.create().gray());
            buffer.setString(1, 7, "quarkus.debug.generated-classes-dir=true", Style.create().green());
            renderFooter(buffer, width, height);
            return;
        }

        // Summary bar
        renderSummary(buffer, width);

        // Tabs
        renderTabs(buffer, width);

        // Content
        int contentStartRow = 6;
        int contentHeight = height - 8;

        TableView<?> currentTable = currentTab == Tab.BUILD_STEPS ? stepsTable : itemsTable;
        currentTable.setVisibleRows(contentHeight);
        currentTable.setWidth(width - 4);
        currentTable.render(buffer, contentStartRow, 2);

        // Footer
        renderFooter(buffer, width, height);
    }

    private void renderHeader(Buffer buffer, int width) {
        BufferHelper.writeHeader(buffer, " Build Metrics ", width);
    }

    private void renderSummary(Buffer buffer, int width) {
        int x = 1;
        x += buffer.setString(x, 2, "Build Duration: ", Style.create().cyan());
        x += buffer.setString(x, 2, formatDuration(duration), Style.create().green());
        x += buffer.setString(x, 2, "  ", Style.EMPTY);
        x += buffer.setString(x, 2, "Threads: ", Style.create().cyan());
        x += buffer.setString(x, 2, String.valueOf(numberOfThreads), Style.create().cyan());
        x += buffer.setString(x, 2, "  ", Style.EMPTY);
        x += buffer.setString(x, 2, "Steps: ", Style.create().cyan());
        x += buffer.setString(x, 2, String.valueOf(buildSteps.size()), Style.create().cyan());
        x += buffer.setString(x, 2, "  ", Style.EMPTY);
        x += buffer.setString(x, 2, "Items: ", Style.create().cyan());
        buffer.setString(x, 2, String.valueOf(buildItems.size()), Style.create().cyan());
    }

    private void renderTabs(Buffer buffer, int width) {
        int x = 1;
        for (Tab tab : Tab.values()) {
            int count = tab == Tab.BUILD_STEPS ? buildSteps.size() : buildItems.size();
            String label = " " + tab.label + " (" + count + ") ";
            if (tab == currentTab) {
                x += buffer.setString(x, 4, label, Style.create().bold().reversed());
            } else {
                x += buffer.setString(x, 4, label, Style.create().gray());
            }
            x += buffer.setString(x, 4, " ", Style.EMPTY);
        }
    }

    private void renderFooter(Buffer buffer, int width, int height) {
        buffer.setString(1, height - 2, "[Tab] Switch tab  [R] Refresh  [Esc] Back", Style.create().gray());
    }

    @Override
    public boolean handleKey(int key) {
        if (loading) {
            return true;
        }

        // Let current table handle navigation
        TableView<?> currentTable = currentTab == Tab.BUILD_STEPS ? stepsTable : itemsTable;
        if (currentTable.handleKey(key)) {
            this.ctx.requestRedraw();
            return true;
        }

        switch (key) {
            case KeyCode.ESCAPE:
                this.ctx.goBack();
                return true;

            case KeyCode.TAB:
                // Switch tabs
                Tab[] tabs = Tab.values();
                int current = currentTab.ordinal();
                currentTab = tabs[(current + 1) % tabs.length];
                this.ctx.requestRedraw();
                return true;

            case 'r':
            case 'R':
                loadBuildMetrics();
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onResize(int width, int height) {
        stepsTable.setWidth(width - 4);
        stepsTable.setVisibleRows(height - 8);
        itemsTable.setWidth(width - 4);
        itemsTable.setVisibleRows(height - 8);
    }

    private String formatStepName(BuildStep step) {
        String name = step.stepId;
        // Extract class name from full step ID
        // Format: io.quarkus.deployment.SomeProcessor#someMethod
        int hashIdx = name.lastIndexOf('#');
        if (hashIdx > 0) {
            String className = name.substring(0, hashIdx);
            String methodName = name.substring(hashIdx + 1);
            int dotIdx = className.lastIndexOf('.');
            if (dotIdx > 0) {
                className = className.substring(dotIdx + 1);
            }
            return className + "#" + methodName;
        }
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx > 0) {
            return name.substring(dotIdx + 1);
        }
        return name;
    }

    private String formatItemName(BuildItem item) {
        String name = item.className;
        // Extract simple class name
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx > 0) {
            return name.substring(dotIdx + 1);
        }
        return name;
    }

    private String formatDuration(long nanos) {
        if (nanos < 1_000_000) {
            return String.format("%.2f \u03BCs", nanos / 1000.0);
        } else if (nanos < 1_000_000_000) {
            return String.format("%.2f ms", nanos / 1_000_000.0);
        } else {
            return String.format("%.2f s", nanos / 1_000_000_000.0);
        }
    }

    private static long toLong(Object value, long defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Data class for build step information.
     */
    private static class BuildStep {
        String stepId;
        long duration;
        String thread;
        long started;
    }

    /**
     * Data class for build item information.
     */
    private static class BuildItem {
        String className;
        int count;
    }
}
