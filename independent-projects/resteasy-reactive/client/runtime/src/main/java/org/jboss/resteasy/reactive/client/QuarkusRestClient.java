package org.jboss.resteasy.reactive.client;

import io.netty.channel.EventLoopGroup;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.TimeoutStream;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.dns.DnsClientOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.http.WebsocketVersion;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.spi.VerticleFactory;
import io.vertx.core.streams.ReadStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import org.jboss.resteasy.reactive.client.handlers.ClientErrorHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientRequestFiltersRestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientResponseRestHandler;
import org.jboss.resteasy.reactive.client.handlers.ClientSendRequestHandler;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.jaxrs.QuarkusRestConfiguration;

public class QuarkusRestClient implements Client {

    final ClientContext clientContext;
    final boolean closeVertx;
    final HttpClient httpClient;
    final QuarkusRestConfiguration configuration;
    final HostnameVerifier hostnameVerifier;
    final SSLContext sslContext;
    private boolean isClosed;
    final ClientRestHandler[] handlerChain;
    final ClientRestHandler[] abortHandlerChain;
    final Vertx vertx;

    public QuarkusRestClient(QuarkusRestConfiguration configuration, ClientContext clientContext,
            HostnameVerifier hostnameVerifier,
            SSLContext sslContext) {
        this.configuration = configuration != null ? configuration : new QuarkusRestConfiguration(RuntimeType.CLIENT);
        this.clientContext = clientContext;
        this.hostnameVerifier = hostnameVerifier;
        this.sslContext = sslContext;
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
        this.httpClient = this.vertx.createHttpClient();
        abortHandlerChain = new ClientRestHandler[] { new ClientErrorHandler() };
        handlerChain = new ClientRestHandler[] { new ClientRequestFiltersRestHandler(), new ClientSendRequestHandler(),
                new ClientResponseRestHandler() };
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
    }

