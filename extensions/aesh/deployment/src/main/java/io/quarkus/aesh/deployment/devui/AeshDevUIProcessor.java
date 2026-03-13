package io.quarkus.aesh.deployment.devui;

import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.aesh.deployment.AeshCommandBuildItem;
import io.quarkus.aesh.deployment.AeshModeBuildItem;
import io.quarkus.aesh.deployment.AeshRemoteTransportBuildItem;
import io.quarkus.aesh.runtime.devui.AeshJsonRPCService;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;

class AeshDevUIProcessor {

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void pages(List<AeshCommandBuildItem> commands,
            AeshModeBuildItem mode,
            List<AeshRemoteTransportBuildItem> transports,
            BuildProducer<CardPageBuildItem> cardPages) {

        CardPageBuildItem pageBuildItem = new CardPageBuildItem();

        // Commands page -- always present
        pageBuildItem.addPage(Page.webComponentPageBuilder()
                .title("Commands")
                .icon("font-awesome-solid:terminal")
                .componentLink("qwc-aesh-commands.js")
                .staticLabel(String.valueOf(commands.size())));

        // Build-time data for commands page
        pageBuildItem.addBuildTimeData("commands", commands.stream()
                .map(cmd -> {
                    var map = new java.util.LinkedHashMap<String, Object>();
                    map.put("name", cmd.getCommandName());
                    map.put("description", cmd.getDescription() != null ? cmd.getDescription() : "");
                    map.put("className", cmd.getClassName());
                    map.put("groupCommand", cmd.isGroupCommand());
                    map.put("topCommand", cmd.isTopCommand());
                    map.put("cliCommand", cmd.isCliCommand());
                    if (cmd.getSubCommandClassNames() != null && !cmd.getSubCommandClassNames().isEmpty()) {
                        map.put("subCommands", cmd.getSubCommandClassNames());
                    }
                    return map;
                })
                .collect(Collectors.toList()));
        pageBuildItem.addBuildTimeData("mode", mode.getResolvedMode().name());

        List<String> transportNames = transports.stream()
                .map(AeshRemoteTransportBuildItem::getName)
                .collect(Collectors.toList());
        pageBuildItem.addBuildTimeData("transports", transportNames);

        // Sessions page -- only if remote transports are present
        if (!transports.isEmpty()) {
            pageBuildItem.addPage(Page.webComponentPageBuilder()
                    .title("Sessions")
                    .icon("font-awesome-solid:plug")
                    .componentLink("qwc-aesh-sessions.js"));
        }

        // Terminal page -- only if websocket transport is present
        boolean hasWebSocket = transports.stream()
                .anyMatch(t -> "websocket".equals(t.getName()));
        if (hasWebSocket) {
            pageBuildItem.addPage(Page.webComponentPageBuilder()
                    .title("Terminal")
                    .icon("font-awesome-solid:rectangle-terminal")
                    .componentLink("qwc-aesh-terminal.js"));

            pageBuildItem.addBuildTimeData("websocketPath", "/aesh/terminal");
        }

        cardPages.produce(pageBuildItem);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem rpcProvider() {
        return new JsonRPCProvidersBuildItem(AeshJsonRPCService.class);
    }
}
