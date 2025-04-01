package io.quarkus.test.junit;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesNetworkIdBuildItem;
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

        List<DevServicesResultBuildItem> devServicesResultBuildItems = buildResult
                .consumeMulti(DevServicesResultBuildItem.class);
        for (DevServicesResultBuildItem devServicesResultBuildItem : devServicesResultBuildItems) {
            devServicesResultBuildItem.start();

            // It would be nice to use the config source, but since we have the build item right there and this is a one-shot operation, just ask it instead
            Map<String, String> dynamicConfig = devServicesResultBuildItem.getDynamicConfig();
            for (Map.Entry<String, String> entry : dynamicConfig.entrySet()) {
                propertyConsumer.accept(entry.getKey(), entry.getValue());

            }
        }
    }
}
