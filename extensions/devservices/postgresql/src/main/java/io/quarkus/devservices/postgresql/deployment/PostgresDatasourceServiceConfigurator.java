package io.quarkus.devservices.postgresql.deployment;

import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_NAME;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_PASSWORD;
import static io.quarkus.datasource.deployment.spi.DatabaseDefaultSetupConfig.DEFAULT_DATABASE_USERNAME;

import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider.RunningDevServicesDatasource;
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.DatasourceServiceConfigurator;

public class PostgresDatasourceServiceConfigurator implements DatasourceServiceConfigurator {

    private final static String[] USERNAME_ENVS = new String[] { "POSTGRES_USER", "POSTGRESQL_USER", "POSTGRESQL_USERNAME" };

    private final static String[] PASSWORD_ENVS = new String[] { "POSTGRES_PASSWORD", "POSTGRESQL_PASSWORD" };

    private final static String[] DATABASE_ENVS = new String[] { "POSTGRES_DB", "POSTGRESQL_DB", "POSTGRESQL_DATABASE" };

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
                extractPassword(container, effectivePassword),
                null);
    }

    private String extractPassword(RunningContainer container, String effectivePassword) {
        if ("trust".equals(container.env().get("POSTGRES_HOST_AUTH_METHOD"))) {
            return null;
        }
        if (container.env().containsKey("ALLOW_EMPTY_PASSWORD")) {
            return "";
        }
        return container.tryGetEnv(PASSWORD_ENVS).orElse(effectivePassword);
    }

    @Override
    public String getJdbcPrefix() {
        return "postgresql";
    }
}
