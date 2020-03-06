package io.quarkus.liquibase.runtime;

import java.lang.annotation.Annotation;
import java.util.Map.Entry;

import javax.enterprise.inject.Default;
import javax.enterprise.util.AnnotationLiteral;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.liquibase.LiquibaseDataSource;
import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.runtime.annotations.Recorder;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;

/**
 * The liquibase recorder
 */
@Recorder
public class LiquibaseRecorder {

    /**
     * Sets the liquibase build configuration
     * 
     * @param liquibaseBuildConfig the liquibase build time configuration
     * @return the bean container listener
     */
    public BeanContainerListener setLiquibaseBuildConfig(LiquibaseBuildTimeConfig liquibaseBuildConfig) {
        return beanContainer -> {
            LiquibaseProducer producer = beanContainer.instance(LiquibaseProducer.class);
            producer.setLiquibaseBuildConfig(liquibaseBuildConfig);
        };
    }

    /**
     * Configure the liquibase runtime properties
     * 
     * @param liquibaseRuntimeConfig the liquibase runtime configuration
     * @param container the bean container
     */
    public void configureLiquibaseProperties(LiquibaseRuntimeConfig liquibaseRuntimeConfig, BeanContainer container) {
        container.instance(LiquibaseProducer.class).setLiquibaseRuntimeConfig(liquibaseRuntimeConfig);
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
}
