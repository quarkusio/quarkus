package io.quarkus.hibernate.search.orm.elasticsearch.runtime.mapping;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;

public class QuarkusHibernateOrmSearchMappingConfigurer implements HibernateOrmSearchMappingConfigurer {
    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        // Jandex is not available at runtime in Quarkus,
        // so Hibernate Search cannot perform classpath scanning on startup.
        context.annotationMapping()
                .discoverJandexIndexesFromAddedTypes(false)
                .buildMissingDiscoveredJandexIndexes(false);
    }
}
