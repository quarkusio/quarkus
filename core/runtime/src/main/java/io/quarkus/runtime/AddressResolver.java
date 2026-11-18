package io.quarkus.runtime;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.inject.spi.CDI;

import io.smallrye.common.constraint.Assert;
import io.smallrye.common.net.Inet;

/**
 * Provides a low-level API for resolving references to the {@link Address} of a service of the running application,
 * such as {@code quarkus://vertx-http/hello}, to the URL where that service is bound. Typically, such a reference is
 * the configured URL of a client of a service of the same application, so that the client reaches the service on the
 * port it is bound to, known only at application startup.
 * <p>
 * The authority of a reference is the name of the {@link Address}, such as {@code vertx-http}, {@code vertx-https} or
 * {@code vertx-management}, with which the service exposes it as a {@code @Named} CDI bean. The resolved URL takes its
 * scheme from {@link Address#protocol()}, and its host and port from {@link Address#inet()}. The user info, path, query
 * and fragment of the reference are kept as written, so a reference to the management interface specifies its root
 * path, as in {@code quarkus://vertx-management/q/health}.
 * <p>
 * A value with any other scheme is returned as is, so a consumer handles a plain URL and a reference with the same
 * code path.
 * <p>
 * While primarily targeting Quarkus Extensions that connect to a service of the same application, general Quarkus
 * Applications can also use the {@link AddressResolver}.
 */
public final class AddressResolver {
    /**
     * The scheme of a reference to an {@link Address}.
     */
    public static final String SCHEME = "quarkus";

    private AddressResolver() {
        throw new UnsupportedOperationException();
    }

    /**
     * Resolves the specified reference with the {@code @Named} {@link Address} beans of the running application. A
     * plain URL is returned as is, and does not require a running application.
     *
     * @param value the reference to resolve, such as {@code quarkus://vertx-http/hello}, or a plain URL
     * @return the URL to which the specified reference resolves
     * @throws IllegalArgumentException if the specified value is not a valid URI, or does not name an {@link Address}
     * @throws IllegalStateException if no {@link Address} with the specified name is available, or if that
     *         {@link Address} has no TCP/IP socket address
     */
    public static URI resolve(final String value) {
        return resolve(AddressResolver::lookup, value);
    }

    /**
     * Resolves the specified reference with the specified lookup of an {@link Address} by name. A plain URL is
     * returned as is.
     *
     * @param addresses the lookup of an {@link Address} by name, which returns an empty {@link Optional} when no
     *        {@link Address} with that name is available
     * @param value the reference to resolve, such as {@code quarkus://vertx-http/hello}, or a plain URL
     * @return the URL to which the specified reference resolves
     * @throws IllegalArgumentException if the specified value is not a valid URI, or does not name an {@link Address}
     * @throws IllegalStateException if no {@link Address} with the specified name is available, or if that
     *         {@link Address} has no TCP/IP socket address
     */
    public static URI resolve(final Function<String, Optional<Address>> addresses, final String value) {
        return resolve(addresses, parse(value));
    }

    private static URI resolve(final Function<String, Optional<Address>> addresses, final URI reference) {
        if (!SCHEME.equals(reference.getScheme())) {
            return reference;
        }

        String name = addressName(reference);
        Address address = addresses.apply(name).orElseThrow(() -> new IllegalStateException(
                "No address is available as '" + name + "'. Either nothing is listening under that name, or '"
                        + reference + "' was resolved before the server bound its sockets"));
        InetSocketAddress socket = address.inet().orElseThrow(() -> new IllegalStateException(
                "'" + name + "' is only bound to " + address + ", which a URL cannot express"));

        StringBuilder resolved = new StringBuilder(address.protocol()).append("://");
        String authority = reference.getRawAuthority();
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            resolved.append(authority, 0, at + 1);
        }

        InetAddress inetAddress = socket.getAddress();
        String host = inetAddress.isAnyLocalAddress() ? "localhost" : Inet.toURLString(inetAddress, true);
        resolved.append(host).append(':').append(socket.getPort());
        if (reference.getRawPath() != null) {
            resolved.append(reference.getRawPath());
        }
        if (reference.getRawQuery() != null) {
            resolved.append('?').append(reference.getRawQuery());
        }
        if (reference.getRawFragment() != null) {
            resolved.append('#').append(reference.getRawFragment());
        }
        return URI.create(resolved.toString());
    }

    /**
     * Looks up the {@code @Named} {@link Address} bean with the specified name. The bean of a service that is not
     * bound, such as {@code vertx-https} without TLS, has no value and fails on access, which counts as no
     * {@link Address} being available.
     */
    private static Optional<Address> lookup(final String name) {
        Instance<Address> instance = CDI.current().select(Address.class, NamedLiteral.of(name));
        if (instance.isUnsatisfied()) {
            return Optional.empty();
        }
        try {
            return Optional.of(instance.get());
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }

    private static URI parse(final String value) {
        Assert.checkNotNullParam("value", value);
        String reference = Assert.checkNotEmptyParam("value", value.trim());
        try {
            return new URI(reference);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot resolve '" + value + "': " + e.getReason(), e);
        }
    }

    private static String addressName(final URI reference) {
        String authority = reference.getRawAuthority();
        if (authority == null || authority.isBlank()) {
            throw new IllegalArgumentException("'" + reference + "' does not name an address");
        }
        int at = authority.lastIndexOf('@');
        String name = at >= 0 ? authority.substring(at + 1) : authority;
        if (name.indexOf(':') >= 0) {
            throw new IllegalArgumentException(
                    "'" + reference + "' must not specify a port; the port is what the reference resolves to");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("'" + reference + "' does not name an address");
        }
        return name;
    }
}
