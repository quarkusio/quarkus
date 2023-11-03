package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;
import org.wildfly.common.net.Inet;

/**
 * A converter which produces values of type {@link InetAddress}.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class InetAddressConverter implements Converter<InetAddress>, Serializable {

    private static final long serialVersionUID = 4539214213710330204L;

    @Override
    public InetAddress convert(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        final InetAddress parsed = Inet.parseInetAddress(value);
        if (parsed != null) {
            return parsed;
        }
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unable to resolve \"" + value + "\"", e);
        }
    }
}
