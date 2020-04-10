package io.quarkus.vertx.http.runtime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.event.Event;

import org.jboss.logging.Logger;
import org.wildfly.common.cpu.ProcessorInfo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.netty.runtime.virtual.VirtualAddress;
import io.quarkus.netty.runtime.virtual.VirtualChannel;
import io.quarkus.netty.runtime.virtual.VirtualServerChannel;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.Timing;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigInstantiator;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.shutdown.ShutdownConfig;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.quarkus.vertx.http.runtime.HttpConfiguration.InsecureRequests;
import io.quarkus.vertx.http.runtime.filters.Filter;
import io.quarkus.vertx.http.runtime.filters.Filters;
import io.quarkus.vertx.http.runtime.filters.GracefulShutdownFilter;
import io.quarkus.vertx.http.runtime.filters.QuarkusRequestWrapper;
import io.quarkus.vertx.http.runtime.filters.accesslog.AccessLogHandler;
import io.quarkus.vertx.http.runtime.filters.accesslog.AccessLogReceiver;
import io.quarkus.vertx.http.runtime.filters.accesslog.DefaultAccessLogReceiver;
import io.quarkus.vertx.http.runtime.filters.accesslog.JBossLoggingAccessLogReceiver;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.impl.Http1xServerConnection;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.core.net.impl.VertxHandler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

@Recorder
public class VertxHttpRecorder {

    /**
     * The key that the request start time is stored under
     */
    public static final String REQUEST_START_TIME = "io.quarkus.request-start-time";

    public static final String MAX_REQUEST_SIZE_KEY = "io.quarkus.max-request-size";

    private static final Logger LOGGER = Logger.getLogger(VertxHttpRecorder.class.getName());

    private static volatile Handler<RoutingContext> hotReplacementHandler;

    private static volatile Runnable closeTask;

    private static volatile Handler<HttpServerRequest> rootHandler;

    private static final Handler<HttpServerRequest> ACTUAL_ROOT = new Handler<HttpServerRequest>() {
        @Override
        public void handle(HttpServerRequest httpServerRequest) {
            //we need to pause the request to make sure that data does
            //not arrive before handlers have a chance to install a read handler
            //as it is possible filters such as the auth filter can do blocking tasks
            //as the underlying handler has not had a chance to install a read handler yet
            //and data that arrives while the blocking task is being processed will be lost
            httpServerRequest.pause();
            rootHandler.handle(httpServerRequest);
        }
    };

    public static void setHotReplacement(Handler<RoutingContext> handler) {
        hotReplacementHandler = handler;
    }

    public static void shutDownDevMode() {
        closeTask.run();
        closeTask = null;
        rootHandler = null;
        hotReplacementHandler = null;
    }

