package io.quarkus.runtime;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import io.quarkus.value.registry.ValueRegistry;

/**
 * Provides a low-level API for resolving references to the {@link Address} of a service of the running application,
 * such as {@code quarkus://vertx-http/hello}, to the URL where that service is bound. Typically, such a reference is
 * the configured URL of a client of a service of the same application, so that the client reaches the service on the
 * port it is bound to, known only at application startup.
 * <p>
 * The authority of a reference is the name with which the {@link Address} is registered in the {@link ValueRegistry},
 * such as {@code vertx-http}, {@code vertx-https} or {@code vertx-management}. The resolved URL takes its scheme from
 * {@link Address#protocol()}, and its host and port from {@link Address#inet()}. The user info, path, query and
 * fragment of the reference are kept as written, so a reference to the management interface specifies its root path,
 * as in {@code quarkus://vertx-management/q/health}.
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
     * Resolves the specified reference with the {@link ValueRegistry} of the running {@link Application}. A plain URL
     * is returned as is, and does not require a running application.
     *
     * @param value the reference to resolve, such as {@code quarkus://vertx-http/hello}, or a plain URL
     * @return the URL to which the specified reference resolves
     * @throws IllegalArgumentException if the specified value is not a valid URI, or does not name an {@link Address}
     * @throws IllegalStateException if no application is running, if no {@link Address} is registered with the
     *         specified name, or if that {@link Address} has no TCP/IP socket address
     */
    public static URI resolve(final String value) {
        URI uri = parse(value);
        if (!SCHEME.equals(uri.getScheme())) {
            return uri;
        }
        Application application = Application.currentApplication();
        if (application == null) {
            throw new IllegalStateException("Cannot resolve '" + value + "': no application is running");
        }
        return resolve(application.getValueRegistry(), uri);
    }

    /**
     * Resolves the specified reference with the specified {@link ValueRegistry}. A plain URL is returned as is.
     *
     * @param valueRegistry the {@link ValueRegistry} in which the referenced {@link Address} is registered
     * @param value the reference to resolve, such as {@code quarkus://vertx-http/hello}, or a plain URL
     * @return the URL to which the specified reference resolves
     * @throws IllegalArgumentException if the specified value is not a valid URI, or does not name an {@link Address}
     * @throws IllegalStateException if no {@link Address} is registered with the specified name, or if that
     *         {@link Address} has no TCP/IP socket address
     */
    public static URI resolve(final ValueRegistry valueRegistry, final String value) {
        return resolve(valueRegistry, parse(value));
    }

    private static URI resolve(final ValueRegistry valueRegistry, final URI reference) {
        if (!SCHEME.equals(reference.getScheme())) {
            return reference;
        }

        String name = addressName(reference);
        Address address = valueRegistry.getOrDefault(Address.key(name), null);
        if (address == null) {
            throw new IllegalStateException("No address is registered as '" + name + "'. Either nothing is listening " +
                    "under that name, or '" + reference + "' was resolved before the server bound its sockets");
        }
        InetSocketAddress socket = address.inet().orElseThrow(() -> new IllegalStateException(
                "'" + name + "' is only bound to " + address + ", which a URL cannot express"));

        StringBuilder resolved = new StringBuilder(address.protocol()).append("://");
        String authority = reference.getRawAuthority();
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            resolved.append(authority, 0, at + 1);
        }

        String host = socket.getAddress() != null && socket.getAddress().isAnyLocalAddress() ? "localhost"
                : socket.getHostString();
        resolved.append(host.indexOf(':') >= 0 ? "[" + host + "]" : host).append(':').append(socket.getPort());
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

    private static URI parse(final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Cannot resolve a blank reference");
        }
        try {
            return new URI(value.trim());
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
