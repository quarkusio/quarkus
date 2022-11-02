package io.quarkus.devservices.common;

import java.util.Optional;

import org.testcontainers.containers.GenericContainer;

public final class Labels {

    private static final String DATASOURCE = "datasource";

    public static void addDataSourceLabel(GenericContainer<?> container, Optional<String> datasourceName) {
        container.withLabel(DATASOURCE, datasourceName.orElse("default"));
    }

    private Labels() {
    }
}
