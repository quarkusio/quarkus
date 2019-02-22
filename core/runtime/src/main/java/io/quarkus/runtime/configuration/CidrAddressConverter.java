package io.quarkus.runtime.configuration;

import org.eclipse.microprofile.config.spi.Converter;
import org.wildfly.common.net.CidrAddress;
import org.wildfly.common.net.Inet;

/**
 * A converter which converts a CIDR address into an instance of {@link CidrAddress}.
 */
public class CidrAddressConverter implements Converter<CidrAddress> {

    @Override
    public CidrAddress convert(final String value) {
        if (value.isEmpty())
            return null;
        final CidrAddress result = Inet.parseCidrAddress(value);
        if (result == null)
            throw new IllegalArgumentException("Failed to parse CIDR address \"" + value + "\"");
        return result;
    }
}
