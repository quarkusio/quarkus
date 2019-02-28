package io.quarkus.runtime.configuration.ssl;

import org.eclipse.microprofile.config.spi.Converter;
import org.wildfly.security.ssl.Protocol;

/**
 * A converter for SSL/TLS protocol names.
 */
public final class ProtocolConverter implements Converter<Protocol> {
    /**
     * Construct a new instance.
     */
    public ProtocolConverter() {
    }

    public Protocol convert(final String value) {
        final Protocol protocol = Protocol.forName(value);
        if (protocol == null)
            throw new IllegalArgumentException("Unknown protocol value: " + value);
        return protocol;
    }
}
