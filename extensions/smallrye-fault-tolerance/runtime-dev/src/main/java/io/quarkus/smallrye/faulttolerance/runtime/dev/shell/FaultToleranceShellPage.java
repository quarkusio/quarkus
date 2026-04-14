package io.quarkus.smallrye.faulttolerance.runtime.dev.shell;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import io.quarkus.devshell.runtime.tui.ShellExtension;
import io.quarkus.devshell.runtime.tui.pages.BaseExtensionPage;
import io.quarkus.devshell.runtime.tui.widgets.KeyValuePanel;
import io.quarkus.devshell.runtime.tui.widgets.ListView;
import io.quarkus.smallrye.faulttolerance.runtime.devui.FaultToleranceJsonRpcService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Shell page for SmallRye Fault Tolerance extension.
 * Displays guarded methods with their fault tolerance configurations.
 */
public class FaultToleranceShellPage extends BaseExtensionPage {

    @Inject
    FaultToleranceJsonRpcService ftService;

    private ListView<GuardedMethod> methodList;

    // Parsed data
    private List<GuardedMethod> methods = new ArrayList<>();

    // Counts by type
    private int retryCount = 0;
    private int circuitBreakerCount = 0;
    private int timeoutCount = 0;
    private int fallbackCount = 0;
    private int bulkheadCount = 0;
    private int rateLimitCount = 0;

    // No-arg constructor for CDI
    public FaultToleranceShellPage() {
        setTabs("Info", "Methods");
        setTabArrowNavigation(true);
        this.methodList = new ListView<>(m -> {
            return m.simpleClassName + "." + m.method + " [" + m.annotations + "]";
        });
    }

    public FaultToleranceShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        methods.clear();
        resetCounts();
        redraw();

        try {
            JsonArray array = ftService.getGuardedMethods();
            loading = false;

            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.getJsonObject(i);
                    String beanClass = obj.getString("beanClass");
                    String method = obj.getString("method");

                    if (beanClass == null || method == null) {
                        continue;
                    }

                    String simpleClassName = beanClass;
                    int lastDot = beanClass.lastIndexOf('.');
                    if (lastDot >= 0) {
                        simpleClassName = beanClass.substring(lastDot + 1);
                    }

                    List<String> annotations = new ArrayList<>();

                    if (obj.containsKey("Retry")) {
                        annotations.add("Retry");
                        retryCount++;
                    }
                    if (obj.containsKey("CircuitBreaker")) {
                        annotations.add("CB");
                        circuitBreakerCount++;
                    }
                    if (obj.containsKey("Timeout")) {
                        annotations.add("Timeout");
                        timeoutCount++;
                    }
                    if (obj.containsKey("Fallback")) {
                        annotations.add("Fallback");
                        fallbackCount++;
                    }
                    if (obj.containsKey("Bulkhead")) {
                        annotations.add("Bulkhead");
                        bulkheadCount++;
                    }
                    if (obj.containsKey("RateLimit")) {
                        annotations.add("RateLimit");
                        rateLimitCount++;
                    }

                    methods.add(new GuardedMethod(beanClass, simpleClassName, method,
                            String.join(", ", annotations)));
                }
            }

            methodList.setItems(methods);
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load fault tolerance methods: " + ex.getMessage();
            redraw();
        }
    }

    private void resetCounts() {
        retryCount = 0;
        circuitBreakerCount = 0;
        timeoutCount = 0;
        fallbackCount = 0;
        bulkheadCount = 0;
        rateLimitCount = 0;
    }

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        int row = 3;

        // Render tab bar using base class helper
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
                    renderMethodsTab(buffer, row);
                    break;
            }
        }

        renderFooter(buffer, "");
    }

    private void renderInfoTab(Buffer buffer, int row) {
        KeyValuePanel panel = new KeyValuePanel("Fault Tolerance");
        panel.add("Guarded Methods", String.valueOf(methods.size()));
        panel.addBlank();

        boolean hasStrategies = retryCount > 0 || circuitBreakerCount > 0 || timeoutCount > 0 ||
                fallbackCount > 0 || bulkheadCount > 0 || rateLimitCount > 0;

        if (hasStrategies) {
            panel.add("By Strategy", "");
            if (retryCount > 0) {
                panel.addStyled("  Retry", String.valueOf(retryCount), Style.create().yellow());
            }
            if (circuitBreakerCount > 0) {
                panel.addStyled("  CircuitBreaker", String.valueOf(circuitBreakerCount), Style.create().red());
            }
            if (timeoutCount > 0) {
                panel.addStyled("  Timeout", String.valueOf(timeoutCount), Style.create().cyan());
            }
            if (fallbackCount > 0) {
                panel.addStyled("  Fallback", String.valueOf(fallbackCount), Style.create().green());
            }
            if (bulkheadCount > 0) {
                panel.addStyled("  Bulkhead", String.valueOf(bulkheadCount), Style.create().magenta());
            }
            if (rateLimitCount > 0) {
                panel.addStyled("  RateLimit", String.valueOf(rateLimitCount), Style.create().blue());
            }
        } else {
            panel.add("By Strategy", "(none)");
        }

        panel.render(buffer, row, 2, width - 4);
    }

    private void renderMethodsTab(Buffer buffer, int row) {
        row++;

        if (methods.isEmpty()) {
            buffer.setString(1, row, "No guarded methods found", Style.create().gray());
        } else {
            methodList.setVisibleRows(height - row - 8);
            methodList.setWidth(width - 4);
            methodList.render(buffer, row, 2);

            // Show selected method detail
            GuardedMethod selected = methodList.getSelectedItem();
            if (selected != null) {
                KeyValuePanel detail = new KeyValuePanel();
                detail.add("Class", selected.beanClass);
                detail.add("Method", selected.method);
                detail.add("Strategies", selected.annotations);
                detail.render(buffer, height - 7, 2, width - 4);
            }
        }
    }

    @Override
    public boolean handleKey(int key) {
        // Let list handle navigation when on Methods tab
        if (getCurrentTabIndex() == 1 && methodList.handleKey(key)) {
            redraw();
            return true;
        }

        return super.handleKey(key);
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        methodList.setVisibleRows(height - 10);
        methodList.setWidth(width - 4);
    }

    // Data class
    private static class GuardedMethod {
        final String beanClass;
        final String simpleClassName;
        final String method;
        final String annotations;

        GuardedMethod(String beanClass, String simpleClassName, String method, String annotations) {
            this.beanClass = beanClass;
            this.simpleClassName = simpleClassName;
            this.method = method;
            this.annotations = annotations;
        }
    }
}