    void abortIfClosed() {
        if (isClosed)
            throw new IllegalStateException("Client is closed");
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
        return new QuarkusRestWebTarget(this, httpClient, uriBuilder, new QuarkusRestConfiguration(configuration), handlerChain,
                abortHandlerChain, null);
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
    public QuarkusRestConfiguration getConfiguration() {
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

        public LazyVertx(Supplier<Vertx> supplier) {
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
        public HttpClient createHttpClient(HttpClientOptions httpClientOptions) {
            return new LazyHttpClient(new Supplier<HttpClient>() {
                @Override
                public HttpClient get() {
                    return getDelegate().createHttpClient(httpClientOptions);
                }
            });
        }

        @Override
        public HttpClient createHttpClient() {
            return new LazyHttpClient(new Supplier<HttpClient>() {
                @Override
                public HttpClient get() {
                    return getDelegate().createHttpClient();
                }
            });
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
        public long setTimer(long l, Handler<Long> handler) {
            return getDelegate().setTimer(l, handler);
        }

        @Override
        public TimeoutStream timerStream(long l) {
            return getDelegate().timerStream(l);
        }

        @Override
        public long setPeriodic(long l, Handler<Long> handler) {
            return getDelegate().setPeriodic(l, handler);
        }

        @Override
        public TimeoutStream periodicStream(long l) {
            return getDelegate().periodicStream(l);
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
        public void close() {
            if (supplied != null) { // no need to close if we never obtained a reference
                getDelegate().close();
            }
        }

        @Override
        public void close(Handler<AsyncResult<Void>> handler) {
            if (supplied != null) { // no need to close if we never obtained a reference
                getDelegate().close(handler);
            }
        }

        @Override
        public void deployVerticle(Verticle verticle) {
            getDelegate().deployVerticle(verticle);
        }

        @Override
        public void deployVerticle(Verticle verticle, Handler<AsyncResult<String>> handler) {
            getDelegate().deployVerticle(verticle, handler);
        }

        @Override
        public void deployVerticle(Verticle verticle, DeploymentOptions deploymentOptions) {
            getDelegate().deployVerticle(verticle, deploymentOptions);
        }

        @Override
        public void deployVerticle(Class<? extends Verticle> aClass, DeploymentOptions deploymentOptions) {
            getDelegate().deployVerticle(aClass, deploymentOptions);
        }

        @Override
        public void deployVerticle(Supplier<Verticle> supplier, DeploymentOptions deploymentOptions) {
            getDelegate().deployVerticle(supplier, deploymentOptions);
        }

        @Override
        public void deployVerticle(Verticle verticle, DeploymentOptions deploymentOptions,
                Handler<AsyncResult<String>> handler) {
            getDelegate().deployVerticle(verticle, deploymentOptions, handler);
        }

        @Override
        public void deployVerticle(Class<? extends Verticle> aClass, DeploymentOptions deploymentOptions,
                Handler<AsyncResult<String>> handler) {
            getDelegate().deployVerticle(aClass, deploymentOptions, handler);
        }

        @Override
        public void deployVerticle(Supplier<Verticle> supplier, DeploymentOptions deploymentOptions,
                Handler<AsyncResult<String>> handler) {
            getDelegate().deployVerticle(supplier, deploymentOptions, handler);
        }

        @Override
        public void deployVerticle(String s) {
            getDelegate().deployVerticle(s);
        }

        @Override
        public void deployVerticle(String s, Handler<AsyncResult<String>> handler) {
            getDelegate().deployVerticle(s, handler);
        }

        @Override
        public void deployVerticle(String s, DeploymentOptions deploymentOptions) {
            getDelegate().deployVerticle(s, deploymentOptions);
        }

        @Override
        public void deployVerticle(String s, DeploymentOptions deploymentOptions, Handler<AsyncResult<String>> handler) {
            getDelegate().deployVerticle(s, deploymentOptions, handler);
        }

        @Override
        public void undeploy(String s) {
            getDelegate().undeploy(s);
        }

        @Override
        public void undeploy(String s, Handler<AsyncResult<Void>> handler) {
            getDelegate().undeploy(s, handler);
        }

        @Override
        public Set<String> deploymentIDs() {
            return getDelegate().deploymentIDs();
        }

        @Override
        public void registerVerticleFactory(VerticleFactory verticleFactory) {
            getDelegate().registerVerticleFactory(verticleFactory);
        }

        @Override
        public void unregisterVerticleFactory(VerticleFactory verticleFactory) {
            getDelegate().unregisterVerticleFactory(verticleFactory);
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
        public <T> void executeBlocking(Handler<Promise<T>> handler, boolean b, Handler<AsyncResult<T>> handler1) {
            getDelegate().executeBlocking(handler, b, handler1);
        }

        @Override
        public <T> void executeBlocking(Handler<Promise<T>> handler, Handler<AsyncResult<T>> handler1) {
            getDelegate().executeBlocking(handler, handler1);
        }

        @Override
        public EventLoopGroup nettyEventLoopGroup() {
            return getDelegate().nettyEventLoopGroup();
        }

        @Override
        public WorkerExecutor createSharedWorkerExecutor(String s) {
            return getDelegate().createSharedWorkerExecutor(s);
        }

        @Override
        public WorkerExecutor createSharedWorkerExecutor(String s, int i) {
            return getDelegate().createSharedWorkerExecutor(s, i);
        }

        @Override
        public WorkerExecutor createSharedWorkerExecutor(String s, int i, long l) {
            return getDelegate().createSharedWorkerExecutor(s, i, l);
        }

        @Override
        public WorkerExecutor createSharedWorkerExecutor(String s, int i, long l, TimeUnit timeUnit) {
            return getDelegate().createSharedWorkerExecutor(s, i, l, timeUnit);
        }

        @Override
        public boolean isNativeTransportEnabled() {
            return getDelegate().isNativeTransportEnabled();
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

        private static class LazyHttpClient implements HttpClient {
            private final Supplier<HttpClient> supplier;
            private volatile HttpClient supplied = null;

            public LazyHttpClient(Supplier<HttpClient> supplier) {
                this.supplier = supplier;
            }

            private HttpClient getDelegate() {
                if (supplied == null) {
                    supplied = supplier.get();
                }
                return supplied;
            }

            @Override
            public HttpClientRequest request(HttpMethod httpMethod, SocketAddress socketAddress,
                    RequestOptions requestOptions) {
                return getDelegate().request(httpMethod, socketAddress, requestOptions);
            }

            @Override
            public HttpClientRequest request(HttpMethod httpMethod, RequestOptions requestOptions) {
                return getDelegate().request(httpMethod, requestOptions);
            }

            @Override
            public HttpClientRequest request(HttpMethod httpMethod, int i, String s, String s1) {
                return getDelegate().request(httpMethod, i, s, s1);
            }

            @Override
            public HttpClientRequest request(HttpMethod httpMethod, SocketAddress socketAddress, int i, String s, String s1) {
                return getDelegate().request(httpMethod, socketAddress, i, s, s1);
            }

            @Override
            public HttpClientRequest request(HttpMethod httpMethod, String s, String s1) {
                return getDelegate().request(httpMethod, s, s1);
            }

            @Override
            @Deprecated
            public HttpClientRequest request(HttpMethod httpMethod, RequestOptions requestOptions,
                    Handler<HttpClientResponse> handler) {
                return getDelegate().request(httpMethod, requestOptions, handler);
            }

            @Override
            public HttpClientRequest request(HttpMethod httpMethod, SocketAddress socketAddress, RequestOptions requestOptions,
                    Handler<HttpClientResponse> handler) {
                return getDelegate().request(httpMethod, socketAddress, requestOptions, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest request(HttpMethod httpMethod, int i, String s, String s1,
                    Handler<HttpClientResponse> handler) {
                return getDelegate().request(httpMethod, i, s, s1, handler);
            }

            @Override
            public HttpClientRequest request(HttpMethod httpMethod, SocketAddress socketAddress, int i, String s, String s1,
                    Handler<HttpClientResponse> handler) {
                return getDelegate().request(httpMethod, socketAddress, i, s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest request(HttpMethod httpMethod, String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().request(httpMethod, s, s1, handler);
            }

            @Override
            public HttpClientRequest request(HttpMethod httpMethod, String s) {
                return getDelegate().request(httpMethod, s);
            }

            @Override
            @Deprecated
            public HttpClientRequest request(HttpMethod httpMethod, String s, Handler<HttpClientResponse> handler) {
                return getDelegate().request(httpMethod, s, handler);
            }

            @Override
            public HttpClientRequest requestAbs(HttpMethod httpMethod, String s) {
                return getDelegate().requestAbs(httpMethod, s);
            }

            @Override
            public HttpClientRequest requestAbs(HttpMethod httpMethod, SocketAddress socketAddress, String s) {
                return getDelegate().requestAbs(httpMethod, socketAddress, s);
            }

            @Override
            @Deprecated
            public HttpClientRequest requestAbs(HttpMethod httpMethod, String s, Handler<HttpClientResponse> handler) {
                return getDelegate().requestAbs(httpMethod, s, handler);
            }

            @Override
            public HttpClientRequest requestAbs(HttpMethod httpMethod, SocketAddress socketAddress, String s,
                    Handler<HttpClientResponse> handler) {
                return getDelegate().requestAbs(httpMethod, socketAddress, s, handler);
            }

            @Override
            public HttpClientRequest get(RequestOptions requestOptions) {
                return getDelegate().get(requestOptions);
            }

            @Override
            public HttpClientRequest get(int i, String s, String s1) {
                return getDelegate().get(i, s, s1);
            }

            @Override
            public HttpClientRequest get(String s, String s1) {
                return getDelegate().get(s, s1);
            }

            @Override
            @Deprecated
            public HttpClientRequest get(RequestOptions requestOptions, Handler<HttpClientResponse> handler) {
                return getDelegate().get(requestOptions, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest get(int i, String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().get(i, s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest get(String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().get(s, s1, handler);
            }

            @Override
            public HttpClientRequest get(String s) {
                return getDelegate().get(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest get(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().get(s, handler);
            }

            @Override
            public HttpClientRequest getAbs(String s) {
                return getDelegate().getAbs(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest getAbs(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().getAbs(s, handler);
            }

            @Override
            @Deprecated
            public HttpClient getNow(RequestOptions requestOptions, Handler<HttpClientResponse> handler) {
                return getDelegate().getNow(requestOptions, handler);
            }

            @Override
            @Deprecated
            public HttpClient getNow(int i, String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().getNow(i, s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClient getNow(String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().getNow(s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClient getNow(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().getNow(s, handler);
            }

            @Override
            public HttpClientRequest post(RequestOptions requestOptions) {
                return getDelegate().post(requestOptions);
            }

            @Override
            public HttpClientRequest post(int i, String s, String s1) {
                return getDelegate().post(i, s, s1);
            }

            @Override
            public HttpClientRequest post(String s, String s1) {
                return getDelegate().post(s, s1);
            }

            @Override
            @Deprecated
            public HttpClientRequest post(RequestOptions requestOptions, Handler<HttpClientResponse> handler) {
                return getDelegate().post(requestOptions, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest post(int i, String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().post(i, s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest post(String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().post(s, s1, handler);
            }

            @Override
            public HttpClientRequest post(String s) {
                return getDelegate().post(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest post(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().post(s, handler);
            }

            @Override
            public HttpClientRequest postAbs(String s) {
                return getDelegate().postAbs(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest postAbs(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().postAbs(s, handler);
            }

            @Override
            public HttpClientRequest head(RequestOptions requestOptions) {
                return getDelegate().head(requestOptions);
            }

            @Override
            public HttpClientRequest head(int i, String s, String s1) {
                return getDelegate().head(i, s, s1);
            }

            @Override
            public HttpClientRequest head(String s, String s1) {
                return getDelegate().head(s, s1);
            }

            @Override
            @Deprecated
            public HttpClientRequest head(RequestOptions requestOptions, Handler<HttpClientResponse> handler) {
                return getDelegate().head(requestOptions, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest head(int i, String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().head(i, s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest head(String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().head(s, s1, handler);
            }

            @Override
            public HttpClientRequest head(String s) {
                return getDelegate().head(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest head(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().head(s, handler);
            }

            @Override
            public HttpClientRequest headAbs(String s) {
                return getDelegate().headAbs(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest headAbs(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().headAbs(s, handler);
            }

            @Override
            @Deprecated
            public HttpClient headNow(RequestOptions requestOptions, Handler<HttpClientResponse> handler) {
                return getDelegate().headNow(requestOptions, handler);
            }

            @Override
            @Deprecated
            public HttpClient headNow(int i, String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().headNow(i, s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClient headNow(String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().headNow(s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClient headNow(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().headNow(s, handler);
            }

            @Override
            public HttpClientRequest options(RequestOptions requestOptions) {
                return getDelegate().options(requestOptions);
            }

            @Override
            public HttpClientRequest options(int i, String s, String s1) {
                return getDelegate().options(i, s, s1);
            }

            @Override
            public HttpClientRequest options(String s, String s1) {
                return getDelegate().options(s, s1);
            }

            @Override
            @Deprecated
            public HttpClientRequest options(RequestOptions requestOptions, Handler<HttpClientResponse> handler) {
                return getDelegate().options(requestOptions, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest options(int i, String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().options(i, s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest options(String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().options(s, s1, handler);
            }

            @Override
            public HttpClientRequest options(String s) {
                return getDelegate().options(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest options(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().options(s, handler);
            }

            @Override
            public HttpClientRequest optionsAbs(String s) {
                return getDelegate().optionsAbs(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest optionsAbs(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().optionsAbs(s, handler);
            }

            @Override
            @Deprecated
            public HttpClient optionsNow(RequestOptions requestOptions, Handler<HttpClientResponse> handler) {
                return getDelegate().optionsNow(requestOptions, handler);
            }

            @Override
            @Deprecated
            public HttpClient optionsNow(int i, String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().optionsNow(i, s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClient optionsNow(String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().optionsNow(s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClient optionsNow(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().optionsNow(s, handler);
            }

            @Override
            public HttpClientRequest put(RequestOptions requestOptions) {
                return getDelegate().put(requestOptions);
            }

            @Override
            public HttpClientRequest put(int i, String s, String s1) {
                return getDelegate().put(i, s, s1);
            }

            @Override
            public HttpClientRequest put(String s, String s1) {
                return getDelegate().put(s, s1);
            }

            @Override
            @Deprecated
            public HttpClientRequest put(RequestOptions requestOptions, Handler<HttpClientResponse> handler) {
                return getDelegate().put(requestOptions, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest put(int i, String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().put(i, s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest put(String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().put(s, s1, handler);
            }

            @Override
            public HttpClientRequest put(String s) {
                return getDelegate().put(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest put(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().put(s, handler);
            }

            @Override
            public HttpClientRequest putAbs(String s) {
                return getDelegate().putAbs(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest putAbs(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().putAbs(s, handler);
            }

            @Override
            public HttpClientRequest delete(RequestOptions requestOptions) {
                return getDelegate().delete(requestOptions);
            }

            @Override
            public HttpClientRequest delete(int i, String s, String s1) {
                return getDelegate().delete(i, s, s1);
            }

            @Override
            public HttpClientRequest delete(String s, String s1) {
                return getDelegate().delete(s, s1);
            }

            @Override
            @Deprecated
            public HttpClientRequest delete(RequestOptions requestOptions, Handler<HttpClientResponse> handler) {
                return getDelegate().delete(requestOptions, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest delete(int i, String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().delete(i, s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClientRequest delete(String s, String s1, Handler<HttpClientResponse> handler) {
                return getDelegate().delete(s, s1, handler);
            }

            @Override
            public HttpClientRequest delete(String s) {
                return getDelegate().delete(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest delete(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().delete(s, handler);
            }

            @Override
            public HttpClientRequest deleteAbs(String s) {
                return getDelegate().deleteAbs(s);
            }

            @Override
            @Deprecated
            public HttpClientRequest deleteAbs(String s, Handler<HttpClientResponse> handler) {
                return getDelegate().deleteAbs(s, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(RequestOptions requestOptions, Handler<WebSocket> handler) {
                return getDelegate().websocket(requestOptions, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(int i, String s, String s1, Handler<WebSocket> handler) {
                return getDelegate().websocket(i, s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(RequestOptions requestOptions, Handler<WebSocket> handler,
                    Handler<Throwable> handler1) {
                return getDelegate().websocket(requestOptions, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(int i, String s, String s1, Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(i, s, s1, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, String s1, Handler<WebSocket> handler) {
                return getDelegate().websocket(s, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, String s1, Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(s, s1, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(RequestOptions requestOptions, MultiMap multiMap, Handler<WebSocket> handler) {
                return getDelegate().websocket(requestOptions, multiMap, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(int i, String s, String s1, MultiMap multiMap, Handler<WebSocket> handler) {
                return getDelegate().websocket(i, s, s1, multiMap, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(RequestOptions requestOptions, MultiMap multiMap, Handler<WebSocket> handler,
                    Handler<Throwable> handler1) {
                return getDelegate().websocket(requestOptions, multiMap, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(int i, String s, String s1, MultiMap multiMap, Handler<WebSocket> handler,
                    Handler<Throwable> handler1) {
                return getDelegate().websocket(i, s, s1, multiMap, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, String s1, MultiMap multiMap, Handler<WebSocket> handler) {
                return getDelegate().websocket(s, s1, multiMap, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, String s1, MultiMap multiMap, Handler<WebSocket> handler,
                    Handler<Throwable> handler1) {
                return getDelegate().websocket(s, s1, multiMap, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(RequestOptions requestOptions, MultiMap multiMap, WebsocketVersion websocketVersion,
                    Handler<WebSocket> handler) {
                return getDelegate().websocket(requestOptions, multiMap, websocketVersion, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(int i, String s, String s1, MultiMap multiMap, WebsocketVersion websocketVersion,
                    Handler<WebSocket> handler) {
                return getDelegate().websocket(i, s, s1, multiMap, websocketVersion, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(RequestOptions requestOptions, MultiMap multiMap, WebsocketVersion websocketVersion,
                    Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(requestOptions, multiMap, websocketVersion, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(int i, String s, String s1, MultiMap multiMap, WebsocketVersion websocketVersion,
                    Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(i, s, s1, multiMap, websocketVersion, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, String s1, MultiMap multiMap, WebsocketVersion websocketVersion,
                    Handler<WebSocket> handler) {
                return getDelegate().websocket(s, s1, multiMap, websocketVersion, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, String s1, MultiMap multiMap, WebsocketVersion websocketVersion,
                    Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(s, s1, multiMap, websocketVersion, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(RequestOptions requestOptions, MultiMap multiMap, WebsocketVersion websocketVersion,
                    String s, Handler<WebSocket> handler) {
                return getDelegate().websocket(requestOptions, multiMap, websocketVersion, s, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(int i, String s, String s1, MultiMap multiMap, WebsocketVersion websocketVersion,
                    String s2, Handler<WebSocket> handler) {
                return getDelegate().websocket(i, s, s1, multiMap, websocketVersion, s2, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocketAbs(String s, MultiMap multiMap, WebsocketVersion websocketVersion, String s1,
                    Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocketAbs(s, multiMap, websocketVersion, s1, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(RequestOptions requestOptions, MultiMap multiMap, WebsocketVersion websocketVersion,
                    String s, Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(requestOptions, multiMap, websocketVersion, s, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(int i, String s, String s1, MultiMap multiMap, WebsocketVersion websocketVersion,
                    String s2, Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(i, s, s1, multiMap, websocketVersion, s2, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, String s1, MultiMap multiMap, WebsocketVersion websocketVersion, String s2,
                    Handler<WebSocket> handler) {
                return getDelegate().websocket(s, s1, multiMap, websocketVersion, s2, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, String s1, MultiMap multiMap, WebsocketVersion websocketVersion, String s2,
                    Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(s, s1, multiMap, websocketVersion, s2, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, Handler<WebSocket> handler) {
                return getDelegate().websocket(s, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(s, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, MultiMap multiMap, Handler<WebSocket> handler) {
                return getDelegate().websocket(s, multiMap, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, MultiMap multiMap, Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(s, multiMap, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, MultiMap multiMap, WebsocketVersion websocketVersion,
                    Handler<WebSocket> handler) {
                return getDelegate().websocket(s, multiMap, websocketVersion, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, MultiMap multiMap, WebsocketVersion websocketVersion,
                    Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(s, multiMap, websocketVersion, handler, handler1);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, MultiMap multiMap, WebsocketVersion websocketVersion, String s1,
                    Handler<WebSocket> handler) {
                return getDelegate().websocket(s, multiMap, websocketVersion, s1, handler);
            }

            @Override
            @Deprecated
            public HttpClient websocket(String s, MultiMap multiMap, WebsocketVersion websocketVersion, String s1,
                    Handler<WebSocket> handler, Handler<Throwable> handler1) {
                return getDelegate().websocket(s, multiMap, websocketVersion, s1, handler, handler1);
            }

            @Override
            public void webSocket(int i, String s, String s1, Handler<AsyncResult<WebSocket>> handler) {
                getDelegate().webSocket(i, s, s1, handler);
            }

            @Override
            public void webSocket(String s, String s1, Handler<AsyncResult<WebSocket>> handler) {
                getDelegate().webSocket(s, s1, handler);
            }

            @Override
            public void webSocket(String s, Handler<AsyncResult<WebSocket>> handler) {
                getDelegate().webSocket(s, handler);
            }

            @Override
            public void webSocket(WebSocketConnectOptions webSocketConnectOptions, Handler<AsyncResult<WebSocket>> handler) {
                getDelegate().webSocket(webSocketConnectOptions, handler);
            }

            @Override
            public void webSocketAbs(String s, MultiMap multiMap, WebsocketVersion websocketVersion, List<String> list,
                    Handler<AsyncResult<WebSocket>> handler) {
                getDelegate().webSocketAbs(s, multiMap, websocketVersion, list, handler);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(RequestOptions requestOptions) {
                return getDelegate().websocketStream(requestOptions);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(int i, String s, String s1) {
                return getDelegate().websocketStream(i, s, s1);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(String s, String s1) {
                return getDelegate().websocketStream(s, s1);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(RequestOptions requestOptions, MultiMap multiMap) {
                return getDelegate().websocketStream(requestOptions, multiMap);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(int i, String s, String s1, MultiMap multiMap) {
                return getDelegate().websocketStream(i, s, s1, multiMap);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(String s, String s1, MultiMap multiMap) {
                return getDelegate().websocketStream(s, s1, multiMap);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(RequestOptions requestOptions, MultiMap multiMap,
                    WebsocketVersion websocketVersion) {
                return getDelegate().websocketStream(requestOptions, multiMap, websocketVersion);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(int i, String s, String s1, MultiMap multiMap,
                    WebsocketVersion websocketVersion) {
                return getDelegate().websocketStream(i, s, s1, multiMap, websocketVersion);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(String s, String s1, MultiMap multiMap,
                    WebsocketVersion websocketVersion) {
                return getDelegate().websocketStream(s, s1, multiMap, websocketVersion);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStreamAbs(String s, MultiMap multiMap, WebsocketVersion websocketVersion,
                    String s1) {
                return getDelegate().websocketStreamAbs(s, multiMap, websocketVersion, s1);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(RequestOptions requestOptions, MultiMap multiMap,
                    WebsocketVersion websocketVersion, String s) {
                return getDelegate().websocketStream(requestOptions, multiMap, websocketVersion, s);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(int i, String s, String s1, MultiMap multiMap,
                    WebsocketVersion websocketVersion, String s2) {
                return getDelegate().websocketStream(i, s, s1, multiMap, websocketVersion, s2);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(String s, String s1, MultiMap multiMap,
                    WebsocketVersion websocketVersion, String s2) {
                return getDelegate().websocketStream(s, s1, multiMap, websocketVersion, s2);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(String s) {
                return getDelegate().websocketStream(s);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(String s, MultiMap multiMap) {
                return getDelegate().websocketStream(s, multiMap);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(String s, MultiMap multiMap, WebsocketVersion websocketVersion) {
                return getDelegate().websocketStream(s, multiMap, websocketVersion);
            }

            @Override
            @Deprecated
            public ReadStream<WebSocket> websocketStream(String s, MultiMap multiMap, WebsocketVersion websocketVersion,
                    String s1) {
                return getDelegate().websocketStream(s, multiMap, websocketVersion, s1);
            }

            @Override
            public HttpClient connectionHandler(Handler<HttpConnection> handler) {
                return getDelegate().connectionHandler(handler);
            }

            @Override
            public HttpClient redirectHandler(Function<HttpClientResponse, Future<HttpClientRequest>> function) {
                return getDelegate().redirectHandler(function);
            }

            @Override
            public Function<HttpClientResponse, Future<HttpClientRequest>> redirectHandler() {
                return getDelegate().redirectHandler();
            }

            @Override
            public void close() {
                if (supplied != null) { // no need to close if we never obtained a reference
                    getDelegate().close();
                }
            }

            @Override
            public boolean isMetricsEnabled() {
                return getDelegate().isMetricsEnabled();
            }
        }
    }
}
