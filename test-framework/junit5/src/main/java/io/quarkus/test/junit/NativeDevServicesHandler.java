package io.quarkus.test.junit;

import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;

public class NativeDevServicesHandler implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        BiConsumer<String, String> propertyConsumer = (BiConsumer<String, String>) o;

        DevServicesLauncherConfigResultBuildItem devServicesProperties = buildResult
                .consume(DevServicesLauncherConfigResultBuildItem.class);
        for (var entry : devServicesProperties.getConfig().entrySet()) {
            propertyConsumer.accept(entry.getKey(), entry.getValue());
        }

    }
}
