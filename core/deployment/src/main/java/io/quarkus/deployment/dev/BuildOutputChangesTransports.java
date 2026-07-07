package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Creates the local authenticated transport used between an external
 * build-output producer and a Quarkus dev-mode process.
 * <p>
 * The current transport binds to loopback and uses a per-server token. The
 * returned public contracts are shared by Quarkus build-tool integrations;
 * the framing protocol is not a third-party cross-version compatibility
 * promise.
 */
public final class BuildOutputChangesTransports {

    private BuildOutputChangesTransports() {
    }

    static BuildOutputChangesConnection connect(DevModeContext.ExternalBuildOutputTransport transport,
            Function<BuildOutputChanges, BuildOutputChangesApplyStatus> consumer) throws IOException {
        requireNonNull(consumer, "consumer");
        if (transport == null || !transport.isEnabled()) {
            return new BuildOutputChangesConnection() {
                @Override
                public void liveReloadStateChanged(BuildOutputLiveReloadState state) {
                }

                @Override
                public void close() {
                }
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
     *
     * @return a listening server with launch metadata
     * @throws IOException when the loopback listener cannot be created
     */
    public static BuildOutputChangesServer createTcpServer() throws IOException {
        return new BuildOutputChangesTcpServer();
    }

    /**
     * Creates a build-tool-side TCP listener that also receives asynchronous
     * live-reload state from the Quarkus dev-mode process.
     *
     * @param stateListener callback for monotonic live-reload state updates;
     *        a dedicated dispatcher serializes and may coalesce callbacks
     *        outside the transport reader and connection lock
     * @return a listening server with launch metadata
     * @throws IOException when the loopback listener cannot be created
     */
    public static BuildOutputChangesServer createTcpServer(Consumer<BuildOutputLiveReloadState> stateListener)
            throws IOException {
        return new BuildOutputChangesTcpServer(requireNonNull(stateListener, "stateListener"));
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
