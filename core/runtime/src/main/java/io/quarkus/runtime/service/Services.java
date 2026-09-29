package io.quarkus.runtime.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class Services {
    private static final String SCHEME = "quarkus";

    private static final AtomicReference<Resolver> RESOLVER = new AtomicReference<>();

    private Services() {
        throw new UnsupportedOperationException();
    }

    public static Address resolve(final String reference) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Cannot resolve a blank reference");
        }
        try {
            return resolve(new URI(reference.trim()));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot resolve '" + reference + "': " + e.getReason(), e);
        }
    }

    public static Address resolve(final URI reference) {
        if (!SCHEME.equals(reference.getScheme())) {
            // Not a reference to a Quarkus service. Pass it through, but still classified as an Address so that
            // consumers have a single type to handle.
            return new Address(reference);
        }

        String name = serviceName(reference);
        return from(reference, RESOLVER.get().resolve(name));
    }

    static Address from(URI reference, ServiceInstance instance) {
        String socket = instance.domainSocket().orElse(null);

        if (socket != null) {
            return Address.from(reference, instance.scheme(), instance.host(), -1, instance.path().orElse(null),
                    socket);
        }

        int port = instance.port();
        if (port <= 0) {
            throw new IllegalStateException("Service '" + serviceName(reference) + "' reported port " + port + ", " +
                    "which means it is not listening. Was it resolved before the server started?");
        }
        return Address.from(reference, instance.scheme(), instance.host(), port, instance.path().orElse(null), null);
    }

    static String serviceName(URI reference) {
        String authority = reference.getRawAuthority();
        if (authority == null || authority.isBlank()) {
            throw new IllegalArgumentException("'" + reference + "' does not name a service");
        }
        int at = authority.lastIndexOf('@');
        String name = at >= 0 ? authority.substring(at + 1) : authority;
        if (name.indexOf(':') >= 0) {
            throw new IllegalArgumentException(
                    "'" + reference + "' must not specify a port; the port is what the service resolves to");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("'" + reference + "' does not name a service");
        }
        return name;
    }

    public interface Resolver {
        ServiceInstance resolve(String name);
    }

    public record ServiceInstance(
            String scheme,
            String host,
            int port,
            Optional<String> path,
            Optional<String> domainSocket) {
    }

    public static void initialize(final Resolver resolver) {
        RESOLVER.compareAndSet(null, resolver);
    }
}