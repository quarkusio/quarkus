package org.jboss.shamrock.runtime.configuration;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.eclipse.microprofile.config.spi.Converter;
import org.wildfly.common.net.Inet;

/**
 * A converter which converts a socket address in the form of {@code &lt;host-or-address&gt;[:&lt;port&gt;]} into
 * an instance of {@link InetSocketAddress}.  If an address is given, then a resolved instance is returned, otherwise
 * an unresolved instance is returned.
 */
public class InetSocketAddressConverter implements Converter<InetSocketAddress> {

    @Override
    public InetSocketAddress convert(final String value) {
        if (value.isEmpty()) return null;
        final int lastColon = value.lastIndexOf(':');
        final int lastCloseBracket = value.lastIndexOf(']');
        final String hostPart;
        final int portPart;
        if (lastColon == -1 || lastCloseBracket != -1 && lastColon < lastCloseBracket) {
            // no port #
            hostPart = value;
            portPart = 0;
        } else {
            hostPart = value.substring(0, lastColon);
            portPart = Integer.parseInt(value.substring(lastColon + 1));
        }
        InetAddress resolved = Inet.parseInetAddress(hostPart);
        return resolved == null ? InetSocketAddress.createUnresolved(value, 0) : new InetSocketAddress(resolved, portPart);
    }
}
