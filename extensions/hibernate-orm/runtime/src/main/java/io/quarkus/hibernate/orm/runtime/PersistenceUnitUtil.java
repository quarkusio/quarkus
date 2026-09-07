package io.quarkus.hibernate.orm.runtime;

import static io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig.puPropertyKey;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Default;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.runtime.configuration.ConfigurationException;

public class PersistenceUnitUtil {
    private static final Logger LOG = Logger.getLogger(PersistenceUnitUtil.class);

    public static final String DEFAULT_PERSISTENCE_UNIT_NAME = "<default>";

    public static boolean isDefaultPersistenceUnit(String name) {
        return DEFAULT_PERSISTENCE_UNIT_NAME.equals(name);
    }

    public static Annotation qualifier(String persistenceUnitName) {
        if (isDefaultPersistenceUnit(persistenceUnitName)) {
            return Default.Literal.INSTANCE;
        } else {
            return new PersistenceUnit.PersistenceUnitLiteral(persistenceUnitName);
        }
    }

    public static <T> InjectableInstance<T> singleExtensionInstanceForPersistenceUnit(
            Class<T> beanType,
            String persistenceUnitName,
            Annotation... additionalQualifiers) {
        InjectableInstance<T> instance = extensionInstancesForPersistenceUnit(beanType, persistenceUnitName,
                additionalQualifiers);
        if (instance.isAmbiguous()) {
            List<String> ambiguousClassNames = instance.handlesStream().map(h -> h.getBean().getBeanClass().getCanonicalName())
                    .toList();
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "Multiple instances of %1$s were found for persistence unit %2$s. "
                            + "At most one instance can be assigned to each persistence unit. Instances found: %3$s",
                    beanType.getSimpleName(), persistenceUnitName, ambiguousClassNames));
        }
        return instance;
    }

    public static <T> InjectableInstance<T> extensionInstancesForPersistenceUnit(
            Class<T> beanType,
            String persistenceUnitName,
            Annotation... additionalQualifiers) {
        if (additionalQualifiers.length == 0) {
            return Arc.container().select(beanType, new PersistenceUnitExtension.Literal(persistenceUnitName));
        } else {
            Annotation[] qualifiers = Arrays.copyOf(additionalQualifiers, additionalQualifiers.length + 1);
            qualifiers[additionalQualifiers.length] = new PersistenceUnitExtension.Literal(persistenceUnitName);
            return Arc.container().select(beanType, qualifiers);
        }
    }

    public static class PersistenceUnitNameComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            if (DEFAULT_PERSISTENCE_UNIT_NAME.equals(o1)) {
                return -1;
            } else if (DEFAULT_PERSISTENCE_UNIT_NAME.equals(o2)) {
                return +1;
            } else {
                return o1.compareTo(o2);
            }
        }
    }

    /**
     * Retrieves the single {@link TenantResolver} bean assigned to the given persistence unit, if any.
     * <p>
     * {@link TenantResolver} is a generic interface, so implementations expose a parameterized bean type
     * (e.g. {@code TenantResolver<Long>}) that Arc typesafe resolution does not match against a raw
     * {@code TenantResolver.class} required type. We therefore look the bean up by its concrete implementation class,
     * using the set of implementation classes collected at build time (see {@link MultiTenancyResolverClasses}).
     */
    @SuppressWarnings("unchecked")
    public static Optional<InstanceHandle<TenantResolver<Object>>> singleTenantResolver(
            Set<Class<?>> tenantResolverClasses, String persistenceUnitName) {
        return (Optional<InstanceHandle<TenantResolver<Object>>>) (Optional<?>) singleGenericExtensionInstanceForPersistenceUnit(
                tenantResolverClasses, TenantResolver.class, persistenceUnitName);
    }

    /**
     * Retrieves the single {@link TenantConnectionResolver} bean assigned to the given persistence unit, if any.
     *
     * @see #singleTenantResolver(Set, String)
     */
    @SuppressWarnings("unchecked")
    public static Optional<InstanceHandle<TenantConnectionResolver<Object>>> singleTenantConnectionResolver(
            Set<Class<?>> tenantConnectionResolverClasses, String persistenceUnitName) {
        return (Optional<InstanceHandle<TenantConnectionResolver<Object>>>) (Optional<?>) singleGenericExtensionInstanceForPersistenceUnit(
                tenantConnectionResolverClasses, TenantConnectionResolver.class, persistenceUnitName);
    }

    /**
     * Resolves the single bean of a generic {@code @PersistenceUnitExtension} interface for a persistence unit.
     * <p>
     * Because the interface is generic, beans are selected by their concrete implementation class rather than by the
     * interface type. The interface class itself is included in the lookup so that raw-typed beans (such as the
     * Quarkus-provided {@code @DefaultBean}, whose only exposed type is the raw interface) are still found.
     * <p>
     * The {@code @PersistenceUnitExtension} qualifier is tried first; if nothing matches, the deprecated fallback to the
     * {@code @Default} / {@code @PersistenceUnit} qualifiers is applied, matching the behavior of the non-generic
     * extension lookups.
     */
    private static Optional<InstanceHandle<Object>> singleGenericExtensionInstanceForPersistenceUnit(
            Set<Class<?>> beanClasses, Class<?> interfaceType, String persistenceUnitName) {
        Set<Class<?>> lookupClasses = new LinkedHashSet<>();
        lookupClasses.add(interfaceType);
        lookupClasses.addAll(beanClasses);

        List<InstanceHandle<Object>> handles = uniqueBeanHandles(lookupClasses,
                new PersistenceUnitExtension.Literal(persistenceUnitName));
        boolean usedLegacyQualifier = false;
        if (handles.isEmpty()) {
            // Legacy behavior: fall back to the @Default / @PersistenceUnit qualifiers.
            Annotation legacyQualifier = isDefaultPersistenceUnit(persistenceUnitName)
                    ? Default.Literal.INSTANCE
                    : new PersistenceUnit.PersistenceUnitLiteral(persistenceUnitName);
            handles = uniqueBeanHandles(lookupClasses, legacyQualifier);
            usedLegacyQualifier = true;
        }

        if (handles.isEmpty()) {
            return Optional.empty();
        }
        // Replicate the @DefaultBean semantics that Arc typesafe resolution would normally apply:
        // because we select beans by their concrete implementation class, a Quarkus-provided @DefaultBean
        // (e.g. DataSourceTenantConnectionResolver) can be resolved through its own class even when a
        // user-provided bean also matches through the interface type. Arc would suppress the default bean in
        // that situation, so we do the same here: as soon as a non-default bean is present, drop the default one(s).
        handles = suppressDefaultBeans(handles);
        if (handles.size() > 1) {
            List<String> ambiguousClassNames = handles.stream()
                    .map(h -> h.getBean().getBeanClass().getCanonicalName()).toList();
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "Multiple instances of %1$s were found for persistence unit %2$s. "
                            + "At most one instance can be assigned to each persistence unit. Instances found: %3$s",
                    interfaceType.getSimpleName(), persistenceUnitName, ambiguousClassNames));
        }

        InstanceHandle<Object> handle = handles.get(0);
        // As long as this legacy support exists, the default, Quarkus-defined beans need to use this legacy support,
        // in order to not override user beans. So let's not log a warning when we just default to the Quarkus-defined beans...
        if (usedLegacyQualifier && !handle.getBean().isDefaultBean()) {
            if (isDefaultPersistenceUnit(persistenceUnitName)) {
                LOG.warnf("A bean of type %1$s is being retrieved even though it doesn't have a @%2$s qualifier."
                        + " This is deprecated usage and will not work in future versions of Quarkus."
                        + " Annotate this bean with @%2$s to make it future-proof.",
                        interfaceType.getName(), PersistenceUnitExtension.class.getSimpleName());
            } else {
                LOG.warnf("A bean of type %1$s is being retrieved even though it doesn't have a @%2$s qualifier."
                        + " This is deprecated usage and will not work in future versions of Quarkus."
                        + " Annotate this bean with @%2$s(\"%4$s\") instead of @%3$s(\"%4$s\") to make it future-proof.",
                        interfaceType.getName(), PersistenceUnitExtension.class.getSimpleName(),
                        PersistenceUnit.class.getSimpleName(), persistenceUnitName);
            }
        }
        return Optional.of(handle);
    }

    /**
     * Retrieves the available bean handles for the given classes and qualifiers, deduplicated by bean.
     * <p>
     * A single bean can be reachable through multiple of the given classes; to avoid returning it more than once we key
     * the handles by bean identifier. Note that collecting instances in an identity-based set would not work, because
     * dependent-scoped beans would yield a distinct instance per lookup.
     */
    private static List<InstanceHandle<Object>> uniqueBeanHandles(Set<Class<?>> beanClasses, Annotation... qualifiers) {
        Map<String, InstanceHandle<Object>> handlesByBeanId = new LinkedHashMap<>();
        for (Class<?> beanClass : beanClasses) {
            @SuppressWarnings("unchecked")
            Class<Object> castBeanClass = (Class<Object>) beanClass;
            InjectableInstance<Object> instance = Arc.container().select(castBeanClass, qualifiers);
            for (InstanceHandle<Object> handle : instance.handles()) {
                if (handle.isAvailable()) {
                    handlesByBeanId.putIfAbsent(handle.getBean().getIdentifier(), handle);
                }
            }
        }
        return new ArrayList<>(handlesByBeanId.values());
    }

    /**
     * Drops {@code @DefaultBean} handles as soon as at least one non-default bean is present, mirroring the resolution
     * that Arc performs for typesafe lookups. When only default beans are available, the list is returned unchanged.
     */
    private static List<InstanceHandle<Object>> suppressDefaultBeans(List<InstanceHandle<Object>> handles) {
        boolean hasNonDefaultBean = handles.stream().anyMatch(handle -> !handle.getBean().isDefaultBean());
        if (!hasNonDefaultBean) {
            return handles;
        }
        return handles.stream().filter(handle -> !handle.getBean().isDefaultBean()).collect(Collectors.toList());
    }

    public static ConfigurationException unableToFindDataSource(String persistenceUnitName,
            String dataSourceName,
            Throwable cause) {
        return new ConfigurationException(String.format(Locale.ROOT,
                "Unable to find datasource '%s' for persistence unit '%s': %s",
                dataSourceName, persistenceUnitName, cause.getMessage()),
                cause);
    }

    public static String persistenceUnitInactiveReasonDeactivated(String puName,
            Optional<String> datasourceName) {
        return String.format(Locale.ROOT,
                "Persistence unit '%s' was deactivated through configuration properties."
                        + " To activate the persistence unit, set configuration property '%s' to 'true'"
                        + (datasourceName.isPresent()
                                ? String.format(Locale.ROOT, " and configure datasource '%s'."
                                        + " Refer to https://quarkus.io/guides/datasource for guidance.",
                                        datasourceName.get())
                                : "."),
                puName, puPropertyKey(puName, "active"));
    }
}
