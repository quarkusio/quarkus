package io.quarkus.devservices.db2.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_NAME;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_PASSWORD;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_USERNAME;

import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider.RunningDevServicesDatasource;
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.DatasourceServiceConfigurator;

public class DB2DatasourceServiceConfigurator implements DatasourceServiceConfigurator {

    private final static String[] USERNAME_ENVS = new String[] { "DB2INSTANCE" };

    private final static String[] PASSWORD_ENVS = new String[] { "DB2INST1_PASSWORD" };

    private final static String[] DATABASE_ENVS = new String[] { "DBNAME" };

    public RunningDevServicesDatasource composeRunningService(ContainerAddress containerAddress,
            DevServicesDatasourceContainerConfig containerConfig) {
        RunningContainer container = containerAddress.getRunningContainer();
        String effectiveDbName = containerConfig.getDbName().orElse(DEFAULT_DATABASE_NAME);
        String effectiveUsername = containerConfig.getDbName().orElse(DEFAULT_DATABASE_USERNAME);
        String effectivePassword = containerConfig.getDbName().orElse(DEFAULT_DATABASE_PASSWORD);
        String jdbcUrl = getJdbcUrl(containerAddress, container.tryGetEnv(DATABASE_ENVS).orElse(effectiveDbName));
        String reactiveUrl = getReactiveUrl(jdbcUrl);
        return new RunningDevServicesDatasource(
                containerAddress.getId(),
                jdbcUrl,
                reactiveUrl,
                container.tryGetEnv(USERNAME_ENVS).orElse(effectiveUsername),
                container.tryGetEnv(PASSWORD_ENVS).orElse(effectivePassword),
                null);
    }

    @Override
    public String getJdbcPrefix() {
        return "db2";
    }
}
