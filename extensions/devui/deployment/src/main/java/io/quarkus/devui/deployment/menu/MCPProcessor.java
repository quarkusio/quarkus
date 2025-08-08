package io.quarkus.devui.deployment.menu;

import java.io.IOException;
import java.util.List;

import io.quarkus.builder.Version;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.deployment.DevMCPConfig;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.runtime.DevUIRecorder;
import io.quarkus.devui.runtime.mcp.DevMcpJsonRpcService;
import io.quarkus.devui.runtime.mcp.McpResourcesService;
import io.quarkus.devui.runtime.mcp.McpToolsService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

public class MCPProcessor {

    private static final String DEVMCP = "dev-mcp";

    private static final String NS_MCP = "devmcp";
    private static final String NS_RESOURCES = "resources";
    private static final String NS_TOOLS = "tools";

    @BuildStep(onlyIf = IsDevelopment.class)
    void createMCPPage(BuildProducer<InternalPageBuildItem> internalPageProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem, DevMCPConfig devMCPConfig) {
        if (devMCPConfig.enabled()) {
            InternalPageBuildItem mcpServerPage = new InternalPageBuildItem("Dev MCP", 80);

            // Pages
            mcpServerPage.addPage(Page.webComponentPageBuilder()
                    .namespace(NS_MCP)
                    .title("Info")
                    .icon("font-awesome-solid:robot")
                    .componentLink("qwc-dev-mcp-info.js"));

            mcpServerPage.addPage(Page.webComponentPageBuilder()
                    .namespace(NS_MCP)
                    .title("Tools")
                    .icon("font-awesome-solid:screwdriver-wrench")
                    .componentLink("qwc-dev-mcp-tools.js"));

            mcpServerPage.addPage(Page.webComponentPageBuilder()
                    .namespace(NS_MCP)
                    .title("Resources")
                    .icon("font-awesome-solid:file-invoice")
                    .componentLink("qwc-dev-mcp-resources.js"));
            internalPageProducer.produce(mcpServerPage);
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @io.quarkus.deployment.annotations.Record(ExecutionTime.STATIC_INIT)
    void registerDevUiHandlers(
            BuildProducer<RouteBuildItem> routeProducer,
            DevUIRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            DevMCPConfig devMCPConfig) throws IOException {

        if (launchModeBuildItem.isNotLocalDevModeType() || !devMCPConfig.enabled()) {
            return;
        }

        // Streamable HTTP for JsonRPC comms
        routeProducer.produce(
                nonApplicationRootPathBuildItem
                        .routeBuilder().route(DEVMCP)
                        .handler(recorder.mcpStreamableHTTPHandler(Version.getVersion()))
                        .build());

    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void createMCPJsonRPCService(BuildProducer<JsonRPCProvidersBuildItem> bp, DevMCPConfig devMCPConfig) {
        if (devMCPConfig.enabled()) {
            bp.produce(List.of(new JsonRPCProvidersBuildItem(NS_RESOURCES, McpResourcesService.class),
                    new JsonRPCProvidersBuildItem(NS_TOOLS, McpToolsService.class),
                    new JsonRPCProvidersBuildItem(NS_MCP, DevMcpJsonRpcService.class)));
        }
    }
}
