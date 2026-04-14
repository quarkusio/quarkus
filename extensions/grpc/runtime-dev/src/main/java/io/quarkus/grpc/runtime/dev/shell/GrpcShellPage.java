package io.quarkus.grpc.runtime.dev.shell;

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
import io.quarkus.grpc.runtime.dev.ui.GrpcJsonRPCService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Shell page for gRPC extension.
 * Displays gRPC services and their methods.
 */
public class GrpcShellPage extends BaseExtensionPage {

    @Inject
    GrpcJsonRPCService grpcService;

    private ListView<GrpcService> serviceList;
    private ListView<GrpcMethod> methodList;

    // Parsed data
    private List<GrpcService> services = new ArrayList<>();
    private GrpcService selectedService = null;

    // No-arg constructor for CDI
    public GrpcShellPage() {
        setTabs("Info", "Services", "Methods");
        setTabArrowNavigation(true);
        this.serviceList = new ListView<>(s -> {
            return String.format("%-8s", s.status) + " " + s.name + " (" + s.serviceClass + ")";
        });
        this.methodList = new ListView<>(m -> {
            String testableFlag = m.testable ? " [testable]" : "";
            return String.format("%-20s", m.type) + " " + m.name + testableFlag;
        });
    }

    public GrpcShellPage(ShellExtension extension) {
        this();
        setExtension(extension);
    }

    @Override
    public void loadData() {
        loading = true;
        error = null;
        services.clear();
        selectedService = null;
        redraw();

        try {
            JsonArray array = grpcService.getServices();
            loading = false;

            if (array != null) {
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.getJsonObject(i);
                    String name = obj.getString("name");
                    if (name == null) {
                        continue;
                    }
                    String status = obj.getString("status");
                    String serviceClass = obj.getString("serviceClass");
                    GrpcService service = new GrpcService(
                            status != null ? status : "UNKNOWN",
                            name,
                            serviceClass != null ? serviceClass : "");

                    JsonArray methodsArray = obj.getJsonArray("methods");
                    if (methodsArray != null) {
                        for (int j = 0; j < methodsArray.size(); j++) {
                            JsonObject methodObj = methodsArray.getJsonObject(j);
                            String methodName = methodObj.getString("bareMethodName");
                            if (methodName != null) {
                                String type = methodObj.getString("type");
                                boolean testable = methodObj.getBoolean("isTestable", false);
                                service.methods.add(new GrpcMethod(methodName, type != null ? type : "UNKNOWN", testable));
                            }
                        }
                    }
                    services.add(service);
                }
            }

            serviceList.setItems(services);
            if (!services.isEmpty()) {
                selectedService = services.get(0);
                methodList.setItems(selectedService.methods);
            }
            redraw();
        } catch (Exception ex) {
            loading = false;
            error = "Failed to load gRPC services: " + ex.getMessage();
            redraw();
        }
    }

    // Color methods removed - buffer rendering uses Style objects instead

    @Override
    public void render(Frame frame) {
        Buffer buffer = frame.buffer();
        renderHeader(buffer, width);

        int row = renderTabBar(buffer, 3);

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
                    renderServicesTab(buffer, row);
                    break;
                case 2:
                    renderMethodsTab(buffer, row);
                    break;
            }
        }

        renderFooter(buffer, "");
    }

    private void renderInfoTab(Buffer buffer, int row) {
        // Count by status
        int serving = 0, notServing = 0;
        int totalMethods = 0;
        for (GrpcService s : services) {
            if ("SERVING".equals(s.status)) {
                serving++;
            } else {
                notServing++;
            }
            totalMethods += s.methods.size();
        }

        KeyValuePanel summary = new KeyValuePanel("gRPC Services");
        summary.add("Total Services", String.valueOf(services.size()));
        summary.addStyled("Serving", String.valueOf(serving), Style.create().green());
        if (notServing > 0) {
            summary.addStyled("Not Serving", String.valueOf(notServing), Style.create().red());
        }
        summary.add("Total Methods", String.valueOf(totalMethods));
        row = summary.render(buffer, row, 2, width - 4);

        row += 2;

        // Method type legend
        KeyValuePanel legend = new KeyValuePanel("Method Types");
        legend.addStyled("UNARY", "Single request/response", Style.create().green());
        legend.addStyled("SERVER_STREAMING", "Server streams responses", Style.create().cyan());
        legend.addStyled("CLIENT_STREAMING", "Client streams requests", Style.create().yellow());
        legend.addStyled("BIDI_STREAMING", "Bidirectional streaming", Style.create().magenta());
        legend.render(buffer, row, 2, width - 4);
    }

    private void renderServicesTab(Buffer buffer, int row) {
        row++;

        if (services.isEmpty()) {
            buffer.setString(1, row, "No gRPC services found", Style.create().gray());
        } else {
            serviceList.setVisibleRows(height - row - 4);
            serviceList.setWidth(width - 4);
            serviceList.render(buffer, row, 2);
        }
    }

    private void renderMethodsTab(Buffer buffer, int row) {
        if (selectedService != null) {
            buffer.setString(1, row, "Methods for: " + selectedService.name, Style.create().cyan());
        } else {
            buffer.setString(1, row, "Select a service first", Style.create().gray());
        }
        row++;

        if (selectedService == null || selectedService.methods.isEmpty()) {
            buffer.setString(1, row, "No methods found", Style.create().gray());
        } else {
            methodList.setVisibleRows(height - row - 4);
            methodList.setWidth(width - 4);
            methodList.render(buffer, row, 2);
        }
    }

    @Override
    public boolean handleKey(int key) {
        // Let lists handle navigation on their respective tabs
        if (getCurrentTabIndex() == 1 && serviceList.handleKey(key)) {
            // Update selected service
            int idx = serviceList.getSelectedIndex();
            if (idx >= 0 && idx < services.size()) {
                selectedService = services.get(idx);
                methodList.setItems(selectedService.methods);
            }
            redraw();
            return true;
        }
        if (getCurrentTabIndex() == 2 && methodList.handleKey(key)) {
            redraw();
            return true;
        }

        return super.handleKey(key);
    }

    @Override
    public void onResize(int width, int height) {
        super.onResize(width, height);
        serviceList.setVisibleRows(height - 10);
        serviceList.setWidth(width - 4);
        methodList.setVisibleRows(height - 10);
        methodList.setWidth(width - 4);
    }

    // Data classes
    private static class GrpcService {
        final String status;
        final String name;
        final String serviceClass;
        final List<GrpcMethod> methods = new ArrayList<>();

        GrpcService(String status, String name, String serviceClass) {
            this.status = status;
            this.name = name;
            this.serviceClass = serviceClass;
        }
    }

    private static class GrpcMethod {
        final String name;
        final String type;
        final boolean testable;

        GrpcMethod(String name, String type, boolean testable) {
            this.name = name;
            this.type = type;
            this.testable = testable;
        }
    }
}
