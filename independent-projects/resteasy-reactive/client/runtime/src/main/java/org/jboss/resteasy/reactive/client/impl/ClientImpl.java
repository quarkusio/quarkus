package org.jboss.resteasy.reactive.client.impl;

import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.CAPTURE_STACKTRACE;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.CONNECTION_POOL_SIZE;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.CONNECTION_TTL;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.CONNECT_TIMEOUT;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.KEEP_ALIVE_ENABLED;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.MAX_HEADER_SIZE;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.MAX_INITIAL_LINE_LENGTH;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.MAX_REDIRECTS;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.NAME;
import static org.jboss.resteasy.reactive.client.api.QuarkusRestClientProperties.SHARED;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import org.jboss.resteasy.reactive.client.handlers.AdvancedRedirectHandler;
import org.jboss.resteasy.reactive.client.handlers.RedirectHandler;
import org.jboss.resteasy.reactive.client.spi.ClientContext;
import org.jboss.resteasy.reactive.common.jaxrs.ConfigurationImpl;
import org.jboss.resteasy.reactive.common.jaxrs.MultiQueryParamMode;
import org.jboss.resteasy.reactive.common.jaxrs.UriBuilderImpl;

import io.vertx.core.Context;
import io.vertx.core.Deployable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Timer;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.dns.DnsClientOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientBuilder;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.PoolOptions;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.spi.VerticleFactory;

public class ClientImpl implements Client {

    private static final Logger log = Logger.getLogger(ClientImpl.class);

    private static final int DEFAULT_CONNECT_TIMEOUT = 15000;
    private static final int DEFAULT_CONNECTION_POOL_SIZE = 50;

    final ClientContext clientContext;
    final boolean closeVertx;
    final HttpClient httpClient;
    final ConfigurationImpl configuration;
    final HostnameVerifier hostnameVerifier;
    final SSLContext sslContext;
    private boolean isClosed;
    final HandlerChain handlerChain;
    final Vertx vertx;
    private final MultiQueryParamMode multiQueryParamMode;
    private final String userAgent;
    private final String tlsConfigName;

