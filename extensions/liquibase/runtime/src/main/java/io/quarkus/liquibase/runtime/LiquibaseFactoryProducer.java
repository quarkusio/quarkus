package io.quarkus.liquibase.runtime;

import javax.sql.DataSource;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.liquibase.LiquibaseFactory;

/**
 * This class is sort of a producer for {@link LiquibaseFactory}.
 *
 * It isn't a CDI producer in the literal sense, but it is marked as a bean
 * and it's {@code createLiquibaseFactory} method is called at runtime in order to produce
 * the actual {@code LiquibaseFactory} objects.
 *
 * CDI scopes and qualifiers are setup at build-time, which is why this class is devoid of
 * any CDI annotations
 *
 */
public class LiquibaseFactoryProducer {

    private final LiquibaseBuildTimeConfig liquibaseBuildTimeConfig;
    private final LiquibaseRuntimeConfig liquibaseRuntimeConfig;

    public LiquibaseFactoryProducer(LiquibaseBuildTimeConfig liquibaseBuildTimeConfig,
            LiquibaseRuntimeConfig liquibaseRuntimeConfig) {
        this.liquibaseBuildTimeConfig = liquibaseBuildTimeConfig;
        this.liquibaseRuntimeConfig = liquibaseRuntimeConfig;
    }

    public LiquibaseFactory createLiquibaseFactory(DataSource dataSource, String dataSourceName) {
        LiquibaseDataSourceBuildTimeConfig matchingBuildTimeConfig = DataSourceUtil.isDefault(dataSourceName)
                ? liquibaseBuildTimeConfig.defaultDataSource
                : liquibaseBuildTimeConfig.getConfigForDataSourceName(dataSourceName);
        LiquibaseDataSourceRuntimeConfig matchingRuntimeConfig = DataSourceUtil.isDefault(dataSourceName)
                ? liquibaseRuntimeConfig.defaultDataSource
                : liquibaseRuntimeConfig.getConfigForDataSourceName(dataSourceName);
        return new LiquibaseCreator(matchingRuntimeConfig, matchingBuildTimeConfig)
                .createLiquibaseFactory(dataSource, dataSourceName);
    }
}
