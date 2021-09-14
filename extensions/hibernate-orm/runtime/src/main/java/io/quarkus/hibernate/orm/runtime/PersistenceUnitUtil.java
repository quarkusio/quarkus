package io.quarkus.hibernate.orm.runtime;

import java.util.Locale;

import javax.enterprise.inject.Default;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.PersistenceUnitExtension;

public class PersistenceUnitUtil {
    private static final Logger LOG = Logger.getLogger(PersistenceUnitUtil.class);

    public static final String DEFAULT_PERSISTENCE_UNIT_NAME = "<default>";

    public static boolean isDefaultPersistenceUnit(String name) {
        return DEFAULT_PERSISTENCE_UNIT_NAME.equals(name);
    }

    public static <T> InjectableInstance<T> singleExtensionInstanceForPersistenceUnit(Class<T> beanType,
            String persistenceUnitName) {
        InjectableInstance<T> instance = extensionInstanceForPersistenceUnit(beanType, persistenceUnitName);
        if (instance.isAmbiguous()) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "Multiple instances of %1$s were found for persistence unit %2$s. "
                            + "At most one instance can be assigned to each persistence unit.",
                    beanType.getSimpleName(), persistenceUnitName));
        }
        return instance;
    }

    public static <T> InjectableInstance<T> extensionInstanceForPersistenceUnit(Class<T> beanType, String persistenceUnitName) {
        return Arc.container().select(beanType,
                new PersistenceUnitExtension.Literal(persistenceUnitName));
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
                            PersistenceUnit.class.getSimpleName());
                }
            }
        }
        return instance;
    }

    private static <T> boolean isDefaultBean(InjectableInstance<T> instance) {
        return instance.isResolvable() && instance.getHandle().getBean().isDefaultBean();
    }
}
