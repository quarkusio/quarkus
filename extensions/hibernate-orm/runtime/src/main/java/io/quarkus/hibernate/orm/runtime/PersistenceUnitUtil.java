package io.quarkus.hibernate.orm.runtime;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import jakarta.enterprise.inject.Default;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;
import io.quarkus.runtime.configuration.ConfigurationException;

public class PersistenceUnitUtil {
    private static final Logger LOG = Logger.getLogger(PersistenceUnitUtil.class);

    public static final String DEFAULT_PERSISTENCE_UNIT_NAME = "<default>";

    public static boolean isDefaultPersistenceUnit(String name) {
        return DEFAULT_PERSISTENCE_UNIT_NAME.equals(name);
    }

    public static <T> InjectableInstance<T> singleExtensionInstanceForPersistenceUnit(Class<T> beanType,
            String persistenceUnitName,
            Annotation... additionalQualifiers) {
        InjectableInstance<T> instance = extensionInstanceForPersistenceUnit(beanType, persistenceUnitName,
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

    public static <T> InjectableInstance<T> extensionInstanceForPersistenceUnit(Class<T> beanType, String persistenceUnitName,
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

    @Deprecated
    public static <T> InjectableInstance<T> legacySingleExtensionInstanceForPersistenceUnit(Class<T> beanType,
            String persistenceUnitName) {
        InjectableInstance<T> instance = singleExtensionInstanceForPersistenceUnit(
                beanType, persistenceUnitName);
        if (instance.isUnsatisfied()) {
            // Legacy behavior
            if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
                instance = Arc.container().select(beanType, Default.Literal.INSTANCE);
                if (!instance.isUnsatisfied()
                        // As long as this legacy support exists, the default,
                        // Quarkus-defined beans need to use this legacy support,
                        // in order to not override user beans.
                        // So let's not log a warning when we just default to the Quarkus-defined beans...
                        && !isDefaultBean(instance)) {
                    LOG.warnf("A bean of type %1$s is being retrieved even though it doesn't have a @%2$s qualifier."
                            + " This is deprecated usage and will not work in future versions of Quarkus."
                            + " Annotate this bean with @%2$s to make it future-proof.",
                            beanType.getName(), PersistenceUnitExtension.class.getSimpleName());
                }
            } else {
                instance = Arc.container().select(beanType,
                        new PersistenceUnit.PersistenceUnitLiteral(persistenceUnitName));
                if (!instance.isUnsatisfied()
                        // As long as this legacy support exists, the default,
                        // Quarkus-defined beans need to use this legacy support,
                        // in order to not override user beans.
                        // So let's not log a warning when we just default to the Quarkus-defined beans...
                        && !isDefaultBean(instance)) {
                    LOG.warnf("A bean of type %1$s is being retrieved even though it doesn't have a @%2$s qualifier."
                            + " This is deprecated usage and will not work in future versions of Quarkus."
                            + " Annotate this bean with @%2$s(\"%4$s\") instead of @%3$s(\"%4$s\") to make it future-proof.",
                            beanType.getName(), PersistenceUnitExtension.class.getSimpleName(),
                            PersistenceUnit.class.getSimpleName(), persistenceUnitName);
                }
            }
        }
        return instance;
    }

    private static <T> boolean isDefaultBean(InjectableInstance<T> instance) {
        return instance.isResolvable() && instance.getHandle().getBean().isDefaultBean();
    }

    public static ConfigurationException unableToFindDataSource(String persistenceUnitName,
            String dataSourceName,
            Throwable cause) {
        return new ConfigurationException(String.format(Locale.ROOT,
                "Unable to find datasource '%s' for persistence unit '%s': %s",
                dataSourceName, persistenceUnitName, cause.getMessage()),
                cause);
    }
}
