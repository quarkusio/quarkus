package io.quarkus.test.junit;

import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesNetworkIdBuildItem;

public class NativeDevServicesHandler implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        BiConsumer<String, String> propertyConsumer = (BiConsumer<String, String>) o;

        DevServicesNetworkIdBuildItem compose = buildResult.consumeOptional(DevServicesNetworkIdBuildItem.class);
        DevServicesLauncherConfigResultBuildItem devServicesProperties = buildResult
                .consume(DevServicesLauncherConfigResultBuildItem.class);
        for (var entry : devServicesProperties.getConfig().entrySet()) {
            propertyConsumer.accept(entry.getKey(), entry.getValue());
        }
        if (compose != null && compose.getNetworkId() != null) {
            propertyConsumer.accept("quarkus.test.container.network", compose.getNetworkId());
        }
    }
}
