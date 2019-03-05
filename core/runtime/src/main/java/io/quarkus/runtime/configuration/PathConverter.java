package io.quarkus.runtime.configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.microprofile.config.spi.Converter;

public class PathConverter implements Converter<Path> {
    @Override
    public Path convert(String value) {
        return Paths.get(value);
    }
}
