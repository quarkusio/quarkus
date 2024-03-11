package io.quarkus.hibernate.search.backend.elasticsearch.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.environment.bean.BeanReference;

public interface MapperContext {

    String toString();

    Set<String> backendNamesForIndexedEntities();

    Map<String, Set<String>> backendAndIndexNamesForSearchExtensions();

    String backendPropertyKey(String backendName, String indexName, String propertyKeyRadical);

    <T> Optional<BeanReference<T>> singleExtensionBeanReferenceFor(Optional<String> override, Class<T> beanType,
            String backendName, String indexName);

    <T> Optional<List<BeanReference<T>>> multiExtensionBeanReferencesFor(Optional<List<String>> override,
            Class<T> beanType,
            String backendName, String indexName);
}
