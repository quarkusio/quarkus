package io.quarkus.liquibase.runtime;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.inject.Default;
import javax.enterprise.util.AnnotationLiteral;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.liquibase.runtime.graal.LiquibaseServiceLoader;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import liquibase.exception.ServiceNotFoundException;
import liquibase.servicelocator.ServiceLocator;

/**
 * The liquibase recorder
 */
@Recorder
public class LiquibaseRecorder {

    private static final Logger log = Logger.getLogger(LiquibaseRecorder.class);

    public void setServicesImplementations(Map<String, List<String>> serviceLoader) {
        LiquibaseServiceLoader.setServicesImplementations(serviceLoader);
    }

    /**
     * Do start actions
     *
     * @param config the runtime configuration
     * @param container the bean container
     */
    public void doStartActions(LiquibaseRuntimeConfig config, BeanContainer container) {
        try {
            if (config.defaultDataSource.cleanAtStart) {
                dropAll(container, Default.Literal.INSTANCE);
            }
            if (config.defaultDataSource.migrateAtStart) {
                if (config.defaultDataSource.validateOnMigrate) {
                    validate(container, Default.Literal.INSTANCE);
                }
                migrate(container, Default.Literal.INSTANCE);
            }
            for (Entry<String, LiquibaseDataSourceRuntimeConfig> configPerDataSource : config.namedDataSources.entrySet()) {
                if (configPerDataSource.getValue().cleanAtStart) {
                    dropAll(container, LiquibaseDataSource.LiquibaseDataSourceLiteral.of(configPerDataSource.getKey()));
                }
                if (configPerDataSource.getValue().migrateAtStart) {
                    if (configPerDataSource.getValue().validateOnMigrate) {
                        validate(container, LiquibaseDataSource.LiquibaseDataSourceLiteral.of(configPerDataSource.getKey()));
                    }
                    migrate(container, LiquibaseDataSource.LiquibaseDataSourceLiteral.of(configPerDataSource.getKey()));
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Drop all database objects
     *
     * @param container the bean container
     * @param qualifier the bean qualifier
     * @throws LiquibaseException if the database actions fails
     */
    private void dropAll(BeanContainer container, AnnotationLiteral<? extends Annotation> qualifier) throws Exception {
        LiquibaseFactory liquibaseFactory = container.instance(LiquibaseFactory.class, qualifier);
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            liquibase.dropAll();
        }
    }

    /**
     * Migrate the database objects
     *
     * @param container the bean container
     * @param qualifier the bean qualifier
     * @throws LiquibaseException if the database actions fails
     */
    private void migrate(BeanContainer container, AnnotationLiteral<? extends Annotation> qualifier) throws Exception {
        LiquibaseFactory liquibaseFactory = container.instance(LiquibaseFactory.class, qualifier);
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            liquibase.update(liquibaseFactory.createContexts(), liquibaseFactory.createLabels());
        }
    }

    /**
     * Validate the database objects
     *
     * @param container the bean container
     * @param qualifier the bean qualifier
     * @throws LiquibaseException if the database actions fails
     */
    private void validate(BeanContainer container, AnnotationLiteral<? extends Annotation> qualifier) throws Exception {
        LiquibaseFactory liquibaseFactory = container.instance(LiquibaseFactory.class, qualifier);
        try (Liquibase liquibase = liquibaseFactory.createLiquibase()) {
            liquibase.validate();
        }
    }

    public void setJvmServiceImplementations(Map<String, List<String>> services) {
        ServiceLocator.setInstance(new ServiceLocator() {

            @Override
            public <T> Class<? extends T>[] findClasses(Class<T> requiredInterface) throws ServiceNotFoundException {
                List<String> found = services.get(requiredInterface.getName());
                if (found == null) {
                    log.warnf("Failed to find pre-indexed service %s, falling back to slow classpath scanning",
                            requiredInterface);
                    return super.findClasses(requiredInterface);
                }
                List<Class<? extends T>> ret = new ArrayList<>();
                for (String i : found) {
                    try {
                        ret.add((Class<? extends T>) Class.forName(i, false, Thread.currentThread().getContextClassLoader()));
                    } catch (ClassNotFoundException e) {
                        log.error("Failed to load Liquibase service", e);
                    }
                }
                return ret.toArray(new Class[0]);
            }
        });
    }
}
