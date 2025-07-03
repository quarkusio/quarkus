package io.quarkus.devservices.common;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.datasource.common.runtime.DataSourceUtil;

public final class Labels {

    public static final String QUARKUS_DEV_SERVICE = "io.quarkus.devservice";
    public static final String DOCKER_COMPOSE_PROJECT = "com.docker.compose.project";
    public static final String DOCKER_COMPOSE_NETWORK = "com.docker.compose.network";
    public static final String DOCKER_COMPOSE_SERVICE = "com.docker.compose.service";
    public static final String DOCKER_COMPOSE_CONTAINER_NUMBER = "com.docker.compose.container-number";

    public static final String QUARKUS_COMPOSE_PREFIX = "io.quarkus.devservices.compose";

    public static final String COMPOSE_IGNORE = QUARKUS_COMPOSE_PREFIX + ".ignore";

    public static final String COMPOSE_CONFIG_MAP = QUARKUS_COMPOSE_PREFIX + ".config_map";
    public static final String COMPOSE_CONFIG_MAP_ENV_VAR = COMPOSE_CONFIG_MAP + ".env";
    public static final String COMPOSE_CONFIG_MAP_PORT = COMPOSE_CONFIG_MAP + ".port";

    public static final String COMPOSE_WAIT_FOR = QUARKUS_COMPOSE_PREFIX + ".wait_for";
    public static final String COMPOSE_WAIT_FOR_LOGS = COMPOSE_WAIT_FOR + ".logs";
    public static final String COMPOSE_WAIT_FOR_PORTS = COMPOSE_WAIT_FOR + ".ports";
    public static final String COMPOSE_WAIT_FOR_PORTS_DISABLE = COMPOSE_WAIT_FOR_PORTS + ".disable";
    public static final String COMPOSE_WAIT_FOR_PORTS_TIMEOUT = COMPOSE_WAIT_FOR_PORTS + ".timeout";

    public static final String COMPOSE_JDBC_PARAMETERS = QUARKUS_COMPOSE_PREFIX + ".jdbc.parameters";

    public static final String COMPOSE_EXPOSED_PORTS = QUARKUS_COMPOSE_PREFIX + ".exposed_ports";

    public static final String QUARKUS_LAUNCH_MODE = QUARKUS_DEV_SERVICE + ".launch-mode";

    /**
     * Label which indicates that this dev service was started by this process, and therefore should not be discovered for
     * re-use;
     * instead reuse should be managed by the DevServicesRegistry, so that config updates apply.
     * We use a UUID as the value so that we don't filter out dev services from other processes.
     */
    public static final String QUARKUS_PROCESS_UUID = QUARKUS_DEV_SERVICE + ".process-uuid";

    private static final String DATASOURCE = "datasource";

    public static void addDataSourceLabel(GenericContainer<?> container, String datasourceName) {
        container.withLabel(DATASOURCE, DataSourceUtil.isDefault(datasourceName) ? "default" : datasourceName);
    }

    private Labels() {
    }
}
