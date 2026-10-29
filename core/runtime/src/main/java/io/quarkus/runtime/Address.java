package io.quarkus.runtime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;

/**
 * The address where a Quarkus listener can be reached: a {@link URI} and, optionally, a Unix domain socket path.
 * <p>
 * A listener publishes its {@code Address} once it has bound its sockets, so it carries the actual values (for
 * instance, a random port that was only known after binding), and not the configured ones. Each address has a name,
 * such as {@code vertx-http} for the main HTTP listener, and can be reached in three ways:
 * <ul>
 * <li>Code in the startup graph depends on it with {@code require(Address.class, "<name>")}.</li>
 * <li>CDI code injects {@code @Named("<name>") Address}.</li>
 * <li>Code that receives a {@code quarkus://<name>} reference as a plain string, typically from configuration such as
 * {@code quarkus.rest-client.my-client.url=quarkus://vertx-http}, resolves it with {@link #resolve(String)}. This only
 * needs the running {@link Application}, not CDI: the address is published in the application {@link ValueRegistry}
 * under {@link #key(String)}.</li>
 * </ul>
 */
public final class Address {
    private static final String SCHEME = "quarkus";
    private static final String KEY_PREFIX = "quarkus.address.";

    private final URI uri;
    private final String domainSocket;

    private Address(URI uri, String domainSocket) {
        this.uri = Objects.requireNonNull(uri, "uri");
        this.domainSocket = domainSocket;
    }

    /**
     * An address reachable at the given URI.
     *
     * @param uri the URI where the service can be reached (must not be {@code null})
     * @return the address
     */
    public static Address of(final URI uri) {
        return new Address(uri, null);
    }

    /**
     * An address reachable at the given URI, or over a Unix domain socket.
     *
     * @param uri the URI where the service can be reached (must not be {@code null})
     * @param domainSocket the path of the Unix domain socket, or {@code null} if there is none
     * @return the address
     */
    public static Address of(final URI uri, final String domainSocket) {
        return new Address(uri, domainSocket);
    }

    /**
     * The {@link ValueRegistry} key under which a listener publishes its {@link Address}.
     *
     * @param name the address name, as used in the authority of a {@code quarkus://} reference
     * @return the registry key
     */
    public static RuntimeKey<Address> key(final String name) {
        return RuntimeKey.key(KEY_PREFIX + name, Address.class);
    }

    /**
     * Resolve a reference to the {@link Address} where the listener is actually bound, using the
     * {@link ValueRegistry} of the running {@link Application}.
     * <p>
     * A reference with any other scheme than {@value #SCHEME} is passed through unchanged, and does not need a
     * running application.
     *
     * @param reference the reference, such as {@code quarkus://vertx-http/hello}
     * @return the resolved address
     * @throws IllegalArgumentException if the reference is not a valid URI, or does not carry a name
     * @throws IllegalStateException if no application is running, or no address is published under that name
     * @see #resolve(ValueRegistry, String)
     */
    public static Address resolve(final String reference) {
        URI uri = parse(reference);
        if (!SCHEME.equals(uri.getScheme())) {
            return of(uri);
        }
        Application application = Application.currentApplication();
        if (application == null) {
            throw new IllegalStateException("Cannot resolve '" + reference + "': no application is running");
        }
        return resolve(application.getValueRegistry(), uri);
    }

    /**
     * Resolve a reference to the {@link Address} where the listener is actually bound.
     * <p>
     * A reference with any other scheme than {@value #SCHEME} is passed through unchanged, so consumers can handle a
     * plain URI and an address reference with a single code path.
     *
     * @param valueRegistry the registry where listeners publish their address
     * @param reference the reference, such as {@code quarkus://vertx-http/hello}
     * @return the resolved address
     * @throws IllegalArgumentException if the reference is not a valid URI, or does not carry a name
     * @throws IllegalStateException if no address is published under that name
     */
    public static Address resolve(final ValueRegistry valueRegistry, final String reference) {
        return resolve(valueRegistry, parse(reference));
    }

    private static URI parse(final String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Cannot resolve a blank reference");
        }
        try {
            return new URI(reference.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot resolve '" + reference + "': " + e.getReason(), e);
        }
    }

    /**
     * Resolve a reference to the {@link Address} where the listener is actually bound.
     *
     * @see #resolve(ValueRegistry, String)
     */
    public static Address resolve(final ValueRegistry valueRegistry, final URI reference) {
        if (!SCHEME.equals(reference.getScheme())) {
            // Not an address reference. Pass it through, but still as an Address so that consumers have a single
            // type to handle.
            return of(reference);
        }

        String name = addressName(reference);
        Address address = valueRegistry.getOrDefault(key(name), null);
        if (address == null) {
            throw new IllegalStateException("No address is published as '" + name + "'. Either nothing is listening " +
                    "under that name, or '" + reference + "' was resolved before the listener was bound");
        }
        return address.resolveReference(reference);
    }

    static String addressName(final URI reference) {
        String authority = reference.getRawAuthority();
        if (authority == null || authority.isBlank()) {
            throw new IllegalArgumentException("'" + reference + "' does not carry an address name");
        }
        int at = authority.lastIndexOf('@');
        String name = at >= 0 ? authority.substring(at + 1) : authority;
        if (name.indexOf(':') >= 0) {
            throw new IllegalArgumentException(
                    "'" + reference + "' must not specify a port; the port is what the reference resolves to");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("'" + reference + "' does not carry an address name");
        }
        return name;
    }

    public URI uri() {
        return uri;
    }

    public Optional<String> domainSocket() {
        return Optional.ofNullable(domainSocket);
    }

    /**
     * Resolve a {@code quarkus://} reference against this address.
     * <p>
     * The scheme, host and port come from this address. The user info, path, query and fragment of the reference are
     * appended, so {@code quarkus://vertx-http/hello?x=1} resolved against {@code http://localhost:8081} is
     * {@code http://localhost:8081/hello?x=1}.
     *
     * @param reference the address reference
     * @return the resolved address
     */
    public Address resolveReference(final URI reference) {
        StringBuilder resolved = new StringBuilder(uri.getScheme()).append("://");

        String authority = reference.getRawAuthority();
        int at = authority == null ? -1 : authority.lastIndexOf('@');
        if (at >= 0) {
            resolved.append(authority, 0, at + 1);
        }

        resolved.append(uri.getHost());
        if (uri.getPort() > 0) {
            resolved.append(':').append(uri.getPort());
        }

        String basePath = uri.getRawPath();
        if (basePath != null && !basePath.isBlank()) {
            while (basePath.endsWith("/")) {
                basePath = basePath.substring(0, basePath.length() - 1);
            }
            resolved.append(basePath);
        }
        if (reference.getRawPath() != null) {
            resolved.append(reference.getRawPath());
        }
        if (reference.getRawQuery() != null) {
            resolved.append('?').append(reference.getRawQuery());
        }
        if (reference.getRawFragment() != null) {
            resolved.append('#').append(reference.getRawFragment());
        }

        return of(URI.create(resolved.toString()), domainSocket);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Address address)) {
            return false;
        }
        return uri.equals(address.uri) && Objects.equals(domainSocket, address.domainSocket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, domainSocket);
    }

    @Override
    public String toString() {
        return uri.toString();
    }
}
