package io.quarkus.vertx.http.runtime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.enterprise.event.Event;

import org.jboss.logging.Logger;
import org.wildfly.common.cpu.ProcessorInfo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.netty.runtime.virtual.VirtualAddress;
import io.quarkus.netty.runtime.virtual.VirtualChannel;
import io.quarkus.netty.runtime.virtual.VirtualServerChannel;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.LiveReloadConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigInstantiator;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.shutdown.ShutdownConfig;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.quarkus.vertx.http.runtime.HttpConfiguration.InsecureRequests;
import io.quarkus.vertx.http.runtime.devmode.RemoteSyncHandler;
import io.quarkus.vertx.http.runtime.devmode.VertxHttpHotReplacementSetup;
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
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.impl.Http1xServerConnection;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.core.net.PemKeyCertOptions;
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

    // We do not use Integer.MAX on purpose to allow advanced users to register a route AFTER the default route
    public static final int DEFAULT_ROUTE_ORDER = 10_000;

    private static final Logger LOGGER = Logger.getLogger(VertxHttpRecorder.class.getName());

    private static volatile Handler<RoutingContext> hotReplacementHandler;
    private static volatile HotReplacementContext hotReplacementContext;
    private static volatile RemoteSyncHandler remoteSyncHandler;

    private static volatile Runnable closeTask;

    static volatile Handler<HttpServerRequest> rootHandler;

    private static volatile Handler<RoutingContext> nonApplicationRedirectHandler;

    private static volatile int actualHttpPort = -1;
    private static volatile int actualHttpsPort = -1;

    public static final String GET = "GET";
    private static final Handler<HttpServerRequest> ACTUAL_ROOT = new Handler<HttpServerRequest>() {
        @Override
        public void handle(HttpServerRequest httpServerRequest) {
            try {
                // we simply need to know if the URI is valid
                new URI(httpServerRequest.uri());
            } catch (URISyntaxException e) {
                httpServerRequest.response().setStatusCode(400).end();
                return;
            }
            //we need to pause the request to make sure that data does
            //not arrive before handlers have a chance to install a read handler
            //as it is possible filters such as the auth filter can do blocking tasks
            //as the underlying handler has not had a chance to install a read handler yet
            //and data that arrives while the blocking task is being processed will be lost
            httpServerRequest.pause();
            Handler<HttpServerRequest> rh = VertxHttpRecorder.rootHandler;
            if (rh != null) {
                rh.handle(httpServerRequest);
            } else {
                //very rare race condition, that can happen when dev mode is shutting down
                httpServerRequest.resume();
                httpServerRequest.response().setStatusCode(503).end();
            }
        }
    };
    final HttpBuildTimeConfig httpBuildTimeConfig;
    final RuntimeValue<HttpConfiguration> httpConfiguration;

    public VertxHttpRecorder(HttpBuildTimeConfig httpBuildTimeConfig, RuntimeValue<HttpConfiguration> httpConfiguration) {
        this.httpBuildTimeConfig = httpBuildTimeConfig;
        this.httpConfiguration = httpConfiguration;
    }

    public static void setHotReplacement(Handler<RoutingContext> handler, HotReplacementContext hrc) {
        hotReplacementHandler = handler;
        hotReplacementContext = hrc;
    }

    public static void shutDownDevMode() {
        if (closeTask != null) {
            closeTask.run();
            closeTask = null;
        }
        rootHandler = null;
        hotReplacementHandler = null;

    }

    public static void startServerAfterFailedStart() {
        if (closeTask != null) {
            //it is possible start failed after the server was started
            //we shut it down in this case, as we have no idea what state it is in
            final Handler<RoutingContext> prevHotReplacementHandler = hotReplacementHandler;
            shutDownDevMode();
            // reset back to the older live reload handler, so that it can be used
            // to watch any artifacts that need hot deployment to fix the reason which caused
            // the server start to fail
            hotReplacementHandler = prevHotReplacementHandler;
        }
        Supplier<Vertx> supplier = VertxCoreRecorder.getVertx();
        Vertx vertx;
        if (supplier == null) {
            VertxConfiguration vertxConfiguration = new VertxConfiguration();
            ConfigInstantiator.handleObject(vertxConfiguration);
            vertx = VertxCoreRecorder.recoverFailedStart(vertxConfiguration).get();
        } else {
            vertx = supplier.get();
        }

        try {
            HttpBuildTimeConfig buildConfig = new HttpBuildTimeConfig();
            ConfigInstantiator.handleObject(buildConfig);
            HttpConfiguration config = new HttpConfiguration();
            ConfigInstantiator.handleObject(config);
            if (config.host == null) {
                //HttpHostConfigSource does not come into play here
                config.host = "localhost";
            }
            Router router = Router.router(vertx);
            if (hotReplacementHandler != null) {
                router.route().order(Integer.MIN_VALUE).blockingHandler(hotReplacementHandler);
            }

            Handler<HttpServerRequest> root = router;
            LiveReloadConfig liveReloadConfig = new LiveReloadConfig();
            ConfigInstantiator.handleObject(liveReloadConfig);
            if (liveReloadConfig.password.isPresent()
                    && hotReplacementContext.getDevModeType() == DevModeType.REMOTE_SERVER_SIDE) {
                root = remoteSyncHandler = new RemoteSyncHandler(liveReloadConfig.password.get(), root, hotReplacementContext);
            }
            rootHandler = root;

            //we can't really do
            doServerStart(vertx, buildConfig, config, LaunchMode.DEVELOPMENT, new Supplier<Integer>() {
                @Override
                public Integer get() {
                    return ProcessorInfo.availableProcessors() * 2; //this is dev mode, so the number of IO threads not always being 100% correct does not really matter in this case
                }
            }, null, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RuntimeValue<Router> initializeRouter(final Supplier<Vertx> vertxRuntimeValue) {
        Vertx vertx = vertxRuntimeValue.get();
        Router router = Router.router(vertx);
        return new RuntimeValue<>(router);
    }

    public RuntimeValue<io.vertx.mutiny.ext.web.Router> createMutinyRouter(final RuntimeValue<Router> router) {
        return new RuntimeValue<>(new io.vertx.mutiny.ext.web.Router(router.getValue()));
    }

    public void startServer(Supplier<Vertx> vertx, ShutdownContext shutdown,
            LaunchMode launchMode,
            boolean startVirtual, boolean startSocket, Supplier<Integer> ioThreads, List<String> websocketSubProtocols,
            boolean auxiliaryApplication)
            throws IOException {

        if (startVirtual) {
            initializeVirtual(vertx.get());
        }
        HttpConfiguration httpConfiguration = this.httpConfiguration.getValue();
        if (startSocket && (httpConfiguration.hostEnabled || httpConfiguration.domainSocketEnabled)) {
            // Start the server
            if (closeTask == null) {
                doServerStart(vertx.get(), httpBuildTimeConfig, httpConfiguration, launchMode, ioThreads,
                        websocketSubProtocols, auxiliaryApplication);
                if (launchMode != LaunchMode.DEVELOPMENT) {
                    shutdown.addShutdownTask(closeTask);
                } else {
                    shutdown.addShutdownTask(new Runnable() {
                        @Override
                        public void run() {
                            VertxHttpHotReplacementSetup.handleDevModeRestart();
                        }
                    });
                }
            }
        }
    }

    public void mountFrameworkRouter(RuntimeValue<Router> mainRouter, RuntimeValue<Router> frameworkRouter,
            String frameworkPath) {
        mainRouter.getValue().mountSubRouter(frameworkPath, frameworkRouter.getValue());
    }

    public void finalizeRouter(BeanContainer container, Consumer<Route> defaultRouteHandler,
            List<Filter> filterList, Supplier<Vertx> vertx,
            LiveReloadConfig liveReloadConfig, Optional<RuntimeValue<Router>> mainRouterRuntimeValue,
            RuntimeValue<Router> httpRouterRuntimeValue, RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter,
            String rootPath, LaunchMode launchMode, boolean requireBodyHandler,
            Handler<RoutingContext> bodyHandler,
            GracefulShutdownFilter gracefulShutdownFilter, ShutdownConfig shutdownConfig,
            Executor executor) {
        HttpConfiguration httpConfiguration = this.httpConfiguration.getValue();
        // install the default route at the end
        Router httpRouteRouter = httpRouterRuntimeValue.getValue();

        //allow the router to be modified programmatically
        Event<Object> event = Arc.container().beanManager().getEvent();

        // First, fire an event with the filter collector
        Filters filters = new Filters();
        event.select(Filters.class).fire(filters);

        filterList.addAll(filters.getFilters());

        // Then, fire the resuming router
        event.select(Router.class).fire(httpRouteRouter);
        // Also fires the Mutiny one
        event.select(io.vertx.mutiny.ext.web.Router.class).fire(mutinyRouter.getValue());

        for (Filter filter : filterList) {
            if (filter.getHandler() != null) {
                // Filters with high priority gets called first.
                httpRouteRouter.route().order(-1 * filter.getPriority()).handler(filter.getHandler());
            }
        }

        if (defaultRouteHandler != null) {
            defaultRouteHandler.accept(httpRouteRouter.route().order(DEFAULT_ROUTE_ORDER));
        }

        httpRouteRouter.route().last().failureHandler(
                new QuarkusErrorHandler(launchMode.isDevOrTest(), httpConfiguration.unhandledErrorContentTypeDefault));

        if (requireBodyHandler) {
            //if this is set then everything needs the body handler installed
            //TODO: config etc
            httpRouteRouter.route().order(Integer.MIN_VALUE + 1).handler(new Handler<RoutingContext>() {
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
            httpRouteRouter.route().order(-2).handler(new Handler<RoutingContext>() {
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
                //recorders are always executed in the current CL
                ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
                httpRouteRouter.route().order(Integer.MIN_VALUE).handler(new Handler<RoutingContext>() {
                    @Override
                    public void handle(RoutingContext event) {
                        Thread.currentThread().setContextClassLoader(currentCl);
                        hotReplacementHandler.handle(event);
                    }
                });
            }
            root = httpRouteRouter;
        } else {
            Router mainRouter = mainRouterRuntimeValue.isPresent() ? mainRouterRuntimeValue.get().getValue()
                    : Router.router(vertx.get());
            mainRouter.mountSubRouter(rootPath, httpRouteRouter);
            if (hotReplacementHandler != null) {
                ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
                mainRouter.route().order(Integer.MIN_VALUE).handler(new Handler<RoutingContext>() {
                    @Override
                    public void handle(RoutingContext event) {
                        Thread.currentThread().setContextClassLoader(currentCl);
                        hotReplacementHandler.handle(event);
                    }
                });
            }
            root = mainRouter;
        }

        warnIfDeprecatedHttpConfigPropertiesPresent(httpConfiguration);
        ForwardingProxyOptions forwardingProxyOptions = ForwardingProxyOptions.from(httpConfiguration);
        if (forwardingProxyOptions.proxyAddressForwarding) {
            Handler<HttpServerRequest> delegate = root;
            root = new Handler<HttpServerRequest>() {
                @Override
                public void handle(HttpServerRequest event) {
                    delegate.handle(new ForwardedServerRequestWrapper(event, forwardingProxyOptions));
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
            AccessLogHandler handler = new AccessLogHandler(receiver, accessLog.pattern, getClass().getClassLoader(),
                    accessLog.excludePattern);
            httpRouteRouter.route().order(Integer.MIN_VALUE).handler(handler);
            quarkusWrapperNeeded = true;
        }

        BiConsumer<Cookie, HttpServerRequest> cookieFunction = null;
        if (!httpConfiguration.sameSiteCookie.isEmpty()) {
            cookieFunction = processSameSiteConfig(httpConfiguration.sameSiteCookie);
            quarkusWrapperNeeded = true;
        }
        BiConsumer<Cookie, HttpServerRequest> cookieConsumer = cookieFunction;

        if (quarkusWrapperNeeded) {
            Handler<HttpServerRequest> old = root;
            root = new Handler<HttpServerRequest>() {
                @Override
                public void handle(HttpServerRequest event) {
                    old.handle(new QuarkusRequestWrapper(event, cookieConsumer));
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
            httpRouteRouter.route().order(Integer.MIN_VALUE).handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    event.put(REQUEST_START_TIME, System.nanoTime());
                    event.next();
                }
            });
        }
        if (launchMode == LaunchMode.DEVELOPMENT && liveReloadConfig.password.isPresent()
                && hotReplacementContext.getDevModeType() == DevModeType.REMOTE_SERVER_SIDE) {
            root = remoteSyncHandler = new RemoteSyncHandler(liveReloadConfig.password.get(), root, hotReplacementContext);
        }
        rootHandler = root;
    }

    private void warnIfDeprecatedHttpConfigPropertiesPresent(HttpConfiguration httpConfiguration) {
        if (httpConfiguration.proxyAddressForwarding.isPresent()) {
            LOGGER.warn(
                    "`quarkus.http.proxy-address-forwarding` is deprecated and will be removed in a future version - it is "
                            + "recommended to switch to `quarkus.http.proxy.proxy-address-forwarding`");
        }

        if (httpConfiguration.allowForwarded.isPresent()) {
            LOGGER.warn(
                    "`quarkus.http.allow-forwarded` is deprecated and will be removed in a future version - it is "
                            + "recommended to switch to `quarkus.http.proxy.allow-forwarded`");
        }
    }

    private static void doServerStart(Vertx vertx, HttpBuildTimeConfig httpBuildTimeConfig,
            HttpConfiguration httpConfiguration, LaunchMode launchMode,
            Supplier<Integer> eventLoops, List<String> websocketSubProtocols, boolean auxiliaryApplication) throws IOException {
        // Http server configuration
        HttpServerOptions httpServerOptions = createHttpServerOptions(httpConfiguration, launchMode, websocketSubProtocols);
        HttpServerOptions domainSocketOptions = createDomainSocketOptions(httpConfiguration, websocketSubProtocols);
        HttpServerOptions sslConfig = createSslOptions(httpBuildTimeConfig, httpConfiguration, launchMode);

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
        AtomicInteger connectionCount = new AtomicInteger();
        vertx.deployVerticle(new Supplier<Verticle>() {
            @Override
            public Verticle get() {
                return new WebDeploymentVerticle(httpServerOptions, sslConfig, domainSocketOptions, launchMode,
                        httpConfiguration.insecureRequests, httpConfiguration, connectionCount);
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
            VertxCoreRecorder.setWebDeploymentId(deploymentId);
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
                        if (remoteSyncHandler != null) {
                            remoteSyncHandler.close();
                            remoteSyncHandler = null;
                        }
                    }
                }
            };
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unable to start HTTP server", e);
        }

        setHttpServerTiming(httpConfiguration.insecureRequests, httpServerOptions, sslConfig, domainSocketOptions,
                auxiliaryApplication);
    }

    private static void setHttpServerTiming(InsecureRequests insecureRequests, HttpServerOptions httpServerOptions,
            HttpServerOptions sslConfig,
            HttpServerOptions domainSocketOptions, boolean auxiliaryApplication) {
        String serverListeningMessage = "Listening on: ";
        int socketCount = 0;

        if (httpServerOptions != null && !InsecureRequests.DISABLED.equals(insecureRequests)) {
            serverListeningMessage += String.format(
                    "http://%s:%s", httpServerOptions.getHost(), actualHttpPort);
            socketCount++;
        }

        if (sslConfig != null) {
            if (socketCount > 0) {
                serverListeningMessage += " and ";
            }
            serverListeningMessage += String.format("https://%s:%s", sslConfig.getHost(), actualHttpsPort);
            socketCount++;
        }

        if (domainSocketOptions != null) {
            if (socketCount > 0) {
                serverListeningMessage += " and ";
            }
            serverListeningMessage += String.format("unix:%s", domainSocketOptions.getHost());
        }
        Timing.setHttpServer(serverListeningMessage, auxiliaryApplication);
    }

    /**
     * Get an {@code HttpServerOptions} for this server configuration, or null if SSL should not be enabled
     */
    private static HttpServerOptions createSslOptions(HttpBuildTimeConfig buildTimeConfig, HttpConfiguration httpConfiguration,
            LaunchMode launchMode)
            throws IOException {
        if (!httpConfiguration.hostEnabled) {
            return null;
        }

        ServerSslConfig sslConfig = httpConfiguration.ssl;

        final Optional<Path> certFile = sslConfig.certificate.file;
        final Optional<Path> keyFile = sslConfig.certificate.keyFile;
        final List<Path> keys = new ArrayList<>();
        final List<Path> certificates = new ArrayList<>();
        if (sslConfig.certificate.keyFiles.isPresent()) {
            keys.addAll(sslConfig.certificate.keyFiles.get());
        }
        if (sslConfig.certificate.files.isPresent()) {
            certificates.addAll(sslConfig.certificate.files.get());
        }
        if (keyFile.isPresent()) {
            keys.add(keyFile.get());
        }
        if (certFile.isPresent()) {
            certificates.add(certFile.get());
        }

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
        serverOptions.setMaxChunkSize(httpConfiguration.limits.maxChunkSize.asBigInteger().intValueExact());
        serverOptions.setMaxFormAttributeSize(httpConfiguration.limits.maxFormAttributeSize.asBigInteger().intValueExact());
        setIdleTimeout(httpConfiguration, serverOptions);

        if (!certificates.isEmpty() && !keys.isEmpty()) {
            createPemKeyCertOptions(certificates, keys, serverOptions);
        } else if (keyStoreFile.isPresent()) {
            KeyStoreOptions options = createKeyStoreOptions(
                    keyStoreFile.get(),
                    keystorePassword,
                    sslConfig.certificate.keyStoreFileType,
                    sslConfig.certificate.keyStoreProvider,
                    sslConfig.certificate.keyStoreKeyAlias,
                    sslConfig.certificate.keyStoreKeyPassword);
            serverOptions.setKeyCertOptions(options);
        } else {
            return null;
        }

        if (trustStoreFile.isPresent()) {
            if (!trustStorePassword.isPresent()) {
                throw new IllegalArgumentException("No trust store password provided");
            }
            KeyStoreOptions options = createKeyStoreOptions(
                    trustStoreFile.get(),
                    trustStorePassword.get(),
                    sslConfig.certificate.trustStoreFileType,
                    sslConfig.certificate.trustStoreProvider,
                    sslConfig.certificate.trustStoreCertAlias,
                    Optional.empty());
            serverOptions.setTrustOptions(options);
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
        serverOptions.setSni(sslConfig.sni);
        serverOptions.setHost(httpConfiguration.host);
        serverOptions.setPort(httpConfiguration.determineSslPort(launchMode));
        serverOptions.setClientAuth(buildTimeConfig.tlsClientAuth);
        serverOptions.setReusePort(httpConfiguration.soReusePort);
        serverOptions.setTcpQuickAck(httpConfiguration.tcpQuickAck);
        serverOptions.setTcpCork(httpConfiguration.tcpCork);
        serverOptions.setTcpFastOpen(httpConfiguration.tcpFastOpen);
        serverOptions.setMaxInitialLineLength(httpConfiguration.limits.maxInitialLineLength);

        return serverOptions;
    }

    private static KeyStoreOptions createKeyStoreOptions(Path path, String password, Optional<String> fileType,
            Optional<String> provider, Optional<String> alias, Optional<String> aliasPassword) throws IOException {
        final String type;
        if (fileType.isPresent()) {
            type = fileType.get().toLowerCase();
        } else {
            type = findKeystoreFileType(path);
        }

        byte[] data = getFileContent(path);
        KeyStoreOptions options = new KeyStoreOptions()
                .setPassword(password)
                .setValue(Buffer.buffer(data))
                .setType(type.toUpperCase())
                .setProvider(provider.orElse(null))
                .setAlias(alias.orElse(null))
                .setAliasPassword(aliasPassword.orElse(null));
        return options;
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

    private static void createPemKeyCertOptions(List<Path> certFile, List<Path> keyFile,
            HttpServerOptions serverOptions) throws IOException {

        if (certFile.size() != keyFile.size()) {
            throw new ConfigurationException("Invalid certificate configuration - `files` and `keyFiles` must have the "
                    + "same number of elements");
        }

        List<Buffer> certificates = new ArrayList<>();
        List<Buffer> keys = new ArrayList<>();

        for (Path p : certFile) {
            final byte[] cert = getFileContent(p);
            certificates.add(Buffer.buffer(cert));
        }

        for (Path p : keyFile) {
            final byte[] key = getFileContent(p);
            keys.add(Buffer.buffer(key));
        }

        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
                .setCertValues(certificates)
                .setKeyValues(keys);
        serverOptions.setPemKeyCertOptions(pemKeyCertOptions);
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
            LaunchMode launchMode, List<String> websocketSubProtocols) {
        if (!httpConfiguration.hostEnabled) {
            return null;
        }
        // TODO other config properties
        HttpServerOptions options = new HttpServerOptions();
        options.setHost(httpConfiguration.host);
        options.setPort(httpConfiguration.determinePort(launchMode));
        setIdleTimeout(httpConfiguration, options);
        options.setMaxHeaderSize(httpConfiguration.limits.maxHeaderSize.asBigInteger().intValueExact());
        options.setMaxChunkSize(httpConfiguration.limits.maxChunkSize.asBigInteger().intValueExact());
        options.setMaxFormAttributeSize(httpConfiguration.limits.maxFormAttributeSize.asBigInteger().intValueExact());
        options.setWebSocketSubProtocols(websocketSubProtocols);
        options.setReusePort(httpConfiguration.soReusePort);
        options.setTcpQuickAck(httpConfiguration.tcpQuickAck);
        options.setTcpCork(httpConfiguration.tcpCork);
        options.setTcpFastOpen(httpConfiguration.tcpFastOpen);
        options.setCompressionSupported(httpConfiguration.enableCompression);
        options.setDecompressionSupported(httpConfiguration.enableDecompression);
        options.setMaxInitialLineLength(httpConfiguration.limits.maxInitialLineLength);
        return options;
    }

    private static HttpServerOptions createDomainSocketOptions(HttpConfiguration httpConfiguration,
            List<String> websocketSubProtocols) {
        if (!httpConfiguration.domainSocketEnabled) {
            return null;
        }
        HttpServerOptions options = new HttpServerOptions();
        options.setHost(httpConfiguration.domainSocket);
        setIdleTimeout(httpConfiguration, options);
        options.setMaxHeaderSize(httpConfiguration.limits.maxHeaderSize.asBigInteger().intValueExact());
        options.setMaxChunkSize(httpConfiguration.limits.maxChunkSize.asBigInteger().intValueExact());
        options.setMaxFormAttributeSize(httpConfiguration.limits.maxFormAttributeSize.asBigInteger().intValueExact());
        options.setWebSocketSubProtocols(websocketSubProtocols);
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

        if (blocking == HandlerType.BLOCKING) {
            vr.blockingHandler(handler, false);
        } else if (blocking == HandlerType.FAILURE) {
            vr.failureHandler(handler);
        } else {
            vr.handler(handler);
        }

    }

    public void setNonApplicationRedirectHandler(String nonApplicationPath, String rootPath) {
        nonApplicationRedirectHandler = new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext context) {
                String absoluteURI = context.request().path();
                String target = absoluteURI.substring(rootPath.length());
                String redirectTo = nonApplicationPath + target;

                String query = context.request().query();
                if (query != null && !query.isEmpty()) {
                    redirectTo += '?' + query;
                }

                context.response()
                        .setStatusCode(HttpResponseStatus.MOVED_PERMANENTLY.code())
                        .putHeader(HttpHeaderNames.LOCATION, redirectTo)
                        .end();
            }
        };
    }

    public Handler<RoutingContext> getNonApplicationRedirectHandler() {
        return nonApplicationRedirectHandler;
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
        private volatile Map<String, String> portPropertiesToRestore;
        private final HttpConfiguration.InsecureRequests insecureRequests;
        private final HttpConfiguration quarkusConfig;
        private final AtomicInteger connectionCount;

        public WebDeploymentVerticle(HttpServerOptions httpOptions, HttpServerOptions httpsOptions,
                HttpServerOptions domainSocketOptions, LaunchMode launchMode,
                InsecureRequests insecureRequests, HttpConfiguration quarkusConfig, AtomicInteger connectionCount) {
            this.httpOptions = httpOptions;
            this.httpsOptions = httpsOptions;
            this.launchMode = launchMode;
            this.domainSocketOptions = domainSocketOptions;
            this.insecureRequests = insecureRequests;
            this.quarkusConfig = quarkusConfig;
            this.connectionCount = connectionCount;
        }

        @Override
        public void start(Promise<Void> startFuture) {
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
                setupTcpHttpServer(httpServer, httpOptions, false, startFuture, remainingCount, connectionCount);
            }

            if (domainSocketOptions != null) {
                domainSocketServer = vertx.createHttpServer(domainSocketOptions);
                domainSocketServer.requestHandler(ACTUAL_ROOT);
                setupUnixDomainSocketHttpServer(domainSocketServer, domainSocketOptions, startFuture, remainingCount);
            }

            if (httpsOptions != null) {
                httpsServer = vertx.createHttpServer(httpsOptions);
                httpsServer.requestHandler(ACTUAL_ROOT);
                setupTcpHttpServer(httpsServer, httpsOptions, true, startFuture, remainingCount, connectionCount);
            }
        }

        private void setupUnixDomainSocketHttpServer(HttpServer httpServer, HttpServerOptions options,
                Promise<Void> startFuture,
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
                Promise<Void> startFuture, AtomicInteger remainingCount, AtomicInteger currentConnectionCount) {
            if (quarkusConfig.limits.maxConnections.isPresent() && quarkusConfig.limits.maxConnections.getAsInt() > 0) {
                final int maxConnections = quarkusConfig.limits.maxConnections.getAsInt();
                httpServer.connectionHandler(new Handler<HttpConnection>() {

                    @Override
                    public void handle(HttpConnection event) {
                        int current;
                        do {
                            current = currentConnectionCount.get();
                            if (current == maxConnections) {
                                //just close the connection
                                LOGGER.debug("Rejecting connection as there are too many active connections");
                                event.close();
                                return;
                            }
                        } while (!currentConnectionCount.compareAndSet(current, current + 1));
                        event.closeHandler(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                LOGGER.debug("Connection closed");
                                connectionCount.decrementAndGet();
                            }
                        });
                    }
                });
            }
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
                            actualHttpPort = actualPort;
                            schema = "http";
                        }
                        portPropertiesToRestore = new HashMap<>();
                        String portPropertyValue = String.valueOf(actualPort);
                        //we always set the .port property, even if we are in test mode, so this will always
                        //reflect the current port
                        String portPropertyName = "quarkus." + schema + ".port";
                        String prevPortPropertyValue = System.setProperty(portPropertyName, portPropertyValue);
                        if (!Objects.equals(prevPortPropertyValue, portPropertyValue)) {
                            portPropertiesToRestore.put(portPropertyName, prevPortPropertyValue);
                        }
                        if (launchMode == LaunchMode.TEST) {
                            //we also set the test-port property in a test
                            String testPropName = "quarkus." + schema + ".test-port";
                            String prevTestPropPrevValue = System.setProperty(testPropName, portPropertyValue);
                            if (!Objects.equals(prevTestPropPrevValue, portPropertyValue)) {
                                portPropertiesToRestore.put(testPropName, prevTestPropPrevValue);
                            }
                        }
                        if (launchMode.isDevOrTest()) {
                            // set the profile property as well to make sure we don't have any inconsistencies
                            portPropertyName = propertyWithProfilePrefix(portPropertyName);
                            prevPortPropertyValue = System.setProperty(portPropertyName, portPropertyValue);
                            if (!Objects.equals(prevPortPropertyValue, portPropertyValue)) {
                                portPropertiesToRestore.put(portPropertyName, prevPortPropertyValue);
                            }
                        }
                    }
                    if (https) {
                        actualHttpsPort = actualPort;
                    } else {
                        actualHttpPort = actualPort;
                    }
                    if (remainingCount.decrementAndGet() == 0) {
                        startFuture.complete(null);
                    }

                }
            });
        }

        @Override
        public void stop(Promise<Void> stopFuture) {
            if (clearHttpProperty) {
                String portPropertyName = launchMode == LaunchMode.TEST ? "quarkus.http.test-port" : "quarkus.http.port";
                System.clearProperty(portPropertyName);
                if (launchMode.isDevOrTest()) {
                    System.clearProperty(propertyWithProfilePrefix(portPropertyName));
                }

            }
            if (clearHttpsProperty) {
                String portPropertyName = launchMode == LaunchMode.TEST ? "quarkus.https.test-port" : "quarkus.https.port";
                System.clearProperty(portPropertyName);
                if (launchMode.isDevOrTest()) {
                    System.clearProperty(propertyWithProfilePrefix(portPropertyName));
                }
            }
            if (portPropertiesToRestore != null) {
                for (Map.Entry<String, String> entry : portPropertiesToRestore.entrySet()) {
                    if (entry.getValue() == null) {
                        System.clearProperty(entry.getKey());
                    } else {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                }
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

        private String propertyWithProfilePrefix(String portPropertyName) {
            return "%" + launchMode.getDefaultProfile() + "." + portPropertyName;
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
                        EventLoopContext context = vertx.createEventLoopContext();
                        VertxHandler<Http1xServerConnection> handler = VertxHandler.create(chctx -> {

                            Http1xServerConnection conn = new Http1xServerConnection(
                                    () -> context,
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

    /**
     * used in the live reload handler to make sure the application has not been changed by another source (e.g. reactive
     * messaging)
     */
    public static Object getCurrentApplicationState() {
        return rootHandler;
    }

    public Handler<RoutingContext> createBodyHandler() {
        BodyHandler bodyHandler = BodyHandler.create();
        Optional<MemorySize> maxBodySize = httpConfiguration.getValue().limits.maxBodySize;
        if (maxBodySize.isPresent()) {
            bodyHandler.setBodyLimit(maxBodySize.get().asLongValue());
        }
        final BodyConfig bodyConfig = httpConfiguration.getValue().body;
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
                    if (!event.request().isEnded()) {
                        event.request().resume();
                    }
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

    private BiConsumer<Cookie, HttpServerRequest> processSameSiteConfig(Map<String, SameSiteCookieConfig> httpConfiguration) {

        List<BiFunction<Cookie, HttpServerRequest, Boolean>> functions = new ArrayList<>();
        BiFunction<Cookie, HttpServerRequest, Boolean> last = null;

        for (Map.Entry<String, SameSiteCookieConfig> entry : new TreeMap<>(httpConfiguration).entrySet()) {
            Pattern p = Pattern.compile(entry.getKey(), entry.getValue().caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            BiFunction<Cookie, HttpServerRequest, Boolean> biFunction = new BiFunction<Cookie, HttpServerRequest, Boolean>() {
                @Override
                public Boolean apply(Cookie cookie, HttpServerRequest request) {
                    if (p.matcher(cookie.getName()).matches()) {
                        if (entry.getValue().value == CookieSameSite.NONE) {
                            if (entry.getValue().enableClientChecker) {
                                String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
                                if (userAgent != null
                                        && SameSiteNoneIncompatibleClientChecker.isSameSiteNoneIncompatible(userAgent)) {
                                    return false;
                                }
                            }
                            if (entry.getValue().addSecureForNone) {
                                cookie.setSecure(true);
                            }
                        }
                        cookie.setSameSite(entry.getValue().value);
                        return true;
                    }
                    return false;
                }
            };
            if (entry.getKey().equals(".*")) {
                //bit of a hack to make sure the pattern .* is evaluated last
                last = biFunction;
            } else {
                functions.add(biFunction);
            }
        }
        if (last != null) {
            functions.add(last);
        }

        return new BiConsumer<Cookie, HttpServerRequest>() {
            @Override
            public void accept(Cookie cookie, HttpServerRequest request) {
                for (BiFunction<Cookie, HttpServerRequest, Boolean> i : functions) {
                    if (i.apply(cookie, request)) {
                        return;
                    }
                }
            }
        };
    }
}
