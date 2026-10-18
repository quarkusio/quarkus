package io.quarkus.hibernate.search.backend.elasticsearch.common.deployment;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.HibernateSearchBackendElasticsearchBuildTimeConfig;
import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.MapperContext;

public final class HibernateSearchBackendElasticsearchEnabledBuildItem extends MultiBuildItem {

    private final MapperContext mapperContext;
    private final Map<String, HibernateSearchBackendElasticsearchBuildTimeConfig> buildTimeConfig;

    public HibernateSearchBackendElasticsearchEnabledBuildItem(MapperContext mapperContext,
            Map<String, HibernateSearchBackendElasticsearchBuildTimeConfig> buildTimeConfig) {
        this.mapperContext = mapperContext;
        this.buildTimeConfig = buildTimeConfig;
    }

    public MapperContext getMapperContext() {
        return mapperContext;
    }

    public Map<String, HibernateSearchBackendElasticsearchBuildTimeConfig> getBuildTimeConfig() {
        return buildTimeConfig;
    }

}
