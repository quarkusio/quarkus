package io.quarkus.info.runtime.spi;

import java.util.Map;

/**
 * SPI used by extensions to contribute properties to the info endpoint.
 * This is meant to be called when the application is started and provide properties that do not change.
 */
public interface InfoContributor {

    /**
     * The top level property under which the {@code data} will be added
     */
    String name();

    /**
     * Properties to add under {@code name}
     */
    Map<String, Object> data();
}