    public static void startServerAfterFailedStart() {
        if (closeTask != null) {
            //it is possible start failed after the server was started
            //we shut it down in this case, as we have no idea what state it is in
            final Handler<RoutingContext> prevHotReplacementHandler = hotReplacementHandler;
            shutDownDevMode();
            // reset back to the older hot replacement handler, so that it can be used
            // to watch any artifacts that need hot deployment to fix the reason which caused
            // the server start to fail
            hotReplacementHandler = prevHotReplacementHandler;
        }
        VertxConfiguration vertxConfiguration = new VertxConfiguration();
        ConfigInstantiator.handleObject(vertxConfiguration);
        Vertx vertx = VertxCoreRecorder.initialize(vertxConfiguration);

        try {
            HttpConfiguration config = new HttpConfiguration();
            ConfigInstantiator.handleObject(config);

            Router router = Router.router(vertx);
            if (hotReplacementHandler != null) {
                router.route().order(Integer.MIN_VALUE).blockingHandler(hotReplacementHandler);
            }
            rootHandler = router;

            //we can't really do
            doServerStart(vertx, config, LaunchMode.DEVELOPMENT, new Supplier<Integer>() {
                @Override
                public Integer get() {
                    return ProcessorInfo.availableProcessors() * 2; //this is dev mode, so the number of IO threads not always being 100% correct does not really matter in this case
                }
            }, null);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RuntimeValue<Router> initializeRouter(final Supplier<Vertx> vertxRuntimeValue,
            final LaunchMode launchMode, final ShutdownContext shutdownContext) {

        Vertx vertx = vertxRuntimeValue.get();
        Router router = Router.router(vertx);
        if (hotReplacementHandler != null) {
            router.route().order(Integer.MIN_VALUE).handler(hotReplacementHandler);
        }

        return new RuntimeValue<>(router);
    }

    public void startServer(Supplier<Vertx> vertx, ShutdownContext shutdown,
            HttpConfiguration httpConfiguration, LaunchMode launchMode,
            boolean startVirtual, boolean startSocket, Supplier<Integer> ioThreads, String websocketSubProtocols)
            throws IOException {

        if (startVirtual) {
            initializeVirtual(vertx.get());
        }
        if (startSocket) {
            // Start the server
            if (closeTask == null) {
                doServerStart(vertx.get(), httpConfiguration, launchMode, ioThreads, websocketSubProtocols);
                if (launchMode != LaunchMode.DEVELOPMENT) {
                    shutdown.addShutdownTask(closeTask);
                }
            }
        }
    }

    public void finalizeRouter(BeanContainer container, Consumer<Route> defaultRouteHandler,
            List<Filter> filterList, Supplier<Vertx> vertx,
            RuntimeValue<Router> runtimeValue, String rootPath, LaunchMode launchMode, boolean requireBodyHandler,
            Handler<RoutingContext> bodyHandler, HttpConfiguration httpConfiguration,
            GracefulShutdownFilter gracefulShutdownFilter, ShutdownConfig shutdownConfig,
            Executor executor) {
        // install the default route at the end
        Router router = runtimeValue.getValue();

        //allow the router to be modified programmatically
        Event<Object> event = Arc.container().beanManager().getEvent();

        // First, fire an event with the filter collector
        Filters filters = new Filters();
        event.select(Filters.class).fire(filters);

        filterList.addAll(filters.getFilters());

        // Then, fire the resuming router
        event.select(Router.class).fire(router);

        for (Filter filter : filterList) {
            if (filter.getHandler() != null) {
                // Filters with high priority gets called first.
                router.route().order(-1 * filter.getPriority()).handler(filter.getHandler());
            }
        }

        if (defaultRouteHandler != null) {
            defaultRouteHandler.accept(router.route().order(10_000));
        }

        container.instance(RouterProducer.class).initialize(router);
        router.route().last().failureHandler(new QuarkusErrorHandler(launchMode.isDevOrTest()));

        if (requireBodyHandler) {
            //if this is set then everything needs the body handler installed
            //TODO: config etc
            router.route().order(Integer.MIN_VALUE).handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext routingContext) {
                    routingContext.request().resume();
                    bodyHandler.handle(routingContext);
                }
            });
        }