    public ClientImpl(HttpClientOptions options, ConfigurationImpl configuration, ClientContext clientContext,
            HostnameVerifier hostnameVerifier,
            SSLContext sslContext, boolean followRedirects,
            MultiQueryParamMode multiQueryParamMode,
            LoggingScope loggingScope,
            ClientLogger clientLogger, String userAgent,
            String tlsConfigName,
            List<Consumer<HttpClientRequest>> clientRequestCustomizers) {
        this.userAgent = userAgent;
        this.tlsConfigName = tlsConfigName;
        configuration = configuration != null ? configuration : new ConfigurationImpl(RuntimeType.CLIENT);
        this.configuration = configuration;
        this.clientContext = clientContext;
        this.hostnameVerifier = hostnameVerifier;
        this.sslContext = sslContext;
        this.multiQueryParamMode = multiQueryParamMode;
        Supplier<Vertx> vertx = clientContext.getVertx();
        if (vertx != null) {
            this.vertx = vertx.get();
            closeVertx = false;
        } else {
            this.vertx = new LazyVertx(new Supplier<Vertx>() {
                @Override
                public Vertx get() {
                    return Vertx.vertx();
                }
            });
            closeVertx = true;
        }
        Object connectTimeoutMs = configuration.getProperty(CONNECT_TIMEOUT);
        if (connectTimeoutMs == null) {
            options.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        } else {
            options.setConnectTimeout((int) connectTimeoutMs);
        }

        Object maxRedirects = configuration.getProperty(MAX_REDIRECTS);
        if (maxRedirects != null) {
            options.setMaxRedirects((Integer) maxRedirects);
        }

        Object maxHeaderSize = configuration.getProperty(MAX_HEADER_SIZE);
        if (maxHeaderSize != null) {
            options.setMaxHeaderSize((Integer) maxHeaderSize);
        }

        Object maxInitialLineLength = configuration.getProperty(MAX_INITIAL_LINE_LENGTH);
        if (maxInitialLineLength != null) {
            options.setMaxInitialLineLength((Integer) maxInitialLineLength);
        }

        Object connectionTTL = configuration.getProperty(CONNECTION_TTL);
        if (connectionTTL != null) {
            options.setKeepAliveTimeout((int) connectionTTL);
            options.setHttp2KeepAliveTimeout((int) connectionTTL);
        }

        Object connectionPoolSize = configuration.getProperty(CONNECTION_POOL_SIZE);
        if (connectionPoolSize == null) {
            connectionPoolSize = DEFAULT_CONNECTION_POOL_SIZE;
        } else {
            log.debugf("Setting connectionPoolSize to %d", connectionPoolSize);
        }
        Object keepAliveEnabled = configuration.getProperty(KEEP_ALIVE_ENABLED);
        if (keepAliveEnabled != null) {
            Boolean enabled = (Boolean) keepAliveEnabled;
            options.setKeepAlive(enabled);

            if (!enabled) {
                log.debug("keep alive disabled");
            }
        }

        if (loggingScope == LoggingScope.ALL) {
            options.setLogActivity(true);
        }

        Object name = configuration.getProperty(NAME);
        if (name != null) {
            log.debugf("Setting client name to %s", name);
            options.setName((String) name);
        }

        Object shared = configuration.getProperty(SHARED);
        if (shared != null && (boolean) shared) {
            log.debugf("Sharing of the HTTP client '%s' enabled", options.getName());
            options.setShared(true);
        }

        var poolOptions = new PoolOptions()
                .setHttp1MaxSize((int) connectionPoolSize)
                .setHttp2MaxSize((int) connectionPoolSize);
        var httpClientBuilder = this.vertx.httpClientBuilder().with(options).with(poolOptions);

        Function<HttpClientResponse, Future<RequestOptions>> redirectFunction = null;
        AdvancedRedirectHandler advancedRedirectHandler = configuration.getFromContext(AdvancedRedirectHandler.class);
        if (advancedRedirectHandler != null) {
            redirectFunction = new WrapperVertxAdvancedRedirectHandlerImpl(advancedRedirectHandler);
        } else {
            RedirectHandler redirectHandler = configuration.getFromContext(RedirectHandler.class);
            if (redirectHandler != null) {
                redirectFunction = new WrapperVertxRedirectHandlerImpl(redirectHandler);
            }
        }

        if (redirectFunction != null) {
            if (loggingScope != LoggingScope.NONE) {
                Function<HttpClientResponse, Future<RequestOptions>> delegate = redirectFunction;
                redirectFunction = response -> {
                    clientLogger.logResponse(response, true);
                    return delegate.apply(response);
                };
            }
            httpClientBuilder.withRedirectHandler(redirectFunction);
        }

        httpClient = httpClientBuilder.build();

        handlerChain = new HandlerChain(options, isCaptureStacktrace(configuration),
                followRedirects,
                loggingScope,
                clientContext.getMultipartResponsesData(),
                clientLogger,
                clientRequestCustomizers);
    }

    public HttpClient getVertxHttpClient() {
        return httpClient;
    }

    private boolean isCaptureStacktrace(ConfigurationImpl configuration) {
        Object captureStacktraceObj = configuration.getProperty(CAPTURE_STACKTRACE);
        if (captureStacktraceObj == null) {
            return false;
        }
        return (boolean) captureStacktraceObj;
    }

    public ClientContext getClientContext() {
        return clientContext;
    }

    @Override
    public void close() {
        if (isClosed)
            return;
        isClosed = true;
        httpClient.close();
        if (closeVertx) {
            vertx.close();
        }
        log.debug("Client is closed");
    }

