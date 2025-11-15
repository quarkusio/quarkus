package io.quarkus.test.junit;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.builditem.DevServicesCustomizerBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesNetworkIdBuildItem;
import io.quarkus.deployment.builditem.DevServicesRegistryBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;

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

        List<DevServicesResultBuildItem> devServices = buildResult.consumeMulti(DevServicesResultBuildItem.class);
        DevServicesRegistryBuildItem devServicesRegistry = buildResult.consumeOptional(DevServicesRegistryBuildItem.class);
        List<DevServicesCustomizerBuildItem> customizers = buildResult.consumeMulti(DevServicesCustomizerBuildItem.class);
        if (devServicesRegistry != null) {
            devServicesRegistry.startAll(devServices, customizers, null);
            for (Map.Entry<String, String> entry : devServicesRegistry.getConfigForAllRunningServices().entrySet()) {
                propertyConsumer.accept(entry.getKey(), entry.getValue());
            }
        }
    }
}
