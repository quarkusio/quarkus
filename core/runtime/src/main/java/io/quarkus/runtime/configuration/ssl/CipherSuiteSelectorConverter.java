package io.quarkus.runtime.configuration.ssl;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;
import org.wildfly.security.ssl.CipherSuiteSelector;

/**
 * A converter for SSL cipher suites.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public final class CipherSuiteSelectorConverter implements Converter<CipherSuiteSelector> {

    /**
     * Construct a new instance.
     */
    public CipherSuiteSelectorConverter() {
    }

    public CipherSuiteSelector convert(final String value) {
        return CipherSuiteSelector.fromString(value);
    }
}
