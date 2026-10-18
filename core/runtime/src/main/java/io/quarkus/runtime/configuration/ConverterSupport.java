package io.quarkus.runtime.configuration;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * This small utility class holds constants relevant to configuration converters.
 */
public class ConverterSupport {

    /**
     * Default {@link Converter} priority with value {@value #DEFAULT_SMALLRYE_CONVERTER_PRIORITY}
     * to be used for all discovered converters in case when no {@link Priority} annotation is
     * available on the converter class.
     */
    public static final int DEFAULT_SMALLRYE_CONVERTER_PRIORITY = 100;

    /**
     * Default {@link Converter} priority with value {@value #DEFAULT_QUARKUS_CONVERTER_PRIORITY} to
     * be used for all Quarkus converters. The reason why Quarkus priority is higher than a default
     * one, which is {@value #DEFAULT_SMALLRYE_CONVERTER_PRIORITY}, is because Quarkus converters
     * should be used even if some third-party JAR bundles its own converters, unless these 3rd
     * party converters priority is explicitly higher to override Quarkus ones. This way we can be
     * sure that things have a good chance of consistent interoperability.
     */
    public static final int DEFAULT_QUARKUS_CONVERTER_PRIORITY = 200;

    private ConverterSupport() {
        // this is utility class
    }
}
