package io.quarkus.hibernate.search.orm.elasticsearch.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.environment.bean.BeanReference;

import io.quarkus.hibernate.search.backend.elasticsearch.common.runtime.MapperContext;
import io.quarkus.hibernate.search.orm.elasticsearch.runtime.bean.HibernateSearchBeanUtil;
import io.quarkus.runtime.annotations.RecordableConstructor;

public final class HibernateSearchOrmElasticsearchMapperContext implements MapperContext {

    public final String persistenceUnitName;
    private final Set<String> backendNamesForIndexedEntities;
    private final Map<String, Set<String>> backendAndIndexNamesForSearchExtensions;

    @RecordableConstructor
    public HibernateSearchOrmElasticsearchMapperContext(String persistenceUnitName,
            Set<String> backendNamesForIndexedEntities,
            Map<String, Set<String>> backendAndIndexNamesForSearchExtensions) {
        if (persistenceUnitName == null) {
            throw new IllegalArgumentException("persistenceUnitName cannot be null");
        }
        this.persistenceUnitName = persistenceUnitName;
        this.backendNamesForIndexedEntities = backendNamesForIndexedEntities;
        this.backendAndIndexNamesForSearchExtensions = backendAndIndexNamesForSearchExtensions;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + persistenceUnitName + "]";
    }

    @Override
    public Set<String> getBackendNamesForIndexedEntities() {
        return backendNamesForIndexedEntities;
    }

    @Override
    public Map<String, Set<String>> getBackendAndIndexNamesForSearchExtensions() {
        return backendAndIndexNamesForSearchExtensions;
    }

    @Override
    public String backendPropertyKey(String backendName, String indexName, String propertyKeyRadical) {
        return HibernateSearchElasticsearchRuntimeConfig.backendPropertyKey(persistenceUnitName, backendName, indexName,
                propertyKeyRadical);
    }

    @Override
    public <T> Optional<BeanReference<T>> singleExtensionBeanReferenceFor(Optional<String> override, Class<T> beanType,
            String backendName, String indexName) {
        return HibernateSearchBeanUtil.singleExtensionBeanReferenceFor(override, beanType, persistenceUnitName, backendName,
                indexName);
    }

    @Override
    public <T> Optional<List<BeanReference<T>>> multiExtensionBeanReferencesFor(Optional<List<String>> override,
            Class<T> beanType, String backendName, String indexName) {
        return HibernateSearchBeanUtil.multiExtensionBeanReferencesFor(override, beanType, persistenceUnitName, backendName,
                indexName);
    }
}
