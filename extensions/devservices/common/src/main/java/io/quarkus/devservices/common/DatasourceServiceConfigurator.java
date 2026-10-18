package io.quarkus.devservices.common;

import java.util.Map;

import io.quarkus.runtime.util.StringUtil;

public interface DatasourceServiceConfigurator {

    default String getReactiveUrl(String jdbcUrl) {
        return jdbcUrl.replaceFirst("jdbc:", "vertx-reactive:");
    }

    /**
     * Uses {@link DevServicesHostUtil#formatHostAndPort} for IPv6-safe authorities.
     * Implementors overriding this method must bracket IPv6 hosts themselves.
     */
    default String getJdbcUrl(ContainerAddress containerAddress, String databaseName) {
        return String.format("jdbc:%s://%s/%s%s",
                getJdbcPrefix(),
                DevServicesHostUtil.formatResolvedHostAndPort(containerAddress.getId(), containerAddress.getHost(),
                        containerAddress.getPort()),
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
