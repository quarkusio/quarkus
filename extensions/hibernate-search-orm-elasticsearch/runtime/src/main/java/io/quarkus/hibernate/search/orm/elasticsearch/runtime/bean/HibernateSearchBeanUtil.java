package io.quarkus.hibernate.search.orm.elasticsearch.runtime.bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.engine.environment.bean.BeanReference;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.search.orm.elasticsearch.SearchExtension;

public final class HibernateSearchBeanUtil {

    private HibernateSearchBeanUtil() {
    }

    public static <T> Optional<BeanReference<T>> singleExtensionBeanReferenceFor(Optional<String> override, Class<T> beanType,
            String persistenceUnitName, String backendName, String indexName) {
        return override.map(string -> BeanReference.parse(beanType, string))
                .or(() -> singleExtensionBeanReferenceFor(beanType, persistenceUnitName, backendName, indexName));
    }

    private static <T> Optional<BeanReference<T>> singleExtensionBeanReferenceFor(Class<T> beanType,
            String persistenceUnitName, String backendName, String indexName) {
        InjectableInstance<T> instance = extensionInstanceFor(beanType, persistenceUnitName, backendName, indexName);
        if (instance.isAmbiguous()) {
            if (indexName != null) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "Multiple instances of %1$s were found for Hibernate Search index %3$s in persistence unit %2$s."
                                + " At most one instance can be assigned to each index.",
                        beanType.getSimpleName(), persistenceUnitName, indexName));
            } else if (backendName != null) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "Multiple instances of %1$s were found for Hibernate Search backend %3$s in persistence unit %2$s."
                                + " At most one instance can be assigned to each backend.",
                        beanType.getSimpleName(), persistenceUnitName, backendName));
            } else {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "Multiple instances of %1$s were found for Hibernate Search in persistence unit %2$s."
                                + " At most one instance can be assigned to each persistence unit.",
                        beanType.getSimpleName(), persistenceUnitName));
            }
        }
        return instance.isResolvable() ? Optional.of(new ArcBeanReference<>(instance.getHandle().getBean())) : Optional.empty();
    }

    public static <T> Optional<List<BeanReference<T>>> multiExtensionBeanReferencesFor(Optional<List<String>> override,
            Class<T> beanType,
            String persistenceUnitName, String backendName, String indexName) {
        return override.map(strings -> strings.stream()
                .map(string -> BeanReference.parse(beanType, string))
                .collect(Collectors.toList()))
                .or(() -> multiExtensionBeanReferencesFor(beanType, persistenceUnitName, backendName, indexName));
    }

    private static <T> Optional<List<BeanReference<T>>> multiExtensionBeanReferencesFor(Class<T> beanType,
            String persistenceUnitName, String backendName, String indexName) {
        InjectableInstance<T> instance = extensionInstanceFor(beanType, persistenceUnitName, backendName, indexName);
        if (!instance.isResolvable()) {
            return Optional.empty();
        }
        List<BeanReference<T>> references = new ArrayList<>();
        for (InstanceHandle<T> handle : instance.handles()) {
            references.add(new ArcBeanReference<>(handle.getBean()));
        }
        return Optional.of(references);
    }

    public static <T> InjectableInstance<T> extensionInstanceFor(Class<T> beanType, String persistenceUnitName,
            String backendName, String indexName) {
        return Arc.container().select(beanType,
                new SearchExtension.Literal(persistenceUnitName, backendName == null ? "" : backendName,
                        indexName == null ? "" : indexName));
    }

}
