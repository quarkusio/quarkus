package org.acme.extension.deployment;

import java.nio.charset.StandardCharsets;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;

public class IncludedExtensionProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem("included-composite");
    }

    @BuildStep
    GeneratedResourceBuildItem deploymentMarker() {
        return new GeneratedResourceBuildItem("included-extension-deployment.txt",
                "included extension deployment selected".getBytes(StandardCharsets.UTF_8));
    }
}
