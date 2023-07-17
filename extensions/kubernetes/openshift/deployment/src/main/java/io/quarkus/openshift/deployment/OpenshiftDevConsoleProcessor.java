package io.quarkus.openshift.deployment;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.container.image.deployment.devconsole.RebuildHandler;
import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.console.TempSystemProperties;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.openshift.runtime.devui.OpenshiftDeploymentJsonRpcService;
import io.vertx.core.json.JsonArray;

public class OpenshiftDevConsoleProcessor {

    @BuildStep
    DevConsoleRouteBuildItem builder() {
        return new DevConsoleRouteBuildItem("deploy", "POST",
                new RebuildHandler(Collections.singletonMap("quarkus.kubernetes.deploy", "true")));
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem create(List<AvailableContainerImageExtensionBuildItem> extensions) {
        // Get the list of builders
        JsonArray array = extensions.stream().map(AvailableContainerImageExtensionBuildItem::getName).sorted()
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

        CardPageBuildItem card = new CardPageBuildItem();
        card.addBuildTimeData("builderTypes", array);
        card.addPage(Page.webComponentPageBuilder()
                .title("Deploy to OpenShift")
                .componentLink("qwc-openshift-deployment.js")
                .icon("font-awesome-solid:box"));
        return card;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCServiceForContainerBuild() {
        DevConsoleManager.register("openshift-deployment-action", build());
        return new JsonRPCProvidersBuildItem(OpenshiftDeploymentJsonRpcService.class);
    }

    private Function<Map<String, String>, String> build() {
        return (map -> {

            QuarkusBootstrap existing = (QuarkusBootstrap) DevConsoleManager.getQuarkusBootstrap();
            try (TempSystemProperties properties = new TempSystemProperties()) {
                properties.set("quarkus.kubernetes.deploy", "true");
                for (Map.Entry<String, String> arg : map.entrySet()) {
                    properties.set(arg.getKey(), arg.getValue());
                }
                QuarkusBootstrap quarkusBootstrap = existing.clonedBuilder()
                        .setMode(QuarkusBootstrap.Mode.PROD)
                        .setIsolateDeployment(true).build();
                try (CuratedApplication bootstrap = quarkusBootstrap.bootstrap()) {
                    bootstrap
                            .createAugmentor().createProductionApplication();
                    return quarkusBootstrap.getBaseName() + " deployed successfully";
                } catch (Exception e) {
                    return getRootMessage(e);
                }
            }
        });
    }

    private String getRootMessage(Throwable e) {
        if (e.getCause() != null) {
            return getRootMessage(e.getCause());
        }
        return e.getMessage();
    }

}
