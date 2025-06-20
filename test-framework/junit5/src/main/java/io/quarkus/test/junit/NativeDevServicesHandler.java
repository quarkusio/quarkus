package io.quarkus.test.junit;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesNetworkIdBuildItem;
import io.quarkus.deployment.builditem.DevServicesRegistryBuildItem;
import io.quarkus.deployment.builditem.DevServicesRequestBuildItem;

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

        List<DevServicesRequestBuildItem> devServicesRequests = buildResult
                .consumeMulti(DevServicesRequestBuildItem.class);
        DevServicesRegistryBuildItem devServicesRegistry = buildResult.consumeOptional(DevServicesRegistryBuildItem.class);
        if (devServicesRegistry != null) {
            for (DevServicesRequestBuildItem serviceRequest : devServicesRequests) {
                devServicesRegistry.start(serviceRequest);
            }
            // It would be nice to use the config source, but since we have the build item right there and this is a one-shot operation, just ask it instead
            for (Map.Entry<String, String> entry : devServicesRegistry.getConfigForAllRunningServices().entrySet()) {
                propertyConsumer.accept(entry.getKey(), entry.getValue());
            }
        }
    }
}
