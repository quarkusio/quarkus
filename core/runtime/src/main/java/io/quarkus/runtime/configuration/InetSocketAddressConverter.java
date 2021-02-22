package io.quarkus.runtime.configuration;

import static io.quarkus.runtime.configuration.ConverterSupport.DEFAULT_QUARKUS_CONVERTER_PRIORITY;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;
import org.wildfly.common.net.Inet;

/**
 * A converter which converts a socket address in the form of {@code &lt;host-or-address&gt;[:&lt;port&gt;]} into
 * an instance of {@link InetSocketAddress}. If an address is given, then a resolved instance is returned, otherwise
 * an unresolved instance is returned.
 */
@Priority(DEFAULT_QUARKUS_CONVERTER_PRIORITY)
public class InetSocketAddressConverter implements Converter<InetSocketAddress>, Serializable {

    private static final long serialVersionUID = 1928336763333858343L;

    @Override
    public InetSocketAddress convert(String value) {
        value = value.trim();
        if (value.isEmpty()) {
            return null;
        }
        final int lastColon = value.lastIndexOf(':');
        final int lastCloseBracket = value.lastIndexOf(']');
        String hostPart;
        final int portPart;
        if (lastColon == -1 || lastCloseBracket != -1 && lastColon < lastCloseBracket) {
            // no port #
            hostPart = value;
            portPart = 0;
        } else {
            hostPart = value.substring(0, lastColon);
            portPart = Integer.parseInt(value.substring(lastColon + 1));
        }
        while (hostPart.startsWith("[") && hostPart.endsWith("]")) {
            hostPart = hostPart.substring(1, hostPart.length() - 1);
        }
        if (hostPart.isEmpty()) {
            return new InetSocketAddress(portPart);
        } else {
            InetAddress resolved = Inet.parseInetAddress(hostPart);
            return resolved == null ? InetSocketAddress.createUnresolved(hostPart, portPart)
                    : new InetSocketAddress(resolved, portPart);
        }
    }
}
