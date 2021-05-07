package io.quarkus.extest.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.UberJarIgnoredResourceBuildItem;
import io.quarkus.deployment.pkg.builditem.UberJarMergedResourceBuildItem;

/**
 * Used in UberJarMergedResourceBuildItemTest
 */
public class UberJarConfigBuildStep {

    @BuildStep
    UberJarMergedResourceBuildItem uberJarMergedResourceBuildItem() {
        return new UberJarMergedResourceBuildItem("META-INF/cxf/bus-extensions.txt");
    }

    @BuildStep
    UberJarIgnoredResourceBuildItem uberJarIgnoredResourceBuildItem() {
        return new UberJarIgnoredResourceBuildItem("META-INF/cxf/cxf.fixml");
    }

}
