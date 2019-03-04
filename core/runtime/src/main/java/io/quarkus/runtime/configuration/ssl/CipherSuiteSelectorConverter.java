package io.quarkus.runtime.configuration.ssl;

import org.eclipse.microprofile.config.spi.Converter;
import org.wildfly.security.ssl.CipherSuiteSelector;

/**
 * A converter for SSL cipher suites.
 */
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
