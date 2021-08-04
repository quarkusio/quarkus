package io.quarkus.container.image.deployment.devconsole;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.app.ArtifactResult;
import io.quarkus.bootstrap.app.AugmentResult;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.dev.devui.DevConsoleManager;
import io.quarkus.dev.console.TempSystemProperties;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public class RebuildHandler extends DevConsolePostHandler {

    private final Map<String, String> config;

    public RebuildHandler(Map<String, String> config) {
        this.config = config;
    }

    @Override
    protected void handlePostAsync(RoutingContext event, MultiMap form) throws Exception {
        QuarkusBootstrap existing = (QuarkusBootstrap) DevConsoleManager.getQuarkusBootstrap();
        try (TempSystemProperties properties = new TempSystemProperties()) {
            for (Map.Entry<String, String> i : config.entrySet()) {
                properties.set(i.getKey(), i.getValue());
            }
            for (Map.Entry<String, String> i : form.entries()) {
                if (!i.getValue().isEmpty()) {
                    properties.set(i.getKey(), i.getValue());
                }
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
                    flashMessage(event,
                            "Container-image: " + containerArtifactResults.get(0).getMetadata().get("container-image")
                                    + " created.",
                            Duration.ofSeconds(10));
                }
            }
        }
        ;
    }

}
