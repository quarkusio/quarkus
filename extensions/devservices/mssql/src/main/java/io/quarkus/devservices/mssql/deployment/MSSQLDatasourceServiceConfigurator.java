package io.quarkus.devservices.mssql.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_PASSWORD;

import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider.RunningDevServicesDatasource;
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.DatasourceServiceConfigurator;

public class MSSQLDatasourceServiceConfigurator implements DatasourceServiceConfigurator {

    private final static String[] PASSWORD_ENVS = new String[] { "MSSQL_SA_PASSWORD", "SA_PASSWORD", "MSSQL_PASSWORD" };

    private final static String DEFAULT_MSSQL_USERNAME = "sa";

    public RunningDevServicesDatasource composeRunningService(ContainerAddress containerAddress,
            DevServicesDatasourceContainerConfig containerConfig) {
        RunningContainer container = containerAddress.getRunningContainer();
        String effectiveUsername = containerConfig.getUsername().orElse(DEFAULT_MSSQL_USERNAME);
        String effectivePassword = containerConfig.getPassword().orElse(DEFAULT_DATABASE_PASSWORD);
        String jdbcUrl = getJdbcUrl(containerAddress, null);
        String reactiveUrl = getReactiveUrl(jdbcUrl);
        return new RunningDevServicesDatasource(
                containerAddress.getId(),
                jdbcUrl,
                reactiveUrl,
                effectiveUsername,
                container.tryGetEnv(PASSWORD_ENVS).orElse(effectivePassword),
                null);
    }

    @Override
    public String getJdbcPrefix() {
        return "sqlserver";
    }

    @Override
    public String getParametersStartCharacter() {
        return ";";
    }

    @Override
    public String getJdbcUrl(ContainerAddress containerAddress, String databaseName) {
        return String.format("jdbc:%s://%s:%d%s",
                getJdbcPrefix(),
                containerAddress.getHost(),
                containerAddress.getPort(),
                getParameters(containerAddress.getRunningContainer().containerInfo().labels()));
    }

}
