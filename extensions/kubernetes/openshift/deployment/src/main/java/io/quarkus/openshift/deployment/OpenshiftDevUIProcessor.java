package io.quarkus.openshift.deployment;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.console.TempSystemProperties;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.vertx.core.json.JsonArray;

public class OpenshiftDevUIProcessor {

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
    BuildTimeActionBuildItem createBuildTimeActionsForContainerBuild() {
        BuildTimeActionBuildItem deployActions = new BuildTimeActionBuildItem();
        deployActions.addAction("build", build());
        return deployActions;
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
