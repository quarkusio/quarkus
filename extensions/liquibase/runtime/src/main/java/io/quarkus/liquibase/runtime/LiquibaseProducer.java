package io.quarkus.liquibase.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import io.agroal.api.AgroalDataSource;
import io.quarkus.liquibase.LiquibaseFactory;

/**
 * The liquibase producer
 */
@ApplicationScoped
public class LiquibaseProducer {

    /**
     * The error not ready message
     */
    private static final String ERROR_NOT_READY = "The Liquibase settings are not ready to be consumed: the %s configuration has not been injected yet";

    /**
     * The default data source
     */
    @Inject
    @Default
    Instance<AgroalDataSource> defaultDataSource;

    /**
     * The liquibase runtime configuration
     */
    @Inject
    LiquibaseRuntimeConfig liquibaseRuntimeConfig;

    /**
     * The liquibase build configuration
     */
    @Inject
    LiquibaseBuildTimeConfig liquibaseBuildConfig;

    /**
     * The default quarkus liquibase producer method
     * 
     * @return the quarkus liquibase instance for the default datasource
     */
    @Produces
    @Dependent
    @Default
    public LiquibaseFactory produceLiquibase() {
        return createDefaultLiquibase(defaultDataSource.get());
    }

    /**
     * Creates the quarkus liquibase for the datasource instance
     * 
     * @param dataSource the datasource instance
     * @return the corresponding quarkus liquibase instance
     */
    private LiquibaseFactory createDefaultLiquibase(AgroalDataSource dataSource) {
        return new LiquibaseCreator(getLiquibaseRuntimeConfig().defaultDataSource, getLiquibaseBuildConfig().defaultDataSource)
                .createLiquibase(dataSource);
    }

    /**
     * Creates the quarkus liquibase for the datasource instance and datasource name.
     * 
     * @param dataSource the datasource instance
     * @param dataSourceName the datasource name
     * @return the corresponding quarkus liquibase instance
     */
    public LiquibaseFactory createLiquibase(AgroalDataSource dataSource, String dataSourceName) {
        return new LiquibaseCreator(getLiquibaseRuntimeConfig().getConfigForDataSourceName(dataSourceName),
                getLiquibaseBuildConfig().getConfigForDataSourceName(dataSourceName))
                        .createLiquibase(dataSource);
    }

    /**
     * Gets the liquibase runtime configuration.
     * 
     * @return the liquibase runtime configuration.
     */
    private LiquibaseRuntimeConfig getLiquibaseRuntimeConfig() {
        return failIfNotReady(liquibaseRuntimeConfig, "runtime");
    }

    /**
     * Gets the liquibase build configuration.
     * 
     * @return the liquibase build configuration.
     */
    private LiquibaseBuildTimeConfig getLiquibaseBuildConfig() {
        return failIfNotReady(liquibaseBuildConfig, "build");
    }

    /**
     * Checks the configuration
     * 
     * @param config the configuration
     * @param name the name of the configuration
     * @param <T> the configuration type
     * @return the configuration
     * 
     * @exception IllegalStateException if the configuration is not ready
     */
    private static <T> T failIfNotReady(T config, String name) {
        if (config == null) {
            throw new IllegalStateException(String.format(ERROR_NOT_READY, name));
        }
        return config;
    }
}
