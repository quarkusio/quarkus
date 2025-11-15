package io.quarkus.devui.deployment.menu;

import java.io.IOException;
import java.util.List;

import io.quarkus.builder.Version;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devui.runtime.DevUIRecorder;
import io.quarkus.devui.runtime.mcp.McpDevUIJsonRpcService;
import io.quarkus.devui.runtime.mcp.McpResourcesService;
import io.quarkus.devui.runtime.mcp.McpToolsService;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.SettingPageBuildItem;
import io.quarkus.devui.spi.page.UnlistedPageBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

public class MCPProcessor {

    private static final String DEVMCP = "dev-mcp";

    private static final String NS_MCP = "devmcp";
    private static final String NS_RESOURCES = "resources";
    private static final String NS_TOOLS = "tools";

    @BuildStep(onlyIf = IsDevelopment.class)
    void createMCPPage(BuildProducer<SettingPageBuildItem> settingPageProducer,
            BuildProducer<UnlistedPageBuildItem> unlistedPageProducer) {

        SettingPageBuildItem mcpSettingTab = new SettingPageBuildItem(NS_MCP);

        mcpSettingTab.addPage(Page.webComponentPageBuilder()
                .namespace(NS_MCP)
                .internal("Dev MCP")
                .title("Dev MCP")
                .icon("font-awesome-solid:robot")
                .componentLink("qwc-dev-mcp-setting.js"));
        settingPageProducer.produce(mcpSettingTab);

        UnlistedPageBuildItem mcpOtherPages = new UnlistedPageBuildItem(NS_MCP);

        mcpOtherPages.addPage(Page.webComponentPageBuilder()
                .namespace(NS_MCP)
                .internal("Dev MCP")
                .title("Tools")
                .icon("font-awesome-solid:screwdriver-wrench")
                .componentLink("qwc-dev-mcp-tools.js"));
        mcpOtherPages.addPage(Page.webComponentPageBuilder()
                .namespace(NS_MCP)
                .internal("Dev MCP")
                .title("Resources")
                .icon("font-awesome-solid:file-invoice")
                .componentLink("qwc-dev-mcp-resources.js"));
        unlistedPageProducer.produce(mcpOtherPages);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @io.quarkus.deployment.annotations.Record(ExecutionTime.STATIC_INIT)
    void registerStreamableHTTPHandlers(
            BuildProducer<RouteBuildItem> routeProducer,
            DevUIRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) throws IOException {

        if (launchModeBuildItem.isNotLocalDevModeType()) {
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
    void createMCPJsonRPCService(BuildProducer<JsonRPCProvidersBuildItem> bp) {
        bp.produce(List.of(new JsonRPCProvidersBuildItem(NS_RESOURCES, McpResourcesService.class),
                new JsonRPCProvidersBuildItem(NS_TOOLS, McpToolsService.class),
                new JsonRPCProvidersBuildItem(NS_MCP, McpDevUIJsonRpcService.class)));

    }
}
