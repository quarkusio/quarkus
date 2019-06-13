package io.quarkus.creator.config;

import io.quarkus.creator.config.reader.PropertiesHandler;

/**
 *
 * @author Alexey Loubyansky
 */
public interface Configurable<T> {

    String getConfigPropertyName();

    PropertiesHandler<T> getPropertiesHandler();
}
