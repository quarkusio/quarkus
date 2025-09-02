package io.quarkus.devservices.common;

import java.util.Map;

import io.quarkus.runtime.util.StringUtil;

public interface DatasourceServiceConfigurator {

    default String getReactiveUrl(String jdbcUrl) {
        return jdbcUrl.replaceFirst("jdbc:", "vertx-reactive:");
    }

    default String getJdbcUrl(ContainerAddress containerAddress, String databaseName) {
        return String.format("jdbc:%s://%s:%d/%s%s",
                getJdbcPrefix(),
                containerAddress.getHost(),
                containerAddress.getPort(),
                databaseName,
                getParameters(containerAddress.getRunningContainer().containerInfo().labels()));
    }

    String getJdbcPrefix();

    default String getParametersStartCharacter() {
        return "?";
    }

    default String getParameters(Map<String, String> labels) {
        String parameters = labels.get(Labels.COMPOSE_JDBC_PARAMETERS);
        return StringUtil.isNullOrEmpty(parameters) ? "" : getParametersStartCharacter() + parameters;
    }

}