    void abortIfClosed() {
        if (isClosed)
            throw new IllegalStateException("Client is closed");
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getTlsConfigName() {
        return tlsConfigName;
    }

    @Override
    public WebTarget target(String uri) {
        // close is checked in the other target call
        Objects.requireNonNull(uri);
        return target(UriBuilder.fromUri(uri));
    }

    @Override
    public WebTarget target(URI uri) {
        // close is checked in the other target call
        Objects.requireNonNull(uri);
        return target(UriBuilder.fromUri(uri));
    }

    @Override
    public WebTarget target(UriBuilder uriBuilder) {
        abortIfClosed();
        Objects.requireNonNull(uriBuilder);
        if (uriBuilder instanceof UriBuilderImpl && multiQueryParamMode != null) {
            ((UriBuilderImpl) uriBuilder).multiQueryParamMode(multiQueryParamMode);
        }
        return new WebTargetImpl(this, httpClient, uriBuilder, new ConfigurationImpl(configuration), handlerChain, null);
    }

    @Override
    public WebTarget target(Link link) {
        // close is checked in the other target call
        Objects.requireNonNull(link);
        return target(UriBuilder.fromLink(link));
    }

    @Override
    public Invocation.Builder invocation(Link link) {
        abortIfClosed();
        Objects.requireNonNull(link);
        Builder request = target(link).request();
        if (link.getType() != null)
            request.accept(link.getType());
        return request;
    }

    @Override
    public SSLContext getSslContext() {
        abortIfClosed();
        return sslContext;
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        abortIfClosed();
        return hostnameVerifier;
    }

    @Override
    public ConfigurationImpl getConfiguration() {
        abortIfClosed();
        return configuration;
    }

    @Override
    public Client property(String name, Object value) {
        abortIfClosed();
        configuration.property(name, value);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass) {
        abortIfClosed();
        configuration.register(componentClass);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, int priority) {
        abortIfClosed();
        configuration.register(componentClass, priority);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Class<?>... contracts) {
        abortIfClosed();
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        abortIfClosed();
        configuration.register(componentClass, contracts);
        return this;
    }

    @Override
    public Client register(Object component) {
        abortIfClosed();
        configuration.register(component);
        return this;
    }

    @Override
    public Client register(Object component, int priority) {
        abortIfClosed();
        configuration.register(component, priority);
        return this;
    }

    @Override
    public Client register(Object component, Class<?>... contracts) {
        abortIfClosed();
        configuration.register(component, contracts);
        return this;
    }

    @Override
    public Client register(Object component, Map<Class<?>, Integer> contracts) {
        abortIfClosed();
        configuration.register(component, contracts);
        return this;
    }

    Vertx getVertx() {
        return vertx;
    }

    /**
     * The point of this class is to not obtain a Vertx reference unless it's absolutely necessary.
     * We do this in order to avoid needing a Vertx object unless an proper client request is made.
     * This saves us the need from needing to close the Vertx object.
     */
    private static class LazyVertx implements Vertx {
        private final Supplier<Vertx> supplier;
        private volatile Vertx supplied = null;

        LazyVertx(Supplier<Vertx> supplier) {
            this.supplier = supplier;
        }

        private Vertx getDelegate() {
            if (supplied == null) {
                supplied = supplier.get();
            }
            return supplied;
        }

        @Override
        public Context getOrCreateContext() {
            return getDelegate().getOrCreateContext();
        }

        @Override
        public NetServer createNetServer(NetServerOptions netServerOptions) {
            return getDelegate().createNetServer(netServerOptions);
        }

        @Override
        public NetServer createNetServer() {
            return getDelegate().createNetServer();
        }

        @Override
        public NetClient createNetClient(NetClientOptions netClientOptions) {
            return getDelegate().createNetClient(netClientOptions);
        }

        @Override
        public NetClient createNetClient() {
            return getDelegate().createNetClient();
        }

        @Override
        public HttpServer createHttpServer(HttpServerOptions httpServerOptions) {
            return getDelegate().createHttpServer(httpServerOptions);
        }

        @Override
        public HttpServer createHttpServer() {
            return getDelegate().createHttpServer();
        }

        @Override
        public WebSocketClient createWebSocketClient(WebSocketClientOptions options) {
            return getDelegate().createWebSocketClient(options);
        }

        @Override
        public HttpClientBuilder httpClientBuilder() {
            return getDelegate().httpClientBuilder();
        }

        @Override
        public HttpClientAgent createHttpClient(HttpClientOptions httpClientOptions) {
            return getDelegate().createHttpClient(httpClientOptions);
        }

        @Override
        public HttpClientAgent createHttpClient() {
            return getDelegate().createHttpClient();
        }

        @Override
        public DatagramSocket createDatagramSocket(DatagramSocketOptions datagramSocketOptions) {
            return getDelegate().createDatagramSocket(datagramSocketOptions);
        }

        @Override
        public DatagramSocket createDatagramSocket() {
            return getDelegate().createDatagramSocket();
        }

        @Override
        public FileSystem fileSystem() {
            return getDelegate().fileSystem();
        }

        @Override
        public EventBus eventBus() {
            return getDelegate().eventBus();
        }

        @Override
        public DnsClient createDnsClient(int i, String s) {
            return getDelegate().createDnsClient(i, s);
        }

        @Override
        public DnsClient createDnsClient() {
            return getDelegate().createDnsClient();
        }

        @Override
        public DnsClient createDnsClient(DnsClientOptions dnsClientOptions) {
            return getDelegate().createDnsClient(dnsClientOptions);
        }

        @Override
        public SharedData sharedData() {
            return getDelegate().sharedData();
        }

        @Override
        public Timer timer(long delay, TimeUnit unit) {
            return getDelegate().timer(delay, unit);
        }

        @Override
        public long setTimer(long l, Handler<Long> handler) {
            return getDelegate().setTimer(l, handler);
        }

        @Override
        public long setPeriodic(long l, Handler<Long> handler) {
            return getDelegate().setPeriodic(l, handler);
        }

        @Override
        public long setPeriodic(long initialDelay, long delay, Handler<Long> handler) {
            return getDelegate().setPeriodic(initialDelay, delay, handler);
        }

        @Override
        public boolean cancelTimer(long l) {
            return getDelegate().cancelTimer(l);
        }

        @Override
        public void runOnContext(Handler<Void> handler) {
            getDelegate().runOnContext(handler);
        }

        @Override
        public Future<Void> close() {
            if (supplied != null) { // no need to close if we never obtained a reference
                return getDelegate().close();
            }
            return Future.succeededFuture();
        }

        @Override
        public Future<String> deployVerticle(Deployable verticle) {
            return getDelegate().deployVerticle(verticle);
        }

        public static Vertx vertx() {
            return Vertx.vertx();
        }

        public static Vertx vertx(VertxOptions options) {
            return Vertx.vertx(options);
        }

        public static Future<Vertx> clusteredVertx(VertxOptions options) {
            return Vertx.clusteredVertx(options);
        }

        public static Context currentContext() {
            return Vertx.currentContext();
        }

        @Override
        public Future<String> deployVerticle(Deployable verticle, DeploymentOptions options) {
            return getDelegate().deployVerticle(verticle, options);
        }

        @Override
        public Future<String> deployVerticle(Class<? extends Deployable> verticleClass, DeploymentOptions options) {
            return getDelegate().deployVerticle(verticleClass, options);
        }

        @Override
        public Future<String> deployVerticle(Supplier<? extends Deployable> verticleSupplier, DeploymentOptions options) {
            return getDelegate().deployVerticle(verticleSupplier, options);
        }

        @Override
        public Future<String> deployVerticle(String name) {
            return getDelegate().deployVerticle(name);
        }

        @Override
        public Future<String> deployVerticle(String name, DeploymentOptions options) {
            return getDelegate().deployVerticle(name, options);
        }

        @Override
        public Future<Void> undeploy(String deploymentID) {
            return getDelegate().undeploy(deploymentID);
        }

        @Override
        public Set<String> deploymentIDs() {
            return getDelegate().deploymentIDs();
        }

        @Override
        public void registerVerticleFactory(VerticleFactory factory) {
            getDelegate().registerVerticleFactory(factory);
        }

        @Override
        public void unregisterVerticleFactory(VerticleFactory factory) {
            getDelegate().unregisterVerticleFactory(factory);
        }

        @Override
        public Set<VerticleFactory> verticleFactories() {
            return getDelegate().verticleFactories();
        }

        @Override
        public boolean isClustered() {
            return getDelegate().isClustered();
        }

        @Override
        public <T> Future<T> executeBlocking(Callable<T> blockingCodeHandler, boolean ordered) {
            return getDelegate().executeBlocking(blockingCodeHandler, ordered);
        }

        @Override
        public <T> Future<T> executeBlocking(Callable<T> blockingCodeHandler) {
            return getDelegate().executeBlocking(blockingCodeHandler);
        }

        @Override
        public WorkerExecutor createSharedWorkerExecutor(String name) {
            return getDelegate().createSharedWorkerExecutor(name);
        }

        @Override
        public WorkerExecutor createSharedWorkerExecutor(String name, int poolSize) {
            return getDelegate().createSharedWorkerExecutor(name, poolSize);
        }

        @Override
        public WorkerExecutor createSharedWorkerExecutor(String name, int poolSize, long maxExecuteTime) {
            return getDelegate().createSharedWorkerExecutor(name, poolSize, maxExecuteTime);
        }

        @Override
        public WorkerExecutor createSharedWorkerExecutor(String name, int poolSize, long maxExecuteTime,
                TimeUnit maxExecuteTimeUnit) {
            return getDelegate().createSharedWorkerExecutor(name, poolSize, maxExecuteTime, maxExecuteTimeUnit);
        }

        @Override
        public boolean isNativeTransportEnabled() {
            return getDelegate().isNativeTransportEnabled();
        }

        @Override
        public Throwable unavailableNativeTransportCause() {
            return getDelegate().unavailableNativeTransportCause();
        }

        @Override
        public Vertx exceptionHandler(Handler<Throwable> handler) {
            return getDelegate().exceptionHandler(handler);
        }

        @Override
        public Handler<Throwable> exceptionHandler() {
            return getDelegate().exceptionHandler();
        }

        @Override
        public boolean isMetricsEnabled() {
            return getDelegate().isMetricsEnabled();
        }
    }
}
