package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.function.Function;

public final class BuildOutputChangesTransports {

    private BuildOutputChangesTransports() {
    }

    static AutoCloseable connect(DevModeContext.ExternalBuildOutputTransport transport,
            Function<BuildOutputChanges, BuildOutputChangesApplyStatus> consumer) throws IOException {
        requireNonNull(consumer, "consumer");
        if (transport == null || !transport.isEnabled()) {
            return () -> {
            };
        }

        var uri = transport.getUri()
                .orElseThrow(() -> new IllegalArgumentException("External build output transport URI is required"));
        var scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("External build output transport URI scheme is required: " + uri);
        }
        switch (scheme.toLowerCase(Locale.ROOT)) {
            case "tcp" -> {
                String host = uri.getHost();
                int port = uri.getPort();
                if (host == null || host.isBlank()) {
                    throw new IllegalArgumentException("External build output TCP URI host is required: " + uri);
                }
                if (port < 0) {
                    throw new IllegalArgumentException("External build output TCP URI port is required: " + uri);
                }
                InetAddress address = loopbackAddress(host);
                return new BuildOutputChangesTcpClient(new InetSocketAddress(address, port),
                        requiredToken(transport), consumer);
            }
            default -> throw new IllegalArgumentException("Unsupported external build output transport URI scheme: " + scheme);
        }
    }

    /**
     * Creates a build-tool-side TCP listener for local Quarkus dev-mode output
     * updates.
     */
    public static BuildOutputChangesServer createTcpServer() throws IOException {
        return new BuildOutputChangesTcpServer();
    }

    private static InetAddress loopbackAddress(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            if (!address.isLoopbackAddress()) {
                throw new IllegalArgumentException("External build output TCP URI host must be a loopback address: " + host);
            }
            return address;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("External build output TCP URI host cannot be resolved: " + host, e);
        }
    }

    private static String requiredToken(DevModeContext.ExternalBuildOutputTransport transport) {
        var token = transport.getToken()
                .orElseThrow(() -> new IllegalArgumentException("External build output transport token is required"));
        if (token.isBlank()) {
            throw new IllegalArgumentException("External build output transport token must not be blank");
        }
        if (token.indexOf('\n') >= 0 || token.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("External build output transport token must not contain line breaks");
        }
        return token;
    }
}
