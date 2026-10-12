package io.quarkus.devmcp.deployment;

import java.util.List;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.builder.Version;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.devjsonrpc.deployment.JsonRpcMapperBuildItem;
import io.quarkus.devjsonrpc.deployment.JsonRpcRouterBuildItem;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devmcp.runtime.DevMcpRecorder;
import io.quarkus.devmcp.runtime.McpBuildTimeData;
import io.quarkus.devmcp.runtime.McpDevUIJsonRpcService;
import io.quarkus.devmcp.runtime.McpResourcesService;
import io.quarkus.devmcp.runtime.McpToolsService;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.SettingPageBuildItem;
import io.quarkus.devui.spi.page.UnlistedPageBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.security.SecurityHandlerPriorities;

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
    @Record(ExecutionTime.STATIC_INIT)
    void registerStreamableHTTPFilters(
            DevMcpBuildTimeConfig devMcpConfig,
            BuildProducer<RouteBuildItem> routeProducer,
            DevMcpRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        if (launchModeBuildItem.isNotLocalDevModeType()) {
            return;
        }

        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .orderedRoute(DEVMCP, -2 * SecurityHandlerPriorities.CORS)
                .handler(recorder.createLocalHostOnlyFilter(devMcpConfig.hosts().orElse(null)))
                .build());

        if (devMcpConfig.cors().enabled()) {
            routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                    .orderedRoute(DEVMCP, -1 * SecurityHandlerPriorities.CORS)
                    .handler(recorder.createCorsFilter(devMcpConfig.hosts().orElse(null)))
                    .build());
        }
    }

    /**
     * The Streamable HTTP handler for JsonRPC comms. This is registered at runtime init, after the JsonRPC router is
     * initialized, so that the endpoint is never served with a router that is still being initialized.
     */
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @Consume(JsonRpcRouterBuildItem.class)
    void registerStreamableHTTPHandler(
            JsonRpcMapperBuildItem jsonRpcMapperBuildItem,
            BuildProducer<RouteBuildItem> routeProducer,
            DevMcpRecorder recorder,
            LaunchModeBuildItem launchModeBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {

        if (launchModeBuildItem.isNotLocalDevModeType()) {
            return;
        }

        routeProducer.produce(
                nonApplicationRootPathBuildItem
                        .routeBuilder().route(DEVMCP)
                        .handler(recorder.mcpStreamableHTTPHandler(Version.getVersion(),
                                jsonRpcMapperBuildItem.getJsonMapper()))
                        .build());
    }

    /**
     * The JSON mapper used by the JsonRPC router and the MCP endpoint, which knows about the Vert.x JSON types.
     */
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    JsonRpcMapperBuildItem createJsonRpcMapper(DevMcpRecorder recorder) {
        return new JsonRpcMapperBuildItem(recorder.createJsonMapper());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void logDevMcpEndpoint(DevMcpRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {
        if (launchModeBuildItem.isNotLocalDevModeType()) {
            return;
        }
        String path = nonApplicationRootPathBuildItem.resolvePath(DEVMCP);
        recorder.logDevMcpEndpoint(path);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {
        additionalBeanProducer.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(McpBuildTimeData.class)
                .setDefaultScope(BuiltinScope.APPLICATION.getName())
                .setUnremovable().build());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void createMCPJsonRPCService(BuildProducer<JsonRPCProvidersBuildItem> bp) {
        bp.produce(List.of(new JsonRPCProvidersBuildItem(NS_RESOURCES, McpResourcesService.class),
                new JsonRPCProvidersBuildItem(NS_TOOLS, McpToolsService.class),
                new JsonRPCProvidersBuildItem(NS_MCP, McpDevUIJsonRpcService.class)));

    }
}
