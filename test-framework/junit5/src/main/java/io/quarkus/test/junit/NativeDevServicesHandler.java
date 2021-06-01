package io.quarkus.test.junit;

import java.util.List;
import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.deployment.builditem.DevServicesNativeConfigResultBuildItem;

public class NativeDevServicesHandler implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        BiConsumer<String, String> propertyConsumer = (BiConsumer<String, String>) o;

        List<DevServicesNativeConfigResultBuildItem> devServicesProperties = buildResult
                .consumeMulti(DevServicesNativeConfigResultBuildItem.class);
        for (DevServicesNativeConfigResultBuildItem entry : devServicesProperties) {
            propertyConsumer.accept(entry.getKey(), entry.getValue());
        }

    }
}
