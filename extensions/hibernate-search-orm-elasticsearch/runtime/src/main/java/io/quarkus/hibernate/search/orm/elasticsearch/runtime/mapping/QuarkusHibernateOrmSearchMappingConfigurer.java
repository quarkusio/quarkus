package io.quarkus.hibernate.search.orm.elasticsearch.runtime.mapping;

import java.util.Set;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;

public class QuarkusHibernateOrmSearchMappingConfigurer implements HibernateOrmSearchMappingConfigurer {
    private final Set<Class<?>> rootAnnotationMappedClasses;

    public QuarkusHibernateOrmSearchMappingConfigurer(Set<Class<?>> rootAnnotationMappedClasses) {
        this.rootAnnotationMappedClasses = rootAnnotationMappedClasses;
    }

    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        // Jandex is not available at runtime in Quarkus,
        // so Hibernate Search cannot perform classpath scanning on startup.
        context.annotationMapping().discoverJandexIndexesFromAddedTypes(false)
                .buildMissingDiscoveredJandexIndexes(false);

        // ... but we do better: we perform classpath scanning during the build,
        // and propagate the result here.
        context.annotationMapping().add(rootAnnotationMappedClasses);
    }
}
