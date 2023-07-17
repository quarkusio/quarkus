package io.quarkus.container.image.deployment.devconsole;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.container.image.runtime.devui.ContainerBuilderJsonRpcService;
import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.dev.console.TempSystemProperties;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.vertx.core.json.JsonArray;

public class ContainerImageDevUiProcessor {

    @BuildStep(onlyIf = IsDevelopment.class)
    CardPageBuildItem create(List<AvailableContainerImageExtensionBuildItem> extensions) {
        // Get the list of builders
        JsonArray array = extensions.stream().map(AvailableContainerImageExtensionBuildItem::getName).sorted()
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

        CardPageBuildItem card = new CardPageBuildItem();
        card.addBuildTimeData("builderTypes", array);
        card.addPage(Page.webComponentPageBuilder()
                .title("Build Container")
                .componentLink("qwc-container-image-build.js")
                .icon("font-awesome-solid:box"));
        return card;
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem createJsonRPCServiceForContainerBuild() {
        DevConsoleManager.register("container-image-build-action", build());
        return new JsonRPCProvidersBuildItem(ContainerBuilderJsonRpcService.class);
    }

    private Function<Map<String, String>, String> build() {
        return (map -> {
            QuarkusBootstrap existing = (QuarkusBootstrap) DevConsoleManager.getQuarkusBootstrap();
            try (TempSystemProperties properties = new TempSystemProperties()) {
                properties.set("quarkus.container-image.build", "true");
                for (Map.Entry<String, String> arg : map.entrySet()) {
                    properties.set(arg.getKey(), arg.getValue());
                }

                QuarkusBootstrap quarkusBootstrap = existing.clonedBuilder()
                        .setMode(QuarkusBootstrap.Mode.PROD)
                        .setIsolateDeployment(true).build();
                try (CuratedApplication bootstrap = quarkusBootstrap.bootstrap()) {
                    AugmentResult augmentResult = bootstrap
                            .createAugmentor().createProductionApplication();
                    List<ArtifactResult> containerArtifactResults = augmentResult
                            .resultsMatchingType((s) -> s.contains("container"));
                    if (containerArtifactResults.size() >= 1) {
                        return "Container image: " + containerArtifactResults.get(0).getMetadata().get("container-image")
                                + " created.";
                    } else {
                        return "Unknown error (image not created)";
                    }
                } catch (BootstrapException e) {
                    return e.getMessage();
                }
            }
        });
    }
}
