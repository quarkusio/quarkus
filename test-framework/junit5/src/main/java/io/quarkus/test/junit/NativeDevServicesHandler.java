package io.quarkus.test.junit;

import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;

public class NativeDevServicesHandler implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        BiConsumer<String, String> propertyConsumer = (BiConsumer<String, String>) o;

        DevServicesComposeProjectBuildItem compose = buildResult.consumeOptional(DevServicesComposeProjectBuildItem.class);
        DevServicesLauncherConfigResultBuildItem devServicesProperties = buildResult
                .consume(DevServicesLauncherConfigResultBuildItem.class);
        for (var entry : devServicesProperties.getConfig().entrySet()) {
            propertyConsumer.accept(entry.getKey(), entry.getValue());
        }
        if (compose != null && compose.getDefaultNetworkId() != null) {
            propertyConsumer.accept("quarkus.test.container.network", compose.getDefaultNetworkId());
        }
    }
}
