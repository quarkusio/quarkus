package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;
import java.time.ZoneId;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

/**
 * A converter to support ZoneId.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class ZoneIdConverter implements Converter<ZoneId>, Serializable {

    private static final long serialVersionUID = -439010527617997936L;

    @Override
    public ZoneId convert(final String value) {
        return ZoneId.of(value);
    }
}
