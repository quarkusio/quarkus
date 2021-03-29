package io.quarkus.test.junit;

import java.util.Map;
import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceResultBuildItem;

public class NativeDevServicesDatasourceHandler implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        BiConsumer<String, String> propertyConsumer = (BiConsumer<String, String>) o;
        DevServicesDatasourceResultBuildItem res = buildResult.consumeOptional(DevServicesDatasourceResultBuildItem.class);
        if (res != null) {
            if (res.getDefaultDatasource() != null) {
                for (Map.Entry<String, String> entry : res.getDefaultDatasource().getConfigProperties().entrySet()) {
                    propertyConsumer.accept(entry.getKey(), entry.getValue());
                }
            }
            for (DevServicesDatasourceResultBuildItem.DbResult i : res.getNamedDatasources().values()) {
                for (Map.Entry<String, String> entry : i.getConfigProperties().entrySet()) {
                    propertyConsumer.accept(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
