package io.quarkus.elasticsearch.restclient.common.deployment;

import java.util.Collections;
import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;

public final class DevServicesElasticsearchResultBuildItem extends SimpleBuildItem {

    final Map<String, ElasticsearchInstance> devservices;

    public DevServicesElasticsearchResultBuildItem(Map<String, ElasticsearchInstance> devservices) {
        this.devservices = Collections.unmodifiableMap(devservices);
    }

    public ElasticsearchInstance getDefaultElasticsearchInstance() {
        return devservices.get(DataSourceUtil.DEFAULT_DATASOURCE_NAME);
    }

    public static class ElasticsearchInstance {
        final String distribution;
        final Map<String, String> configProperties;

        public ElasticsearchInstance(String distribution, Map<String, String> configProperties) {
            this.distribution = distribution;
            this.configProperties = Collections.unmodifiableMap(configProperties);
        }

        public String getDistribution() {
            return distribution;
        }

        public Map<String, String> getConfigProperties() {
            return configProperties;
        }
    }
}