        if (httpConfiguration.limits.maxBodySize.isPresent()) {
            long limit = httpConfiguration.limits.maxBodySize.get().asLongValue();
            Long limitObj = limit;
            router.route().order(-2).handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    String lengthString = event.request().headers().get(HttpHeaderNames.CONTENT_LENGTH);

                    if (lengthString != null) {
                        long length = Long.parseLong(lengthString);
                        if (length > limit) {
                            event.response().headers().add(HttpHeaderNames.CONNECTION, "close");
                            event.response().setStatusCode(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE.code());
                            event.response().endHandler(new Handler<Void>() {
                                @Override
                                public void handle(Void e) {
                                    event.request().connection().close();
                                }
                            });
                            event.response().end();
                            return;
                        }
                    } else {
                        event.put(MAX_REQUEST_SIZE_KEY, limitObj);
                    }
                    event.next();
                }
            });
        }

        Handler<HttpServerRequest> root;
        if (rootPath.equals("/")) {
            if (hotReplacementHandler != null) {
                router.route().order(-1).handler(hotReplacementHandler);
            }
            root = router;
        } else {
            Router mainRouter = Router.router(vertx.get());
            mainRouter.mountSubRouter(rootPath, router);
            if (hotReplacementHandler != null) {
                mainRouter.route().order(-1).handler(hotReplacementHandler);
            }
            root = mainRouter;
        }

        if (httpConfiguration.proxyAddressForwarding) {
            Handler<HttpServerRequest> delegate = root;
            root = new Handler<HttpServerRequest>() {
                @Override
                public void handle(HttpServerRequest event) {
                    delegate.handle(new ForwardedServerRequestWrapper(event, httpConfiguration.allowForwarded));
                }
            };
        }
        boolean quarkusWrapperNeeded = false;

        if (shutdownConfig.isShutdownTimeoutSet()) {
            gracefulShutdownFilter.next(root);
            root = gracefulShutdownFilter;
            quarkusWrapperNeeded = true;
        }

        AccessLogConfig accessLog = httpConfiguration.accessLog;
        if (accessLog.enabled) {
            AccessLogReceiver receiver;
            if (accessLog.logToFile) {
                File outputDir = accessLog.logDirectory.isPresent() ? new File(accessLog.logDirectory.get()) : new File("");
                receiver = new DefaultAccessLogReceiver(executor, outputDir, accessLog.baseFileName, accessLog.logSuffix,
                        accessLog.rotate);
            } else {
                receiver = new JBossLoggingAccessLogReceiver(accessLog.category);
            }
            AccessLogHandler handler = new AccessLogHandler(receiver, accessLog.pattern, getClass().getClassLoader());
            router.route().order(Integer.MIN_VALUE).handler(handler);
            quarkusWrapperNeeded = true;
        }

        if (quarkusWrapperNeeded) {
            Handler<HttpServerRequest> old = root;
            root = new Handler<HttpServerRequest>() {
                @Override
                public void handle(HttpServerRequest event) {
                    old.handle(new QuarkusRequestWrapper(event));
                }
            };
        }

        Handler<HttpServerRequest> delegate = root;
        root = new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest event) {
                delegate.handle(new ResumingRequestWrapper(event));
            }
        };
        if (httpConfiguration.recordRequestStartTime) {
            router.route().order(Integer.MIN_VALUE).handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    event.put(REQUEST_START_TIME, System.nanoTime());
                    event.next();
                }
            });
        }

        rootHandler = root;
    }

    private static void doServerStart(Vertx vertx, HttpConfiguration httpConfiguration, LaunchMode launchMode,
            Supplier<Integer> eventLoops, String websocketSubProtocols) throws IOException {
        // Http server configuration
        HttpServerOptions httpServerOptions = createHttpServerOptions(httpConfiguration, launchMode, websocketSubProtocols);
        HttpServerOptions domainSocketOptions = createDomainSocketOptions(httpConfiguration, websocketSubProtocols);
        HttpServerOptions sslConfig = createSslOptions(httpConfiguration, launchMode);
        if (httpConfiguration.insecureRequests != HttpConfiguration.InsecureRequests.ENABLED && sslConfig == null) {
            throw new IllegalStateException("Cannot set quarkus.http.redirect-insecure-requests without enabling SSL.");
        }

        int eventLoopCount = eventLoops.get();
        int ioThreads;
        if (httpConfiguration.ioThreads.isPresent()) {
            ioThreads = Math.min(httpConfiguration.ioThreads.getAsInt(), eventLoopCount);
        } else {
            ioThreads = eventLoopCount;
        }
        CompletableFuture<String> futureResult = new CompletableFuture<>();
        vertx.deployVerticle(new Supplier<Verticle>() {
            @Override
            public Verticle get() {
                return new WebDeploymentVerticle(httpServerOptions, sslConfig, domainSocketOptions, launchMode,
                        httpConfiguration.insecureRequests);
            }
        }, new DeploymentOptions().setInstances(ioThreads), new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> event) {
                if (event.failed()) {
                    futureResult.completeExceptionally(event.cause());
                } else {
                    futureResult.complete(event.result());
                }
            }
        });
        try {

            String deploymentId = futureResult.get();
            closeTask = new Runnable() {
                @Override
                public synchronized void run() {
                    //guard against this being run twice
                    if (closeTask == this) {
                        if (vertx.deploymentIDs().contains(deploymentId)) {
                            CountDownLatch latch = new CountDownLatch(1);
                            try {
                                vertx.undeploy(deploymentId, new Handler<AsyncResult<Void>>() {
                                    @Override
                                    public void handle(AsyncResult<Void> event) {
                                        latch.countDown();
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                latch.await();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        closeTask = null;
                    }
                }
            };
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unable to start HTTP server", e);
        }

        setHttpServerTiming(httpConfiguration.insecureRequests, httpServerOptions, sslConfig, domainSocketOptions);
    }

    private static void setHttpServerTiming(InsecureRequests insecureRequests, HttpServerOptions httpServerOptions,
            HttpServerOptions sslConfig,
            HttpServerOptions domainSocketOptions) {
        String serverListeningMessage = "Listening on: ";
        int socketCount = 0;

        if (httpServerOptions != null && !InsecureRequests.DISABLED.equals(insecureRequests)) {
            serverListeningMessage += String.format(
                    "http://%s:%s", httpServerOptions.getHost(), httpServerOptions.getPort());
            socketCount++;
        }

        if (sslConfig != null) {
            if (socketCount > 0) {
                serverListeningMessage += " and ";
            }
            serverListeningMessage += String.format("https://%s:%s", sslConfig.getHost(), sslConfig.getPort());
            socketCount++;
        }

        if (domainSocketOptions != null) {
            if (socketCount > 0) {
                serverListeningMessage += " and ";
            }
            serverListeningMessage += String.format("unix:%s", domainSocketOptions.getHost());
        }
        Timing.setHttpServer(serverListeningMessage);
    }

    /**
     * Get an {@code HttpServerOptions} for this server configuration, or null if SSL should not be enabled
     */
    private static HttpServerOptions createSslOptions(HttpConfiguration httpConfiguration, LaunchMode launchMode)
            throws IOException {
        if (!httpConfiguration.hostEnabled) {
            return null;
        }

        ServerSslConfig sslConfig = httpConfiguration.ssl;
        //TODO: static fields break config
        final Optional<Path> certFile = sslConfig.certificate.file;
        final Optional<Path> keyFile = sslConfig.certificate.keyFile;
        final Optional<Path> keyStoreFile = sslConfig.certificate.keyStoreFile;
        final String keystorePassword = sslConfig.certificate.keyStorePassword;
        final Optional<Path> trustStoreFile = sslConfig.certificate.trustStoreFile;
        final Optional<String> trustStorePassword = sslConfig.certificate.trustStorePassword;
        final HttpServerOptions serverOptions = new HttpServerOptions();

        //ssl
        if (JdkSSLEngineOptions.isAlpnAvailable()) {
            serverOptions.setUseAlpn(httpConfiguration.http2);
            if (httpConfiguration.http2) {
                serverOptions.setAlpnVersions(Arrays.asList(HttpVersion.HTTP_2, HttpVersion.HTTP_1_1));
            }
        }
        serverOptions.setMaxHeaderSize(httpConfiguration.limits.maxHeaderSize.asBigInteger().intValueExact());
        Optional<MemorySize> maxChunkSize = httpConfiguration.limits.maxChunkSize;
        if (maxChunkSize.isPresent()) {
            serverOptions.setMaxChunkSize(maxChunkSize.get().asBigInteger().intValueExact());
        }
        setIdleTimeout(httpConfiguration, serverOptions);

        if (certFile.isPresent() && keyFile.isPresent()) {
            createPemKeyCertOptions(certFile.get(), keyFile.get(), serverOptions);
        } else if (keyStoreFile.isPresent()) {
            final Path keyStorePath = keyStoreFile.get();
            final Optional<String> keyStoreFileType = sslConfig.certificate.keyStoreFileType;
            final String type;
            if (keyStoreFileType.isPresent()) {
                type = keyStoreFileType.get().toLowerCase();
            } else {
                type = findKeystoreFileType(keyStorePath);
            }

            byte[] data = getFileContent(keyStorePath);
            switch (type) {
                case "pkcs12": {
                    PfxOptions options = new PfxOptions()
                            .setPassword(keystorePassword)
                            .setValue(Buffer.buffer(data));
                    serverOptions.setPfxKeyCertOptions(options);
                    break;
                }
                case "jks": {
                    JksOptions options = new JksOptions()
                            .setPassword(keystorePassword)
                            .setValue(Buffer.buffer(data));
                    serverOptions.setKeyStoreOptions(options);
                    break;
                }
                default:
                    throw new IllegalArgumentException(
                            "Unknown keystore type: " + type + " valid types are jks or pkcs12");
            }

        } else {
            return null;
        }

        if (trustStoreFile.isPresent()) {
            if (!trustStorePassword.isPresent()) {
                throw new IllegalArgumentException("No trust store password provided");
            }
            final String type;
            final Optional<String> trustStoreFileType = sslConfig.certificate.trustStoreFileType;
            final Path trustStoreFilePath = trustStoreFile.get();
            if (trustStoreFileType.isPresent()) {
                type = trustStoreFileType.get().toLowerCase();
            } else {
                type = findKeystoreFileType(trustStoreFilePath);
            }
            createTrustStoreOptions(trustStoreFilePath, trustStorePassword.get(), type,
                    serverOptions);
        }

        for (String cipher : sslConfig.cipherSuites.orElse(Collections.emptyList())) {
            serverOptions.addEnabledCipherSuite(cipher);
        }

        for (String protocol : sslConfig.protocols) {
            if (!protocol.isEmpty()) {
                serverOptions.addEnabledSecureTransportProtocol(protocol);
            }
        }
        serverOptions.setSsl(true);
        serverOptions.setHost(httpConfiguration.host);
        serverOptions.setPort(httpConfiguration.determineSslPort(launchMode));
        serverOptions.setClientAuth(sslConfig.clientAuth);
        serverOptions.setReusePort(httpConfiguration.soReusePort);
        serverOptions.setTcpQuickAck(httpConfiguration.tcpQuickAck);
        serverOptions.setTcpCork(httpConfiguration.tcpCork);
        serverOptions.setTcpFastOpen(httpConfiguration.tcpFastOpen);

        return serverOptions;
    }

    private static byte[] getFileContent(Path path) throws IOException {
        byte[] data;
        final InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(path.toString());
        if (resource != null) {
            try (InputStream is = resource) {
                data = doRead(is);
            }
        } else {
            try (InputStream is = Files.newInputStream(path)) {
                data = doRead(is);
            }
        }
        return data;
    }

    private static void createPemKeyCertOptions(Path certFile, Path keyFile,
            HttpServerOptions serverOptions) throws IOException {
        final byte[] cert = getFileContent(certFile);
        final byte[] key = getFileContent(keyFile);
        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
                .setCertValue(Buffer.buffer(cert))
                .setKeyValue(Buffer.buffer(key));
        serverOptions.setPemKeyCertOptions(pemKeyCertOptions);
    }

    private static void createTrustStoreOptions(Path trustStoreFile, String trustStorePassword,
            String trustStoreFileType, HttpServerOptions serverOptions) throws IOException {
        byte[] data = getFileContent(trustStoreFile);
        switch (trustStoreFileType) {
            case "pkcs12": {
                PfxOptions options = new PfxOptions()
                        .setPassword(trustStorePassword)
                        .setValue(Buffer.buffer(data));
                serverOptions.setPfxTrustOptions(options);
                break;
            }
            case "jks": {
                JksOptions options = new JksOptions()
                        .setPassword(trustStorePassword)
                        .setValue(Buffer.buffer(data));
                serverOptions.setTrustStoreOptions(options);
                break;
            }
            default:
                throw new IllegalArgumentException(
                        "Unknown truststore type: " + trustStoreFileType + " valid types are jks or pkcs12");
        }
    }

    private static String findKeystoreFileType(Path storePath) {
        final String pathName = storePath.toString();
        if (pathName.endsWith(".p12") || pathName.endsWith(".pkcs12") || pathName.endsWith(".pfx")) {
            return "pkcs12";
        } else {
            // assume jks
            return "jks";
        }
    }

    private static byte[] doRead(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int r;
        while ((r = is.read(buf)) > 0) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

    private static HttpServerOptions createHttpServerOptions(HttpConfiguration httpConfiguration,
            LaunchMode launchMode, String websocketSubProtocols) {
        if (!httpConfiguration.hostEnabled) {
            return null;
        }
        // TODO other config properties
        HttpServerOptions options = new HttpServerOptions();
        options.setHost(httpConfiguration.host);
        options.setPort(httpConfiguration.determinePort(launchMode));
        setIdleTimeout(httpConfiguration, options);
        options.setMaxHeaderSize(httpConfiguration.limits.maxHeaderSize.asBigInteger().intValueExact());
        Optional<MemorySize> maxChunkSize = httpConfiguration.limits.maxChunkSize;
        if (maxChunkSize.isPresent()) {
            options.setMaxChunkSize(maxChunkSize.get().asBigInteger().intValueExact());
        }
        options.setWebsocketSubProtocols(websocketSubProtocols);
        options.setReusePort(httpConfiguration.soReusePort);
        options.setTcpQuickAck(httpConfiguration.tcpQuickAck);
        options.setTcpCork(httpConfiguration.tcpCork);
        options.setTcpFastOpen(httpConfiguration.tcpFastOpen);
        return options;
    }

    private static HttpServerOptions createDomainSocketOptions(HttpConfiguration httpConfiguration,
            String websocketSubProtocols) {
        if (!httpConfiguration.domainSocketEnabled) {
            return null;
        }
        HttpServerOptions options = new HttpServerOptions();
        options.setHost(httpConfiguration.domainSocket);
        setIdleTimeout(httpConfiguration, options);
        options.setMaxHeaderSize(httpConfiguration.limits.maxHeaderSize.asBigInteger().intValueExact());
        Optional<MemorySize> maxChunkSize = httpConfiguration.limits.maxChunkSize;
        if (maxChunkSize.isPresent()) {
            options.setMaxChunkSize(maxChunkSize.get().asBigInteger().intValueExact());
        }
        options.setWebsocketSubProtocols(websocketSubProtocols);
        return options;
    }

    private static void setIdleTimeout(HttpConfiguration httpConfiguration, HttpServerOptions options) {
        int idleTimeout = (int) httpConfiguration.idleTimeout.toMillis();
        options.setIdleTimeout(idleTimeout);
        options.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
    }

    public void warnIfPortChanged(HttpConfiguration config, int port) {
        if (config.port != port) {
            LOGGER.errorf(
                    "quarkus.http.port was specified at build time as %s however run time value is %s, Kubernetes metadata will be incorrect.",
                    port, config.port);
        }
    }

    public void addRoute(RuntimeValue<Router> router, Function<Router, Route> route, Handler<RoutingContext> handler,
            HandlerType blocking) {

        Route vr = route.apply(router.getValue());

        Handler<RoutingContext> requestHandler = handler;
        if (blocking == HandlerType.BLOCKING) {
            vr.blockingHandler(requestHandler);
        } else if (blocking == HandlerType.FAILURE) {
            vr.failureHandler(requestHandler);
        } else {
            vr.handler(requestHandler);
        }
    }

    public GracefulShutdownFilter createGracefulShutdownHandler() {
        return new GracefulShutdownFilter();
    }

    private static class WebDeploymentVerticle extends AbstractVerticle {

        private HttpServer httpServer;
        private HttpServer httpsServer;
        private HttpServer domainSocketServer;
        private final HttpServerOptions httpOptions;
        private final HttpServerOptions httpsOptions;
        private final HttpServerOptions domainSocketOptions;
        private final LaunchMode launchMode;
        private volatile boolean clearHttpProperty = false;
        private volatile boolean clearHttpsProperty = false;
        private final HttpConfiguration.InsecureRequests insecureRequests;

        public WebDeploymentVerticle(HttpServerOptions httpOptions, HttpServerOptions httpsOptions,
                HttpServerOptions domainSocketOptions, LaunchMode launchMode,
                HttpConfiguration.InsecureRequests insecureRequests) {
            this.httpOptions = httpOptions;
            this.httpsOptions = httpsOptions;
            this.launchMode = launchMode;
            this.domainSocketOptions = domainSocketOptions;
            this.insecureRequests = insecureRequests;
        }

        @Override
        public void start(Future<Void> startFuture) {
            final AtomicInteger remainingCount = new AtomicInteger(0);
            boolean httpServerEnabled = httpOptions != null && insecureRequests != HttpConfiguration.InsecureRequests.DISABLED;
            if (httpServerEnabled) {
                remainingCount.incrementAndGet();
            }
            if (httpsOptions != null) {
                remainingCount.incrementAndGet();
            }
            if (domainSocketOptions != null) {
                remainingCount.incrementAndGet();
            }

            if (remainingCount.get() == 0) {
                startFuture
                        .fail(new IllegalArgumentException("Must configure at least one of http, https or unix domain socket"));
            }

            if (httpServerEnabled) {
                httpServer = vertx.createHttpServer(httpOptions);
                if (insecureRequests == HttpConfiguration.InsecureRequests.ENABLED) {
                    httpServer.requestHandler(ACTUAL_ROOT);
                } else {
                    httpServer.requestHandler(new Handler<HttpServerRequest>() {
                        @Override
                        public void handle(HttpServerRequest req) {
                            try {
                                String host = req.getHeader(HttpHeaderNames.HOST);
                                if (host == null) {
                                    //TODO: solution for HTTP/1.0, but really there is not much we can do
                                    req.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
                                } else {
                                    int includedPort = host.indexOf(":");
                                    if (includedPort != -1) {
                                        host = host.substring(0, includedPort);
                                    }
                                    req.response()
                                            .setStatusCode(301)
                                            .putHeader("Location",
                                                    "https://" + host + ":" + httpsOptions.getPort() + req.uri())
                                            .end();
                                }
                            } catch (Exception e) {
                                req.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                            }
                        }
                    });
                }
                setupTcpHttpServer(httpServer, httpOptions, false, startFuture, remainingCount);
            }

            if (domainSocketOptions != null) {
                domainSocketServer = vertx.createHttpServer(domainSocketOptions);
                domainSocketServer.requestHandler(ACTUAL_ROOT);
                setupUnixDomainSocketHttpServer(domainSocketServer, domainSocketOptions, startFuture, remainingCount);
            }

            if (httpsOptions != null) {
                httpsServer = vertx.createHttpServer(httpsOptions);
                httpsServer.requestHandler(ACTUAL_ROOT);
                setupTcpHttpServer(httpsServer, httpsOptions, true, startFuture, remainingCount);
            }
        }

        private void setupUnixDomainSocketHttpServer(HttpServer httpServer, HttpServerOptions options, Future<Void> startFuture,
                AtomicInteger remainingCount) {
            httpServer.listen(SocketAddress.domainSocketAddress(options.getHost()), event -> {
                if (event.succeeded()) {
                    if (remainingCount.decrementAndGet() == 0) {
                        startFuture.complete(null);
                    }
                } else {
                    startFuture.fail(event.cause());
                }
            });
        }

        private void setupTcpHttpServer(HttpServer httpServer, HttpServerOptions options, boolean https,
                Future<Void> startFuture, AtomicInteger remainingCount) {
            httpServer.listen(options.getPort(), options.getHost(), event -> {
                if (event.cause() != null) {
                    startFuture.fail(event.cause());
                } else {
                    // Port may be random, so set the actual port
                    int actualPort = event.result().actualPort();
                    if (actualPort != options.getPort()) {
                        // Override quarkus.http(s)?.(test-)?port
                        String schema;
                        if (https) {
                            clearHttpsProperty = true;
                            schema = "https";
                        } else {
                            clearHttpProperty = true;
                            schema = "http";
                        }
                        System.setProperty(
                                launchMode == LaunchMode.TEST ? "quarkus." + schema + ".test-port"
                                        : "quarkus." + schema + ".port",
                                String.valueOf(actualPort));
                        // Set in HttpOptions to output the port in the Timing class
                        options.setPort(actualPort);
                    }
                    if (remainingCount.decrementAndGet() == 0) {
                        startFuture.complete(null);
                    }
                }
            });
        }

        @Override
        public void stop(Future<Void> stopFuture) {
            if (clearHttpProperty) {
                System.clearProperty(launchMode == LaunchMode.TEST ? "quarkus.http.test-port" : "quarkus.http.port");
            }
            if (clearHttpsProperty) {
                System.clearProperty(launchMode == LaunchMode.TEST ? "quarkus.https.test-port" : "quarkus.https.port");
            }

            final AtomicInteger remainingCount = new AtomicInteger(0);
            if (httpServer != null) {
                remainingCount.incrementAndGet();
            }
            if (httpsServer != null) {
                remainingCount.incrementAndGet();
            }
            if (domainSocketServer != null) {
                remainingCount.incrementAndGet();
            }

            Handler<AsyncResult<Void>> handleClose = event -> {
                if (remainingCount.decrementAndGet() == 0) {
                    stopFuture.complete();
                }
            };

            if (httpServer != null) {
                httpServer.close(handleClose);
            }
            if (httpsServer != null) {
                httpsServer.close(handleClose);
            }
            if (domainSocketServer != null) {
                domainSocketServer.close(handleClose);
            }
        }
    }

    protected static ServerBootstrap virtualBootstrap;
    public static VirtualAddress VIRTUAL_HTTP = new VirtualAddress("netty-virtual-http");

    private static void initializeVirtual(Vertx vertxRuntime) {
        if (virtualBootstrap != null) {
            return;
        }
        VertxInternal vertx = (VertxInternal) vertxRuntime;
        virtualBootstrap = new ServerBootstrap();

        virtualBootstrap.group(vertx.getEventLoopGroup())
                .channel(VirtualServerChannel.class)
                .handler(new ChannelInitializer<VirtualServerChannel>() {
                    @Override
                    public void initChannel(VirtualServerChannel ch) throws Exception {
                        //ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                    }
                })
                .childHandler(new ChannelInitializer<VirtualChannel>() {
                    @Override
                    public void initChannel(VirtualChannel ch) throws Exception {
                        ContextInternal context = (ContextInternal) vertx
                                .createEventLoopContext(null, null, new JsonObject(),
                                        Thread.currentThread().getContextClassLoader());
                        VertxHandler<Http1xServerConnection> handler = VertxHandler.create(context, chctx -> {
                            Http1xServerConnection conn = new Http1xServerConnection(
                                    context.owner(),
                                    null,
                                    new HttpServerOptions(),
                                    chctx,
                                    context,
                                    "localhost",
                                    null);
                            conn.handler(ACTUAL_ROOT);
                            return conn;
                        });

                        ch.pipeline().addLast("handler", handler);
                    }
                });

        // Start the server.
        try {
            virtualBootstrap.bind(VIRTUAL_HTTP).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("failed to bind virtual http");
        }

    }

    public static Handler<HttpServerRequest> getRootHandler() {
        return ACTUAL_ROOT;
    }

    public Handler<RoutingContext> createBodyHandler(HttpConfiguration httpConfiguration) {
        BodyHandler bodyHandler = BodyHandler.create();
        Optional<MemorySize> maxBodySize = httpConfiguration.limits.maxBodySize;
        if (maxBodySize.isPresent()) {
            bodyHandler.setBodyLimit(maxBodySize.get().asLongValue());
        }
        final BodyConfig bodyConfig = httpConfiguration.body;
        bodyHandler.setHandleFileUploads(bodyConfig.handleFileUploads);
        bodyHandler.setUploadsDirectory(bodyConfig.uploadsDirectory);
        bodyHandler.setDeleteUploadedFilesOnEnd(bodyConfig.deleteUploadedFilesOnEnd);
        bodyHandler.setMergeFormAttributes(bodyConfig.mergeFormAttributes);
        bodyHandler.setPreallocateBodyBuffer(bodyConfig.preallocateBodyBuffer);
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (!Context.isOnEventLoopThread()) {
                    ((ConnectionBase) event.request().connection()).channel().eventLoop().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //this can happen if blocking authentication is involved for get requests
                                if (!event.request().isEnded()) {
                                    event.request().resume();
                                    if (CAN_HAVE_BODY.contains(event.request().method())) {
                                        bodyHandler.handle(event);
                                    } else {
                                        event.next();
                                    }
                                } else {
                                    event.next();
                                }
                            } catch (Throwable t) {
                                event.fail(t);
                            }
                        }
                    });
                } else {
                    event.request().resume();
                    if (CAN_HAVE_BODY.contains(event.request().method())) {
                        bodyHandler.handle(event);
                    } else {
                        event.next();
                    }
                }
            }
        };
    }

    private static final List<HttpMethod> CAN_HAVE_BODY = Arrays.asList(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH,
            HttpMethod.DELETE);
}
