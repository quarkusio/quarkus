package io.quarkus.rest.client.reactive.deployment.devservices;

import static io.vertx.core.impl.SysProps.DISABLE_DNS_RESOLVER;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.quarkus.rest.client.reactive.spi.DevServicesRestClientProxyProvider;
import io.quarkus.rest.client.reactive.spi.RestClientHttpProxyBuildItem;
import io.quarkus.runtime.ResettableSystemProperties;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.file.FileSystemOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.SocketAddress;
import io.vertx.httpproxy.HttpProxy;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyRequest;
import io.vertx.httpproxy.ProxyResponse;

/**
 * A simple implementation of {@link DevServicesRestClientProxyProvider} that creates a pass-through proxy
 * based on {@code vertx-http-proxy}
 */
public class VertxHttpProxyDevServicesRestClientProxyProvider implements DevServicesRestClientProxyProvider {

    public static final VertxHttpProxyDevServicesRestClientProxyProvider INSTANCE = new VertxHttpProxyDevServicesRestClientProxyProvider();

    static final String NAME = "default";

    protected static final Logger log = Logger.getLogger(VertxHttpProxyDevServicesRestClientProxyProvider.class);

    private static final AtomicReference<Vertx> vertx = new AtomicReference<>();

    // protected for testing
    protected VertxHttpProxyDevServicesRestClientProxyProvider() {
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Closeable setup() {
        if (vertx.get() == null) {
            vertx.set(createVertx());
        }

        return new VertxClosingCloseable(vertx);
    }

    @Override
    public CreateResult create(RestClientHttpProxyBuildItem buildItem) {
        // The application hands the origin on every start, after the proxy started
        AtomicReference<URI> origin = new AtomicReference<>();

        HttpClient proxyClient = vertx.get().createHttpClient(new HttpClientOptions());
        HttpProxy proxy = HttpProxy.reverseProxy(proxyClient);
        proxy.origin(context -> {
            URI target = origin.get();
            if (target == null) {
                return Future.failedFuture("The origin of the HTTP proxy for REST Client '" + buildItem.getClassName()
                        + "' is not available. Either the application has not started yet, or neither 'quarkus.rest-client.\""
                        + buildItem.getClassName() + "\".url' nor 'quarkus.rest-client.\"" + buildItem.getClassName()
                        + "\".uri' is set");
            }
            return context.client().request(new RequestOptions()
                    .setServer(SocketAddress.inetSocketAddress(determineOriginPort(target), target.getHost()))
                    .setSsl("https".equals(target.getScheme())));
        }).addInterceptor(new OriginInterceptor(origin));

        HttpServer proxyServer = vertx.get().createHttpServer();
        Integer proxyPort = findRandomPort();
        proxyServer.requestHandler(proxy).listen(proxyPort);

        logStartup(buildItem.getClassName(), proxyPort);

        return new CreateResult("localhost", proxyPort, origin::set, new HttpServerClosable(proxyServer));
    }

    protected void logStartup(String className, Integer port) {
        log.info("Started HTTP proxy server on http://localhost:" + port + " for REST Client '" + className
                + "'");
    }

    private Vertx createVertx() {
        try (var ignored = ResettableSystemProperties.of(
                DISABLE_DNS_RESOLVER.name, "true")) {
            return Vertx.vertx(
                    new VertxOptions()
                            .setFileSystemOptions(
                                    new FileSystemOptions().setFileCachingEnabled(false).setClassPathResolvingEnabled(false))
                            .setMetricsOptions(new MetricsOptions().setEnabled(false))
                            .setEventLoopPoolSize(2)
                            .setWorkerPoolSize(2)
                            .setInternalBlockingPoolSize(2));
        }
    }

    private static int determineOriginPort(URI baseUri) {
        if (baseUri.getPort() != -1) {
            return baseUri.getPort();
        }
        if (baseUri.getScheme().equals("https")) {
            return 443;
        }
        return 80;
    }

    private Integer findRandomPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Sends each request to the origin: under its path, since the REST Client calls the proxy at its root, and with its
     * authority, which sets the Host HTTP Header in order to avoid having services being blocked for presenting a wrong
     * value.
     */
    private static class OriginInterceptor implements ProxyInterceptor {

        private final AtomicReference<URI> origin;

        private OriginInterceptor(AtomicReference<URI> origin) {
            this.origin = origin;
        }

        @Override
        public Future<ProxyResponse> handleProxyRequest(ProxyContext context) {
            URI target = origin.get();
            if (target != null) {
                ProxyRequest request = context.request();
                request.setAuthority(HostAndPort.authority(target.getHost(), determineOriginPort(target)));
                String basePath = target.getRawPath();
                if (basePath != null && basePath.endsWith("/")) {
                    basePath = basePath.substring(0, basePath.length() - 1);
                }
                if (basePath != null && !basePath.isEmpty()) {
                    request.setURI(basePath + request.getURI());
                }
            }

            return context.sendRequest();
        }
    }

    private static class HttpServerClosable implements Closeable {
        private final HttpServer server;

        public HttpServerClosable(HttpServer server) {
            this.server = server;
        }

        @Override
        public void close() throws IOException {
            try {
                server.close().toCompletionStage().toCompletableFuture().join();
            } catch (Exception e) {
                log.debug("Error closing HTTP Proxy server", e);
            }
        }
    }

    private static class VertxClosingCloseable implements Closeable {
        private final AtomicReference<Vertx> vertx;

        public VertxClosingCloseable(AtomicReference<Vertx> vertx) {
            this.vertx = vertx;
        }

        @Override
        public void close() {
            try {
                vertx.get().close().toCompletionStage().toCompletableFuture().join();
            } catch (Exception e) {
                log.debug("Error closing Vertx", e);
            }
            vertx.set(null);
        }
    }
}
