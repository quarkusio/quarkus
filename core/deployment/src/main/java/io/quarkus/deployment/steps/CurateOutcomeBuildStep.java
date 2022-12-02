package io.quarkus.deployment.steps;

import java.util.Map;
import java.util.Set;

import io.quarkus.deployment.BootstrapConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AppModelProviderBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;

public class CurateOutcomeBuildStep {

    BootstrapConfig config;

    @BuildStep
    CurateOutcomeBuildItem curateOutcome(AppModelProviderBuildItem appModelProvider) {
        return new CurateOutcomeBuildItem(appModelProvider.validateAndGet(config));
    }

    @BuildStep
    void removeResources(CurateOutcomeBuildItem curateOutcome,
            BuildProducer<RemovedResourceBuildItem> removedResourceProducer) {
        final Map<ArtifactKey, Set<String>> excludedResources = curateOutcome.getApplicationModel().getRemovedResources();
        if (!excludedResources.isEmpty()) {
            for (Map.Entry<ArtifactKey, Set<String>> removed : excludedResources.entrySet()) {
                removedResourceProducer.produce(new RemovedResourceBuildItem(removed.getKey(), removed.getValue()));
            }
        }
    }
}
