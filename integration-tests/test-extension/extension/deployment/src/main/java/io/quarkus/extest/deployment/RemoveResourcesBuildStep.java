package io.quarkus.extest.deployment;

import java.util.Set;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.extest.runtime.RemovedResource;
import io.quarkus.maven.dependency.ArtifactKey;

public class RemoveResourcesBuildStep {

    @BuildStep
    void removeResources(BuildProducer<RemovedResourceBuildItem> removed) {
        removed.produce(new RemovedResourceBuildItem(
                ArtifactKey.of("io.smallrye.common", "smallrye-common-net", null, "jar"),
                Set.of(RemovedResource.COMMON_NET_MESSAGES.resourceName())));
    }
}
