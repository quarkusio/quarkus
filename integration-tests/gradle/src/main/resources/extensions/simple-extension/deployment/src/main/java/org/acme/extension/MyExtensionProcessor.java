package org.acme.extension;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;

public class MyExtensionProcessor {

    private static final String MY_EXTENSION_FEATURE = "my-extension";

    @BuildStep
    public FeatureBuildItem produceFeatureBuildItem() {
        return new FeatureBuildItem(MY_EXTENSION_FEATURE);
    }

    @BuildStep
    ServletBuildItem createServlet() {
        ServletBuildItem servletBuildItem = ServletBuildItem.builder("my-extension", MyExtensionServlet.class.getName())
                .addMapping("/my-extension")
                .build();
        return servletBuildItem;
    }

}
