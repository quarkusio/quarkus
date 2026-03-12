package io.quarkus.micrometer.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.runtime.tui.widgets.ListView;
import io.quarkus.micrometer.runtime.dev.MicrometerJsonRpcService;

/**
 * Shell page for Micrometer extension.
 * Displays metrics summary and list of meters.
 */
public class MicrometerShellPage extends BaseExtensionPage {

    @Inject
    MicrometerJsonRpcService micrometerService;

    private ListView<MeterInfo> meterList;

    // Parsed data
    private List<MeterInfo> meters = new ArrayList<>();
    private int totalMetrics = 0;
    private int counters = 0;
    private int gauges = 0;
    private int timers = 0;
    private int other = 0;

    // No-arg constructor for CDI
    public MicrometerShellPage() {
        setTabs("Info", "Metrics");
        setTabArrowNavigation(true);
        this.meterList = new ListView<>(m -> {
            return String.format("%-10s", m.type) + " " + m.name +
                    (m.value != null ? " = " + m.value : "");
        });
    }

    public MicrometerShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        meters.clear();
        redraw();

        try {
            // Load summary
            Map<String, Object> summary = micrometerService.getSummary();
            if (summary != null) {
                totalMetrics = toInt(summary.get("total"));
                counters = toInt(summary.get("counters"));
                gauges = toInt(summary.get("gauges"));
                timers = toInt(summary.get("timers"));
                other = toInt(summary.get("other"));
            }

            // Load metrics
            List<Map<String, Object>> metricsList = micrometerService.getMetrics();
            if (metricsList != null) {
                for (Map<String, Object> meterData : metricsList) {
                    String name = (String) meterData.get("name");
                    String type = (String) meterData.get("type");
                    if (name == null) {
                        continue;
                    }

                    String value = null;
                    if (meterData.containsKey("value")) {
                        Double v = toDouble(meterData.get("value"));
                        if (v != null && !Double.isNaN(v)) {
                            value = formatValue(v);
                        }
                    } else if (meterData.containsKey("count")) {
                        Long count = toLong(meterData.get("count"));
                        Double mean = toDouble(meterData.get("mean"));
                        value = (count != null ? count : 0) + " calls, "
                                + formatValue(mean != null ? mean : 0.0) + "ms avg";
                    }

                    meters.add(new MeterInfo(name, type != null ? type : "UNKNOWN", value));
                }
            }

            loading = false;
            meterList.setItems(meters);
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load metrics: " + ex.getMessage();
            redraw();
        }
    }

    private int toInt(Object val) {
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    private Double toDouble(Object val) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return null;
    }

    private Long toLong(Object val) {
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return null;
    }

    private String formatValue(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        int row = 3;

        // Render tabs
        row = renderTabBar(buffer, row);

        if (loading) {
            renderLoading(buffer, row);
        } else if (error != null) {
            renderError(buffer, row);
        } else {
            switch (getCurrentTabIndex()) {
                case 0:
                    renderInfoTab(buffer, row);
                    break;
                case 1:
                    renderMetricsTab(buffer, row);
                    break;
            }
        }

        renderFooter(buffer, "");
    }

    private void renderInfoTab(Buffer buffer, int row) {
        KeyValuePanel panel = new KeyValuePanel("Micrometer Metrics");
        panel.add("Total Meters", String.valueOf(totalMetrics));

        panel.addBlank();

        if (counters > 0) {
            panel.addStyled("Counters", String.valueOf(counters), Style.create().green());
        }
        if (gauges > 0) {
            panel.addStyled("Gauges", String.valueOf(gauges), Style.create().cyan());
        }
        if (timers > 0) {
            panel.addStyled("Timers", String.valueOf(timers), Style.create().yellow());
        }
        if (other > 0) {
            panel.addStyled("Other", String.valueOf(other), Style.create().white());
        }

        panel.render(buffer, row, 2, width - 4);
    }

    private void renderMetricsTab(Buffer buffer, int row) {
        row++;

        if (meters.isEmpty()) {
            buffer.setString(1, row, "No metrics found", Style.create().gray());
        } else {
            meterList.setVisibleRows(height - row - 4);
            meterList.setWidth(width - 4);
            meterList.render(buffer, row, 2);
        }
    }

    @Override
    public boolean handleKey(int key) {
        // Let list handle navigation when on the Metrics tab
        if (getCurrentTabIndex() == 1 && meterList.handleKey(key)) {
            redraw();
            return true;
        }

        return super.handleKey(key);
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        meterList.setVisibleRows(height - 10);
        meterList.setWidth(width - 4);
    }

    @Override
    protected void renderPanelContent(Buffer buffer, int startRow, int startCol, int panelWidth, int panelHeight) {
        int row = startRow;

        if (loading) {
            renderPanelLoading(buffer, row, startCol);
            return;
        }

        if (error != null) {
            renderPanelError(buffer, row, startCol, panelWidth);
            return;
        }

        KeyValuePanel summary = new KeyValuePanel();
        summary.add("Total", String.valueOf(totalMetrics));
        if (counters > 0) {
            summary.add("Counters", String.valueOf(counters));
        }
        if (gauges > 0) {
            summary.add("Gauges", String.valueOf(gauges));
        }
        if (timers > 0) {
            summary.add("Timers", String.valueOf(timers));
        }
        summary.render(buffer, row, startCol, panelWidth);
    }

    // Data class
    private static class MeterInfo {
        final String name;
        final String type;
        final String value;

        MeterInfo(String name, String type, String value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }
    }
}
