package io.quarkus.spiffe.client.runtime.internal;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.microprofile.config.spi.Converter;

import io.smallrye.common.net.Inet;
import io.smallrye.common.os.OS;

/**
 * Validates and converts SPIFFE Workload Endpoint socket URIs per the
 * <a href="https://github.com/spiffe/spiffe/blob/main/standards/SPIFFE_Workload_Endpoint.md">SPIFFE Workload
 * Endpoint</a> specification.
 */
public final class SpiffeEndpointSocketConverter implements Converter<URI>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public URI convert(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        value = value.trim();

        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw invalid(value, e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            throw invalid(value, "scheme is required (unix or tcp)");
        }
        if (uri.isOpaque()) {
            throw invalid(value, "opaque URI is not allowed");
        }
        if (uri.getQuery() != null) {
            throw invalid(value, "query component is not allowed");
        }
        if (uri.getFragment() != null) {
            throw invalid(value, "fragment component is not allowed");
        }

        switch (scheme) {
            case "unix" -> validateUnix(value, uri);
            case "tcp" -> validateTcp(value, uri);
            default -> throw invalid(value, "scheme must be unix or tcp, got '" + scheme + "'");
        }

        return uri;
    }

    private static void validateUnix(String value, URI uri) {
        if (OS.WINDOWS.isCurrent()) {
            throw invalid(value, "the SPIFFE client extension does not support unix scheme on Windows, use tcp instead");
        }
        if (uri.getHost() != null) {
            throw invalid(value, "authority component is not allowed for unix scheme");
        }
        String path = uri.getPath();
        if (path == null || path.length() <= 1) {
            throw invalid(value, "path is required for unix scheme");
        }
    }

    private static void validateTcp(String value, URI uri) {
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw invalid(value, "host is required for tcp scheme");
        }
        if (!isIpAddress(host)) {
            throw invalid(value, "host must be an IP address, not a hostname");
        }
        if (uri.getPort() == -1) {
            throw invalid(value, "port is required for tcp scheme");
        }
        if (uri.getPath() != null && !uri.getPath().isEmpty()) {
            throw invalid(value, "path is not allowed for tcp scheme");
        }
        if (uri.getUserInfo() != null) {
            throw invalid(value, "userinfo is not allowed for tcp scheme");
        }
    }

    private static boolean isIpAddress(String host) {
        return Inet.parseInetAddress(host) != null;
    }

    private static IllegalArgumentException invalid(String value, String reason) {
        return new IllegalArgumentException("Invalid SPIFFE endpoint socket '" + value + "': " + reason);
    }
}
