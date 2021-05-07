package io.quarkus.test.junit;

import java.util.Map;
import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceResultBuildItem;
import io.quarkus.mongodb.deployment.devservices.DevServicesMongoResultBuildItem;
import io.quarkus.redis.client.deployment.devservices.DevServicesRedisResultBuildItem;

public class NativeDevServicesHandler implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        BiConsumer<String, String> propertyConsumer = (BiConsumer<String, String>) o;
        DevServicesDatasourceResultBuildItem dsRes = buildResult.consumeOptional(DevServicesDatasourceResultBuildItem.class);
        if (dsRes != null) {
            if (dsRes.getDefaultDatasource() != null) {
                for (Map.Entry<String, String> entry : dsRes.getDefaultDatasource().getConfigProperties().entrySet()) {
                    propertyConsumer.accept(entry.getKey(), entry.getValue());
                }
            }
            for (DevServicesDatasourceResultBuildItem.DbResult i : dsRes.getNamedDatasources().values()) {
                for (Map.Entry<String, String> entry : i.getConfigProperties().entrySet()) {
                    propertyConsumer.accept(entry.getKey(), entry.getValue());
                }
            }
        }

        DevServicesMongoResultBuildItem mongoRes = buildResult.consumeOptional(DevServicesMongoResultBuildItem.class);
        if (mongoRes != null) {
            if (mongoRes.getDefaultConnection() != null) {
                Map<String, String> properties = mongoRes.getDefaultConnection().getProperties();
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    propertyConsumer.accept(entry.getKey(), entry.getValue());
                }
            }
            for (Map.Entry<String, DevServicesMongoResultBuildItem.Result> map : mongoRes.getNamedConnections().entrySet()) {
                for (Map.Entry<String, String> entry : map.getValue().getProperties().entrySet()) {
                    propertyConsumer.accept(entry.getKey(), entry.getValue());
                }
            }
        }

        DevServicesRedisResultBuildItem redisDevServices = buildResult.consumeOptional(DevServicesRedisResultBuildItem.class);
        if (redisDevServices != null) {
            if (redisDevServices.getDefaultConnection() != null) {
                Map<String, String> redisDefaultConfiguration = redisDevServices.getDefaultConnection().getProperties();
                for (Map.Entry<String, String> entry : redisDefaultConfiguration.entrySet()) {
                    propertyConsumer.accept(entry.getKey(), entry.getValue());
                }
            }
            for (Map.Entry<String, DevServicesRedisResultBuildItem.Result> redisNamedConfiguration : redisDevServices
                    .getNamedConnections().entrySet()) {
                for (Map.Entry<String, String> entry : redisNamedConfiguration.getValue().getProperties().entrySet()) {
                    propertyConsumer.accept(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
