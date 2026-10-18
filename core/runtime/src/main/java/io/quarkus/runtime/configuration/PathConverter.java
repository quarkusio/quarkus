package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter for a {@link Path} interface.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class PathConverter implements Converter<Path>, Serializable {

    private static final long serialVersionUID = 4452863383998867844L;

    @Override
    public Path convert(String value) {
        return value.isEmpty() ? null : Paths.get(value);
    }
}
