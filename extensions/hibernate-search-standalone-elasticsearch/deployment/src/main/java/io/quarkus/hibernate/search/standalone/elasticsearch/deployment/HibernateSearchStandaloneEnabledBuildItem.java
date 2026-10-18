package io.quarkus.hibernate.search.standalone.elasticsearch.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.HibernateSearchStandaloneElasticsearchMapperContext;

public final class HibernateSearchStandaloneEnabledBuildItem extends SimpleBuildItem {

    final HibernateSearchStandaloneElasticsearchMapperContext mapperContext;
    private final Set<String> rootAnnotationMappedClassNames;

    public HibernateSearchStandaloneEnabledBuildItem(HibernateSearchStandaloneElasticsearchMapperContext mapperContext,
            Set<String> rootAnnotationMappedClassNames) {
        this.mapperContext = mapperContext;
        this.rootAnnotationMappedClassNames = rootAnnotationMappedClassNames;
    }

    public Set<String> getRootAnnotationMappedClassNames() {
        return rootAnnotationMappedClassNames;
    }
}
