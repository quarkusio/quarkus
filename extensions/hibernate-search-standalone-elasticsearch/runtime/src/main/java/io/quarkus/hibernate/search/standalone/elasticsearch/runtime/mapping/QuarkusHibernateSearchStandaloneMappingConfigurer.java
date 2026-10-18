package io.quarkus.hibernate.search.standalone.elasticsearch.runtime.mapping;

import java.util.Set;

import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;

import io.quarkus.hibernate.search.standalone.elasticsearch.runtime.MappingStructure;

public class QuarkusHibernateSearchStandaloneMappingConfigurer implements StandalonePojoMappingConfigurer {
    private final MappingStructure structure;
    private final Set<Class<?>> rootAnnotationMappedClasses;

    public QuarkusHibernateSearchStandaloneMappingConfigurer(MappingStructure structure,
            Set<Class<?>> rootAnnotationMappedClasses) {
        this.structure = structure;
        this.rootAnnotationMappedClasses = rootAnnotationMappedClasses;
    }

    @Override
    public void configure(StandalonePojoMappingConfigurationContext context) {
        // Jandex is not available at runtime in Quarkus,
        // so Hibernate Search cannot perform classpath scanning on startup.
        context.annotationMapping()
                .discoverJandexIndexesFromAddedTypes(false)
                .buildMissingDiscoveredJandexIndexes(false);

        // ... but we do better: we perform classpath scanning during the build,
        // and propagate the result here.
        context.annotationMapping().add(rootAnnotationMappedClasses);

        context.defaultReindexOnUpdate(MappingStructure.DOCUMENT.equals(structure)
                ? ReindexOnUpdate.SHALLOW
                : ReindexOnUpdate.DEFAULT);
    }
}
