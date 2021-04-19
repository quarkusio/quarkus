package io.quarkus.mongodb.deployment;

import java.util.Collections;
import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DevServicesMongoClientResultBuildItem extends SimpleBuildItem {

    final Result defaultDatasource;
    final Map<String, Result> namedDatasources;

    public DevServicesMongoClientResultBuildItem(Result defaultDatasource, Map<String, Result> namedDatasources) {
        this.defaultDatasource = defaultDatasource;
        this.namedDatasources = Collections.unmodifiableMap(namedDatasources);
    }

    public Result getDefaultDatasource() {
        return defaultDatasource;
    }

    public Map<String, Result> getNamedDatasources() {
        return namedDatasources;
    }

    public static class Result {
        final Map<String, String> configProperties;

        public Result(Map<String, String> configProperties) {
            this.configProperties = Collections.unmodifiableMap(configProperties);
        }

        public Map<String, String> getConfigProperties() {
            return configProperties;
        }
    }
}
