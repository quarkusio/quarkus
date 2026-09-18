package io.quarkus.runtime.service;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public final class Address {
    private final URI uri;
    private final String domainSocket;

    public Address(URI uri) {
        this(uri, null);
    }

    public Address(URI uri, String domainSocket) {
        this.uri = Objects.requireNonNull(uri, "uri");
        this.domainSocket = domainSocket;
    }

    public URI uri() {
        return uri;
    }

    public Optional<String> domainSocket() {
        return Optional.ofNullable(domainSocket);
    }

    public static Address from(
            final URI reference,
            final String scheme, final String host, final int port, final String instancePath,
            final String domainSocket) {

        StringBuilder uri = new StringBuilder(scheme).append("://");

        String authority = reference.getRawAuthority();
        int at = authority == null ? -1 : authority.lastIndexOf('@');
        if (at >= 0) {
            uri.append(authority, 0, at + 1);
        }

        uri.append(host);
        if (port > 0) {
            uri.append(':').append(port);
        }

        if (instancePath != null && !instancePath.isBlank()) {
            String prefix = instancePath.startsWith("/") ? instancePath : "/" + instancePath;
            while (prefix.endsWith("/")) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
            uri.append(prefix);
        }
        if (reference.getRawPath() != null) {
            uri.append(reference.getRawPath());
        }
        if (reference.getRawQuery() != null) {
            uri.append('?').append(reference.getRawQuery());
        }
        if (reference.getRawFragment() != null) {
            uri.append('#').append(reference.getRawFragment());
        }

        return new Address(URI.create(uri.toString()), domainSocket);
    }
}
