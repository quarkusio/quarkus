package io.quarkus.vertx.http.runtime;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;
import static io.quarkus.vertx.http.HttpServer.HTTPS_PORT;
import static io.quarkus.vertx.http.HttpServer.HTTPS_TEST_PORT;
import static io.quarkus.vertx.http.HttpServer.HTTP_PORT;
import static io.quarkus.vertx.http.HttpServer.HTTP_TEST_PORT;
import static io.quarkus.vertx.http.HttpServer.LOCAL_BASE_URI;
import static io.quarkus.vertx.http.HttpServer.LOCAL_MANAGEMENT_BASE_URI;
import static io.quarkus.vertx.http.HttpServer.MANAGEMENT_PORT;
import static io.quarkus.vertx.http.HttpServer.MANAGEMENT_TEST_PORT;
import static io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils.getInsecureRequestStrategy;
import static io.quarkus.vertx.http.runtime.options.HttpServerTlsConfig.getHttpServerTlsConfigName;
import static io.quarkus.vertx.http.runtime.options.HttpServerTlsConfig.getTlsClientAuth;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.CDI;

import org.crac.Resource;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.bootstrap.runner.CracSupport;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.netty.runtime.virtual.VirtualAddress;
import io.quarkus.netty.runtime.virtual.VirtualChannel;
import io.quarkus.netty.runtime.virtual.VirtualServerChannel;
import io.quarkus.runtime.ErrorPageAction;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.LiveReloadConfig;
import io.quarkus.runtime.QuarkusBindException;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.ThreadPoolConfig;
import io.quarkus.runtime.ValueRegistryImpl;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.logging.LogBuildTimeConfig;
import io.quarkus.runtime.shutdown.ShutdownConfig;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfig;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.value.registry.ValueRegistry.RuntimeKey;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.quarkus.vertx.http.DomainSocketServerStart;
import io.quarkus.vertx.http.HttpServerConfigCustomizer;
import io.quarkus.vertx.http.HttpServerStart;
import io.quarkus.vertx.http.HttpsServerStart;
import io.quarkus.vertx.http.ManagementInterface;
import io.quarkus.vertx.http.runtime.VertxHttpConfig.InsecureRequests;
import io.quarkus.vertx.http.runtime.cors.CORSFilter;
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
import io.quarkus.vertx.http.runtime.management.ManagementConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.options.HttpServerCommonHandlers;
import io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils;
import io.quarkus.vertx.http.runtime.options.TlsCertificateReloader;
import io.smallrye.common.cpu.ProcessorInfo;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.Http1ServerConfig;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.QueryParamDecoderConfig;
import io.vertx.core.http.WebSocketServerConfig;
import io.vertx.core.http.impl.http1.Http1ServerConnection;
import io.vertx.core.impl.Utils;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.net.SSLEngineOptions;
import io.vertx.core.net.ServerSSLOptions;
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

    private static final String DISABLE_WEBSOCKETS_PROP_NAME = "vertx.disableWebsockets";

    private static final int PORT_SHADOW_CHECK_TIMEOUT_MS = 500;

    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

    private static final Logger LOGGER = Logger.getLogger(VertxHttpRecorder.class.getName());

    private static volatile Handler<RoutingContext> hotReplacementHandler;
    private static volatile HotReplacementContext hotReplacementContext;
    private static volatile RemoteSyncHandler remoteSyncHandler;

    private static volatile Runnable closeTask;

    static volatile Handler<HttpServerRequest> rootHandler;

    private static volatile Handler<RoutingContext> nonApplicationRedirectHandler;

    private static volatile int actualHttpPort = -1;
    private static volatile int actualHttpsPort = -1;
    private static volatile int actualManagementPort = -1;

    public static final String GET = "GET";
    private static final Handler<HttpServerRequest> ACTUAL_ROOT = new Handler<HttpServerRequest>() {

        /** JVM system property that disables URI validation, don't use this in production. */
        private static final String DISABLE_URI_VALIDATION_PROP_NAME = "vertx.disableURIValidation";
        /**
         * Disables HTTP headers validation, so we can save some processing and save some allocations.
         */
        private final boolean DISABLE_URI_VALIDATION = Boolean.getBoolean(DISABLE_URI_VALIDATION_PROP_NAME);

        @Override
        public void handle(HttpServerRequest httpServerRequest) {
            if (!uriValid(httpServerRequest)) {
                httpServerRequest.response().setStatusCode(400).end();
                return;
            }

            //we need to pause the request to make sure that data does
            //not arrive before handlers have a chance to install a read handler
            //as it is possible filters such as the auth filter can do blocking tasks
            //as the underlying handler has not had a chance to install a read handler yet
            //and data that arrives while the blocking task is being processed will be lost
            if (!httpServerRequest.isEnded()) {
                httpServerRequest.pause();
            }
            Handler<HttpServerRequest> rh = VertxHttpRecorder.rootHandler;
            if (rh != null) {
                rh.handle(httpServerRequest);
            } else {
                //very rare race condition, that can happen when dev mode is shutting down
                if (!httpServerRequest.isEnded()) {
                    httpServerRequest.resume();
                }
                httpServerRequest.response().setStatusCode(503).end();
            }
        }

        private boolean uriValid(HttpServerRequest httpServerRequest) {
            String uri = httpServerRequest.uri();
            if (uri == null) { // it's not clear how this can happen, but it can
                return false;
            }
            if (DISABLE_URI_VALIDATION) {
                return true;
            }
            try {
                // we simply need to know if the URI is valid
                new URI(uri);
                return true;
            } catch (URISyntaxException e) {
                return false;
            }
        }
    };
    private static HttpServerConfig httpMainServerConfig;
    private static HttpServerConfig httpMainSslServerConfig;
    private static ServerSSLOptions httpMainSslOptions;
    private static SSLEngineOptions httpMainSslEngineOptions;
    private static HttpServerConfig httpMainDomainSocketConfig;
    private static HttpServerConfig httpManagementServerConfig;
    private static ServerSSLOptions httpManagementSslOptions;
    private static SSLEngineOptions httpManagementSslEngineOptions;

    private static final List<Long> refresTaskIds = new CopyOnWriteArrayList<>();

    final VertxHttpBuildTimeConfig httpBuildTimeConfig;
    final ManagementInterfaceBuildTimeConfig managementBuildTimeConfig;
    final RuntimeValue<VertxHttpConfig> httpConfig;
    final RuntimeValue<ManagementConfig> managementConfig;
    final RuntimeValue<ShutdownConfig> shutdownConfig;

    private static volatile RuntimeValue<ValueRegistry> valueRegistry;

    private static volatile Handler<HttpServerRequest> managementRouter;
    private static volatile Handler<HttpServerRequest> managementRouterDelegate;

    public VertxHttpRecorder(
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            RuntimeValue<VertxHttpConfig> httpConfig,
            RuntimeValue<ManagementConfig> managementConfig,
            RuntimeValue<ShutdownConfig> shutdownConfig,
            RuntimeValue<ValueRegistry> valueRegistry) {
        this.httpBuildTimeConfig = httpBuildTimeConfig;
        this.httpConfig = httpConfig;
        this.managementBuildTimeConfig = managementBuildTimeConfig;
        this.managementConfig = managementConfig;
        this.shutdownConfig = shutdownConfig;
        VertxHttpRecorder.valueRegistry = valueRegistry;
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
        hotReplacementContext = null;
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
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .addDiscoveredSources()
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new VertxConfigBuilder().configBuilder(builder);
                    }
                })
                .withMapping(VertxHttpBuildTimeConfig.class)
                .withMapping(VertxHttpConfig.class)
                .withMapping(ManagementInterfaceBuildTimeConfig.class)
                .withMapping(ManagementConfig.class)
                .withMapping(VertxConfiguration.class)
                .withMapping(ThreadPoolConfig.class)
                .withMapping(LiveReloadConfig.class)
                .build();
        if (supplier == null) {
            VertxConfiguration vertxConfiguration = config.getConfigMapping(VertxConfiguration.class);
            ThreadPoolConfig threadPoolConfig = config.getConfigMapping(ThreadPoolConfig.class);
            vertx = VertxCoreRecorder.recoverFailedStart(vertxConfiguration, threadPoolConfig).get();
        } else {
            vertx = supplier.get();
        }

        try {
            VertxHttpBuildTimeConfig httpBuildConfig = config.getConfigMapping(VertxHttpBuildTimeConfig.class);
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig = config
                    .getConfigMapping(ManagementInterfaceBuildTimeConfig.class);
            VertxHttpConfig httpConfig = config.getConfigMapping(VertxHttpConfig.class);
            ManagementConfig managementConfig = config.getConfigMapping(ManagementConfig.class);

            Router router = Router.router(vertx);
            if (hotReplacementHandler != null) {
                router.route().order(RouteConstants.ROUTE_ORDER_HOT_REPLACEMENT).blockingHandler(hotReplacementHandler);
            }

            Handler<HttpServerRequest> root = router;
            LiveReloadConfig liveReloadConfig = config.getConfigMapping(LiveReloadConfig.class);
            if (liveReloadConfig.password().isPresent()
                    && hotReplacementContext.getDevModeType() == DevModeType.REMOTE_SERVER_SIDE) {
                root = remoteSyncHandler = new RemoteSyncHandler(liveReloadConfig.password().get(), root,
                        hotReplacementContext, "/");
            }
            rootHandler = root;

            var insecureRequestStrategy = getInsecureRequestStrategy(httpConfig, httpBuildConfig, LaunchMode.DEVELOPMENT);
            doServerStart(vertx, httpBuildConfig, httpConfig, null, managementBuildTimeConfig, managementConfig,
                    LaunchMode.DEVELOPMENT,
                    new Supplier<Integer>() {
                        @Override
                        public Integer get() {
                            return ProcessorInfo.availableProcessors(); //this is dev mode, so the number of IO threads not always being 100% correct does not really matter in this case
                        }
                    }, null, insecureRequestStrategy, false);
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

    public RuntimeValue<SubmissionPublisher<String>> createAccessLogPublisher() {
        return new RuntimeValue<>(new SubmissionPublisher<>());
    }

    public void startServer(Supplier<Vertx> vertx, ShutdownContext shutdown,
            LaunchMode launchMode,
            boolean startVirtual, boolean startSocket, Supplier<Integer> ioThreads, List<String> websocketSubProtocols,
            boolean auxiliaryApplication, boolean disableWebSockets)
            throws IOException {

        // disable websockets if we have determined at build time that we should and the user has not overridden the relevant Vert.x property
        if (disableWebSockets && !System.getProperties().containsKey(DISABLE_WEBSOCKETS_PROP_NAME)) {
            System.setProperty(DISABLE_WEBSOCKETS_PROP_NAME, "true");
        }

        if (startVirtual) {
            initializeVirtual(vertx.get());
            shutdown.addShutdownTask(() -> {
                try {
                    virtualBootstrapChannel.channel().close().sync();
                } catch (InterruptedException e) {
                    LOGGER.warn("Unable to close virtualBootstrapChannel");
                } finally {
                    virtualBootstrapChannel = null;
                    virtualBootstrap = null;
                }
            });
        }
        VertxHttpConfig httpConfiguration = this.httpConfig.getValue();
        ManagementConfig managementConfig = this.managementConfig == null ? null : this.managementConfig.getValue();
        if (startSocket && (httpConfiguration.hostEnabled() || httpConfiguration.domainSocketEnabled()
                || (managementConfig != null && managementConfig.hostEnabled())
                || (managementConfig != null && managementConfig.domainSocketEnabled()))) {
            // Start the server
            if (closeTask == null) {
                var insecureRequestStrategy = getInsecureRequestStrategy(httpConfiguration, httpBuildTimeConfig, launchMode);
                doServerStart(vertx.get(), httpBuildTimeConfig, httpConfiguration, managementRouter, managementBuildTimeConfig,
                        managementConfig, launchMode, ioThreads, websocketSubProtocols,
                        insecureRequestStrategy,
                        auxiliaryApplication);
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
        String p = frameworkPath.endsWith("/") ? frameworkPath + "*" : frameworkPath + "/*";
        mainRouter.getValue().route(p).subRouter(frameworkRouter.getValue());
    }

    public void finalizeRouter(
            Consumer<Route> defaultRouteHandler,
            List<Filter> filterList, List<Filter> managementInterfaceFilterList, Supplier<Vertx> vertx,
            LiveReloadConfig liveReloadConfig, Optional<RuntimeValue<Router>> mainRouterRuntimeValue,
            RuntimeValue<Router> httpRouterRuntimeValue, RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter,
            RuntimeValue<Router> frameworkRouter, RuntimeValue<Router> managementRouter,
            String rootPath, String nonRootPath,
            LaunchMode launchMode, BooleanSupplier[] requireBodyHandlerConditions,
            Handler<RoutingContext> bodyHandler,
            GracefulShutdownFilter gracefulShutdownFilter,
            Executor executor,
            LogBuildTimeConfig logBuildTimeConfig,
            String srcMainJava,
            List<String> knowClasses,
            List<ErrorPageAction> actions,
            Optional<RuntimeValue<SubmissionPublisher<String>>> publisher) {
        VertxHttpConfig httpConfig = this.httpConfig.getValue();
        // install the default route at the end
        Router httpRouteRouter = httpRouterRuntimeValue.getValue();

        //allow the router to be modified programmatically
        Event<Object> event = Arc.container().beanManager().getEvent();

        // First, fire an event with the filter collector
        Filters filters = new Filters();
        event.select(Filters.class).fire(filters);

        filterList.addAll(filters.getFilters());

        // Then, fire the resuming router
        event.select(Router.class, Default.Literal.INSTANCE).fire(httpRouteRouter);
        // Also fires the Mutiny one
        event.select(io.vertx.mutiny.ext.web.Router.class).fire(mutinyRouter.getValue());

        for (Filter filter : filterList) {
            if (filter.getHandler() != null) {
                if (filter.isFailureHandler()) {
                    // Filters handling failures with high priority gets called first.
                    httpRouteRouter.route().order(-1 * filter.getPriority()).failureHandler(filter.getHandler());
                } else {
                    // Filters handling HTTP requests with high priority gets called first.
                    httpRouteRouter.route().order(-1 * filter.getPriority()).handler(filter.getHandler());
                }
            }
        }

        if (defaultRouteHandler != null) {
            defaultRouteHandler.accept(httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_DEFAULT));
        }

        applyCompression(httpBuildTimeConfig.enableCompression(), httpRouteRouter);
        httpRouteRouter.route().last().failureHandler(
                new QuarkusErrorHandler(launchMode.isDevOrTest(), decorateStacktrace(launchMode, logBuildTimeConfig),
                        httpConfig.unhandledErrorContentTypeDefault(), srcMainJava, knowClasses, actions));
        for (BooleanSupplier requireBodyHandlerCondition : requireBodyHandlerConditions) {
            if (requireBodyHandlerCondition.getAsBoolean()) {
                //if this is set then everything needs the body handler installed
                //TODO: config etc
                httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_BODY_HANDLER).handler(new Handler<RoutingContext>() {
                    @Override
                    public void handle(RoutingContext routingContext) {
                        routingContext.request().resume();
                        bodyHandler.handle(routingContext);
                    }
                });
                break;
            }
        }

        HttpServerCommonHandlers.enforceMaxBodySize(httpConfig.limits(), httpRouteRouter);
        // Filter Configuration per path
        var filtersInConfig = httpConfig.filter();
        HttpServerCommonHandlers.applyFilters(filtersInConfig, httpRouteRouter);
        // Headers sent on any request, regardless of the response
        HttpServerCommonHandlers.applyHeaders(httpConfig.header(), httpRouteRouter);

        Handler<HttpServerRequest> root;
        if (rootPath.equals("/")) {
            addHotReplacementHandlerIfNeeded(httpRouteRouter);
            root = httpRouteRouter;
        } else {
            Router mainRouter = mainRouterRuntimeValue.isPresent() ? mainRouterRuntimeValue.get().getValue()
                    : Router.router(vertx.get());
            String p = rootPath.endsWith("/") ? rootPath + "*" : rootPath + "/*";
            mainRouter.route(p).subRouter(httpRouteRouter);

            addHotReplacementHandlerIfNeeded(mainRouter);
            root = mainRouter;
        }

        warnIfProxyAddressForwardingAllowedWithMultipleHeaders(httpConfig.proxy());
        root = HttpServerCommonHandlers.applyProxy(httpConfig.proxy(), root, vertx,
                getTlsClientAuth(httpConfig, httpBuildTimeConfig, launchMode),
                getHttpServerTlsConfigName(httpConfig, httpBuildTimeConfig, launchMode),
                "quarkus.http");

        boolean quarkusWrapperNeeded = false;

        if (shutdownConfig.getValue().isTimeoutEnabled()) {
            gracefulShutdownFilter.next(root);
            root = gracefulShutdownFilter;
            quarkusWrapperNeeded = true;
        }

        AccessLogConfig accessLog = httpConfig.accessLog();
        if (accessLog.enabled()) {
            AccessLogReceiver receiver;
            if (accessLog.logToFile()) {
                File outputDir = accessLog.logDirectory().isPresent() ? new File(accessLog.logDirectory().get()) : new File("");
                receiver = new DefaultAccessLogReceiver(executor, outputDir, accessLog.baseFileName(), accessLog.logSuffix(),
                        accessLog.rotate());
            } else {
                receiver = new JBossLoggingAccessLogReceiver(accessLog.category());
            }
            setupAccessLogHandler(mainRouterRuntimeValue, httpRouterRuntimeValue, frameworkRouter, receiver, rootPath,
                    nonRootPath, accessLog.pattern(), accessLog.consolidateReroutedRequests(), accessLog.excludePattern());
            quarkusWrapperNeeded = true;
        }

        // Add an access log for Dev UI

        if (publisher.isPresent()) {
            SubmissionPublisher<String> logPublisher = publisher.get().getValue();
            AccessLogReceiver receiver = new AccessLogReceiver() {
                @Override
                public void logMessage(String message) {
                    logPublisher.submit(message);
                }
            };

            setupAccessLogHandler(mainRouterRuntimeValue, httpRouterRuntimeValue, frameworkRouter, receiver, rootPath,
                    nonRootPath, accessLog.pattern(), accessLog.consolidateReroutedRequests(),
                    accessLog.excludePattern().or(() -> Optional.of("^" + nonRootPath + ".*")));
            quarkusWrapperNeeded = true;
        }

        BiConsumer<Cookie, HttpServerRequest> cookieFunction = null;
        if (!httpConfig.sameSiteCookie().isEmpty()) {
            cookieFunction = processSameSiteConfig(httpConfig.sameSiteCookie());
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

        final boolean mustResumeRequest = httpConfig.limits().maxBodySize().isPresent();
        Handler<HttpServerRequest> delegate = root;
        root = HttpServerCommonHandlers.enforceDuplicatedContext(delegate, mustResumeRequest);
        if (httpConfig.recordRequestStartTime()) {
            httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_RECORD_START_TIME).handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    event.put(REQUEST_START_TIME, System.nanoTime());
                    event.next();
                }
            });
        }
        if (launchMode == LaunchMode.DEVELOPMENT && liveReloadConfig.password().isPresent()
                && hotReplacementContext.getDevModeType() == DevModeType.REMOTE_SERVER_SIDE) {
            root = remoteSyncHandler = new RemoteSyncHandler(liveReloadConfig.password().get(), root, hotReplacementContext,
                    rootPath);
        }
        rootHandler = root;

        if (managementRouter != null && managementRouter.getValue() != null) {
            // Add body handler and cors handler
            var mr = managementRouter.getValue();
            boolean hasManagementRoutes = !mr.getRoutes().isEmpty();

            // We do not add the hotreload handler on the management interface

            mr.route().last().failureHandler(
                    new QuarkusErrorHandler(launchMode.isDevOrTest(), decorateStacktrace(launchMode, logBuildTimeConfig),
                            httpConfig.unhandledErrorContentTypeDefault(), srcMainJava, knowClasses, actions));

            Handler<RoutingContext> hostValidationHandler = HostValidationRecorder.hostValidationHandler(
                    managementConfig.getValue().hostValidation(),
                    managementConfig.getValue().host(),
                    true);
            if (hostValidationHandler != null) {
                mr.route().order(RouteConstants.ROUTE_ORDER_HOST_VALIDATION_MANAGEMENT).handler(hostValidationHandler);
            }

            if (managementConfig.getValue().cors().enabled()) {
                mr.route().order(RouteConstants.ROUTE_ORDER_CORS_MANAGEMENT)
                        .handler(new CORSFilter(managementConfig.getValue().cors()));
            }

            mr.route().order(RouteConstants.ROUTE_ORDER_BODY_HANDLER_MANAGEMENT)
                    .handler(createBodyHandlerForManagementInterface());

            HttpServerCommonHandlers.applyFilters(managementConfig.getValue().filter(), mr);
            for (Filter filter : managementInterfaceFilterList) {
                mr.route().order(filter.getPriority()).handler(filter.getHandler());
            }

            HttpServerCommonHandlers.applyHeaders(managementConfig.getValue().header(), mr);
            applyCompression(managementBuildTimeConfig.enableCompression(), mr);

            Handler<HttpServerRequest> handler = HttpServerCommonHandlers.enforceDuplicatedContext(mr, mustResumeRequest);
            handler = HttpServerCommonHandlers.applyProxy(managementConfig.getValue().proxy(), handler, vertx,
                    managementBuildTimeConfig.tlsClientAuth(), managementConfig.getValue().tlsConfigurationName(),
                    "quarkus.management");

            int routesBeforeMiEvent = mr.getRoutes().size();
            event.select(ManagementInterface.class).fire(new ManagementInterfaceImpl(mr));

            // It may be that no build steps produced any management routes.
            // But we still want to give a chance to the "ManagementInterface event" to collect any
            // routes that users may have provided through observing this event.
            //
            // Hence, we only initialize the `managementRouter` router when we either had some routes from extensions (`hasManagementRoutes`)
            // or if the event collected some routes (`routesBeforeMiEvent < routesAfterMiEvent`)
            if (hasManagementRoutes || routesBeforeMiEvent < mr.getRoutes().size()) {
                VertxHttpRecorder.managementRouterDelegate = handler;
                if (VertxHttpRecorder.managementRouter == null) {
                    VertxHttpRecorder.managementRouter = new Handler<HttpServerRequest>() {
                        @Override
                        public void handle(HttpServerRequest event) {
                            VertxHttpRecorder.managementRouterDelegate.handle(event);
                        }
                    };
                }
            }
        }
    }

    private void setupAccessLogHandler(Optional<RuntimeValue<Router>> mainRouterRuntimeValue,
            RuntimeValue<Router> httpRouterRuntimeValue,
            RuntimeValue<Router> frameworkRouter,
            AccessLogReceiver receiver,
            String rootPath,
            String nonRootPath,
            String pattern,
            boolean consolidateReroutedRequests,
            Optional<String> excludePattern) {

        Router httpRouteRouter = httpRouterRuntimeValue.getValue();
        AccessLogHandler handler = new AccessLogHandler(receiver, pattern, consolidateReroutedRequests,
                getClass().getClassLoader(),
                excludePattern);
        if (rootPath.equals("/") || nonRootPath.equals("/")) {
            mainRouterRuntimeValue.orElse(httpRouterRuntimeValue).getValue().route()
                    .order(RouteConstants.ROUTE_ORDER_ACCESS_LOG_HANDLER)
                    .handler(handler);
        } else if (nonRootPath.startsWith(rootPath)) {
            httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_ACCESS_LOG_HANDLER).handler(handler);
        } else if (rootPath.startsWith(nonRootPath)) {
            // The framework (non-application) routes live on a dedicated router mounted above the
            // application router. If that router was never created (e.g. all non-application routes
            // moved to the management interface, or none were classified as framework routes), fall
            // back to the application router so application routes are still logged.
            if (frameworkRouter != null) {
                frameworkRouter.getValue().route().order(RouteConstants.ROUTE_ORDER_ACCESS_LOG_HANDLER).handler(handler);
            } else {
                httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_ACCESS_LOG_HANDLER).handler(handler);
            }
        } else {
            httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_ACCESS_LOG_HANDLER).handler(handler);
            // Only attach to the framework router when it exists; see comment above.
            if (frameworkRouter != null) {
                frameworkRouter.getValue().route().order(RouteConstants.ROUTE_ORDER_ACCESS_LOG_HANDLER).handler(handler);
            }
        }
    }

    private boolean decorateStacktrace(LaunchMode launchMode, LogBuildTimeConfig logBuildTimeConfig) {
        return logBuildTimeConfig.decorateStacktraces() && launchMode.equals(LaunchMode.DEVELOPMENT);
    }

    private void addHotReplacementHandlerIfNeeded(Router router) {
        if (hotReplacementHandler != null) {
            //recorders are always executed in the current CL
            ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
            router.route().order(RouteConstants.ROUTE_ORDER_HOT_REPLACEMENT)
                    .handler(new HotReplacementRoutingContextHandler(currentCl));
        }
    }

    private void applyCompression(boolean enableCompression, Router httpRouteRouter) {
        if (enableCompression) {
            httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_COMPRESSION).handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext ctx) {
                    // Add "Content-Encoding: identity" header that disables the compression
                    // This header can be removed to enable the compression
                    ctx.response().putHeader(HttpHeaders.CONTENT_ENCODING, HttpHeaders.IDENTITY);
                    ctx.next();
                }
            });
        }
    }

    private void warnIfProxyAddressForwardingAllowedWithMultipleHeaders(ProxyConfig proxyConfig) {
        boolean proxyAddressForwardingActivated = proxyConfig.proxyAddressForwarding();
        boolean forwardedActivated = proxyConfig.allowForwarded();
        boolean xForwardedActivated = proxyConfig.allowXForwarded().orElse(!forwardedActivated);

        if (proxyAddressForwardingActivated && forwardedActivated && xForwardedActivated) {
            LOGGER.warn(
                    "The X-Forwarded-* and Forwarded headers will be considered when determining the proxy address. " +
                            "This configuration can cause a security issue as clients can forge requests and send a " +
                            "forwarded header that is not overwritten by the proxy. " +
                            "Please consider use one of these headers just to forward the proxy address in requests.");
        }
    }

    private static CompletableFuture<HttpServer> initializeManagementInterfaceWithDomainSocket(Vertx vertx,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig, Handler<HttpServerRequest> managementRouter,
            ManagementConfig managementConfig,
            List<String> websocketSubProtocols) {
        CompletableFuture<HttpServer> managementInterfaceDomainSocketFuture = new CompletableFuture<>();
        if (!managementBuildTimeConfig.enabled() || managementRouter == null || managementConfig == null) {
            managementInterfaceDomainSocketFuture.complete(null);
            return managementInterfaceDomainSocketFuture;
        }

        HttpServerConfig domainSocketConfigForManagement = HttpServerOptionsUtils
                .createDomainSocketConfigForManagementInterface(
                        managementBuildTimeConfig, managementConfig, websocketSubProtocols);
        if (domainSocketConfigForManagement != null) {
            File file = new File(managementConfig.domainSocket());
            if (!file.getParentFile().canWrite()) {
                LOGGER.warnf(
                        "Unable to write in the domain socket directory (`%s`). Binding to the socket is likely going to fail.",
                        managementConfig.domainSocket());
            }
            vertx.createHttpServer(domainSocketConfigForManagement, null)
                    .requestHandler(managementRouter)
                    .listen().onComplete(ar -> {
                        if (ar.failed()) {
                            managementInterfaceDomainSocketFuture.completeExceptionally(
                                    new IllegalStateException(
                                            "Unable to start the management interface on the "
                                                    + domainSocketConfigForManagement.getTcpHost() + " domain socket",
                                            ar.cause()));
                        } else {
                            managementInterfaceDomainSocketFuture.complete(ar.result());
                        }
                    });
        } else {
            managementInterfaceDomainSocketFuture.complete(null);
        }
        return managementInterfaceDomainSocketFuture;
    }

    private static CompletableFuture<HttpServer> initializeManagementInterface(
            Vertx vertx,
            Handler<HttpServerRequest> managementRouter,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            ManagementConfig managementConfig,
            LaunchMode launchMode,
            List<String> websocketSubProtocols, TlsConfigurationRegistry registry) throws IOException {
        httpManagementServerConfig = null;
        httpManagementSslOptions = null;
        httpManagementSslEngineOptions = null;
        CompletableFuture<HttpServer> managementInterfaceFuture = new CompletableFuture<>();
        if (!managementBuildTimeConfig.enabled() || managementRouter == null || managementConfig == null) {
            managementInterfaceFuture.complete(null);
            return managementInterfaceFuture;
        }

        HttpServerConfig httpConfigForManagement = HttpServerOptionsUtils.createHttpServerConfigForManagementInterface(
                managementBuildTimeConfig, managementConfig, launchMode, websocketSubProtocols);
        if (httpConfigForManagement != null) {
            int port = managementConfig.determinePort(launchMode);
            if (port == 0) {
                httpConfigForManagement.setPort(
                        SocketAddress.sharedRandomPort(HttpServerOptionsUtils.RANDOM_PORT_MANAGEMENT,
                                managementConfig.host()).port());
            }
        }

        HttpServerOptionsUtils.ServerConfig sslServerConfig = HttpServerOptionsUtils
                .createSslServerConfigForManagementInterface(
                        managementBuildTimeConfig, managementConfig, launchMode, websocketSubProtocols, registry);
        if (sslServerConfig != null) {
            httpManagementServerConfig = sslServerConfig.config();
            httpManagementSslOptions = sslServerConfig.sslOptions();
            httpManagementSslEngineOptions = sslServerConfig.sslEngineOptions();
        }
        if (httpManagementSslOptions != null && httpManagementSslOptions.getKeyCertOptions() == null) {
            httpManagementServerConfig = httpConfigForManagement;
            httpManagementSslOptions = null;
            httpManagementSslEngineOptions = null;
        }
        if (httpManagementServerConfig == null) {
            httpManagementServerConfig = httpConfigForManagement;
        }

        // In Vert.x 5.1, if the configured port is 0, we need to switch to a socket address.
        SocketAddress address;
        if (httpManagementServerConfig != null && httpManagementServerConfig.getTcpPort() <= 0) {
            address = SocketAddress.sharedRandomPort(HttpServerOptionsUtils.RANDOM_PORT_MANAGEMENT,
                    httpManagementServerConfig.getTcpHost());
        } else {
            address = SocketAddress.inetSocketAddress(httpManagementServerConfig.getTcpPort(),
                    httpManagementServerConfig.getTcpHost());
        }

        if (httpManagementServerConfig != null) {
            createHttpServer(vertx, httpManagementServerConfig, httpManagementSslOptions, httpManagementSslEngineOptions)
                    .requestHandler(managementRouter)
                    .listen(address).onComplete(ar -> {
                        if (ar.failed()) {
                            managementInterfaceFuture.completeExceptionally(
                                    new IllegalStateException("Unable to start the management interface on "
                                            + httpManagementServerConfig.getTcpHost() + ":"
                                            + httpManagementServerConfig.getTcpPort(), ar.cause()));
                        } else {
                            if (httpManagementSslOptions != null
                                    && (managementConfig.ssl().certificate().reloadPeriod().isPresent())) {
                                try {
                                    ClientAuth clientAuth = managementBuildTimeConfig.tlsClientAuth();
                                    long l = TlsCertificateReloader.initCertReloadingAction(
                                            vertx, ar.result(), httpManagementServerConfig, httpManagementSslOptions,
                                            managementConfig.ssl(), registry,
                                            managementConfig.tlsConfigurationName(), clientAuth);
                                    if (l != -1) {
                                        refresTaskIds.add(l);
                                    }
                                } catch (IllegalArgumentException e) {
                                    managementInterfaceFuture.completeExceptionally(e);
                                    return;
                                }
                            }

                            if (httpManagementSslOptions != null) {
                                ClientAuth clientAuth = managementBuildTimeConfig.tlsClientAuth();
                                CDI.current().select(HttpCertificateUpdateEventListener.class).get()
                                        .register(ar.result(),
                                                managementConfig.tlsConfigurationName().orElse(TlsConfig.DEFAULT_NAME),
                                                "management interface", clientAuth);
                            }

                            actualManagementPort = ar.result().actualPort();
                            valueRegistry.getValue().register(MANAGEMENT_PORT, actualManagementPort);
                            if (launchMode.isDevOrTest()) {
                                valueRegistry.getValue().register(MANAGEMENT_TEST_PORT, actualManagementPort);
                            }
                            String mgmtScheme = httpManagementSslOptions != null ? "https" : "http";
                            String mgmtHost = httpManagementServerConfig.getTcpHost();
                            if ("0.0.0.0".equals(mgmtHost)) {
                                mgmtHost = "localhost";
                            }
                            String mgmtRootPath = managementBuildTimeConfig.rootPath();
                            if (!mgmtRootPath.startsWith("/")) {
                                mgmtRootPath = "/" + mgmtRootPath;
                            }
                            if (mgmtRootPath.length() > 1 && mgmtRootPath.endsWith("/")) {
                                mgmtRootPath = mgmtRootPath.substring(0, mgmtRootPath.length() - 1);
                            }
                            valueRegistry.getValue().register(LOCAL_MANAGEMENT_BASE_URI,
                                    URI.create(mgmtScheme + "://" + mgmtHost + ":" + actualManagementPort + mgmtRootPath));
                            managementInterfaceFuture.complete(ar.result());
                        }
                    });

        } else {
            managementInterfaceFuture.complete(null);
        }
        return managementInterfaceFuture;
    }

    private static CompletableFuture<String> initializeMainHttpServer(
            Vertx vertx,
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            VertxHttpConfig httpConfig,
            LaunchMode launchMode,
            Supplier<Integer> eventLoops, List<String> websocketSubProtocols, InsecureRequests insecureRequestStrategy,
            TlsConfigurationRegistry registry)
            throws IOException {

        if (!httpConfig.hostEnabled() && !httpConfig.domainSocketEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        // Http server configuration
        httpMainServerConfig = HttpServerOptionsUtils.createHttpServerConfig(httpBuildTimeConfig, httpConfig, launchMode,
                websocketSubProtocols);
        if (httpMainServerConfig != null) {
            int port = httpConfig.determinePort(launchMode);
            if (port <= 0) {
                httpMainServerConfig.setPort(
                        SocketAddress.sharedRandomPort(HttpServerOptionsUtils.RANDOM_PORT_MAIN_HTTP, httpConfig.host()).port());
            }
        }
        httpMainDomainSocketConfig = HttpServerOptionsUtils.createDomainSocketConfig(httpBuildTimeConfig, httpConfig,
                websocketSubProtocols);
        if (httpMainDomainSocketConfig != null) {
            File file = new File(httpConfig.domainSocket());
            if (!file.getParentFile().canWrite()) {
                LOGGER.warnf(
                        "Unable to write in the domain socket directory (`%s`). Binding to the socket is likely going to fail.",
                        httpConfig.domainSocket());
            }
        }

        HttpServerOptionsUtils.ServerConfig tmpSslConfig = HttpServerOptionsUtils.createSslServerConfig(
                httpBuildTimeConfig, httpConfig, launchMode, websocketSubProtocols, registry);
        httpMainSslServerConfig = tmpSslConfig != null ? tmpSslConfig.config() : null;
        ServerSSLOptions tmpSslOptions = tmpSslConfig != null ? tmpSslConfig.sslOptions() : null;
        httpMainSslEngineOptions = tmpSslConfig != null ? tmpSslConfig.sslEngineOptions() : null;

        // Customize
        ArcContainer container = Arc.container();
        if (container != null) {
            List<InstanceHandle<HttpServerConfigCustomizer>> instances = container
                    .listAll(HttpServerConfigCustomizer.class);
            for (InstanceHandle<HttpServerConfigCustomizer> instance : instances) {
                HttpServerConfigCustomizer customizer = instance.get();
                if (httpMainServerConfig != null) {
                    customizer.customizeHttpServer(httpMainServerConfig);
                }
                if (httpMainSslServerConfig != null && tmpSslOptions != null) {
                    customizer.customizeHttpsServer(httpMainSslServerConfig, tmpSslOptions);
                }
                if (httpMainDomainSocketConfig != null) {
                    customizer.customizeDomainSocketServer(httpMainDomainSocketConfig);
                }
            }
        }

        // Disable TLS if certificate options are still missing after customize hooks.
        if (tmpSslOptions != null && tmpSslOptions.getKeyCertOptions() == null) {
            tmpSslOptions = null;
            httpMainSslServerConfig = null;
            httpMainSslEngineOptions = null;
        }
        httpMainSslOptions = tmpSslOptions;

        if (insecureRequestStrategy != InsecureRequests.ENABLED
                && httpMainSslOptions == null) {
            throw new IllegalStateException("Cannot set quarkus.http.insecure-requests without enabling SSL.");
        }

        int eventLoopCount = eventLoops.get();
        final int ioThreads;
        if (httpConfig.ioThreads().isPresent()) {
            ioThreads = Math.min(httpConfig.ioThreads().getAsInt(), eventLoopCount);
        } else if (launchMode.isDevOrTest()) {
            ioThreads = Math.min(2, eventLoopCount); //Don't start ~100 threads to run a couple unit tests
        } else {
            ioThreads = eventLoopCount;
        }
        CompletableFuture<String> futureResult = new CompletableFuture<>();

        AtomicInteger connectionCount = new AtomicInteger();

        // Note that a new HttpServer is created for each IO thread, but we only want register once,
        // for the first server that started listening
        // See https://vertx.io/docs/vertx-core/java/#_server_sharing for more information
        AtomicBoolean startEventsFired = new AtomicBoolean();
        AtomicBoolean registerHttpServer = new AtomicBoolean();
        AtomicBoolean registerHttpsServer = new AtomicBoolean();

        vertx.deployVerticle(new Supplier<Verticle>() {
            @Override
            public Verticle get() {
                return new WebDeploymentVerticle(
                        httpMainServerConfig, httpMainSslServerConfig, httpMainSslOptions, httpMainSslEngineOptions,
                        httpMainDomainSocketConfig,
                        launchMode, insecureRequestStrategy, connectionCount, registry, startEventsFired,
                        httpBuildTimeConfig, httpConfig, registerHttpServer, registerHttpsServer);
            }
        }, new DeploymentOptions().setInstances(ioThreads)).onComplete(new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> event) {
                if (event.failed()) {
                    futureResult.completeExceptionally(event.cause());
                } else {
                    futureResult.complete(event.result());
                }
            }
        });

        return futureResult;
    }

    private static void doServerStart(
            Vertx vertx,
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            VertxHttpConfig httpConfig,
            Handler<HttpServerRequest> managementRouter,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            ManagementConfig managementConfig,
            LaunchMode launchMode,
            Supplier<Integer> eventLoops,
            List<String> websocketSubProtocols,
            InsecureRequests insecureRequestStrategy,
            boolean auxiliaryApplication) throws IOException {

        TlsConfigurationRegistry registry = null;
        if (Arc.container() != null) {
            registry = Arc.container().select(TlsConfigurationRegistry.class).orNull();
        }

        var mainServerFuture = initializeMainHttpServer(vertx, httpBuildTimeConfig, httpConfig, launchMode,
                eventLoops, websocketSubProtocols, insecureRequestStrategy, registry);
        var managementInterfaceFuture = initializeManagementInterface(vertx, managementRouter, managementBuildTimeConfig,
                managementConfig, launchMode, websocketSubProtocols, registry);
        var managementInterfaceDomainSocketFuture = initializeManagementInterfaceWithDomainSocket(vertx,
                managementBuildTimeConfig, managementRouter, managementConfig, websocketSubProtocols);

        try {
            String deploymentIdIfAny = mainServerFuture.get();

            HttpServer tmpManagementServer = null;
            HttpServer tmpManagementServerUsingDomainSocket = null;
            if (managementRouter != null) {
                tmpManagementServer = managementInterfaceFuture.get();
                tmpManagementServerUsingDomainSocket = managementInterfaceDomainSocketFuture.get();
            }
            HttpServer managementServer = tmpManagementServer;
            HttpServer managementServerDomainSocket = tmpManagementServerUsingDomainSocket;
            if (deploymentIdIfAny != null) {
                VertxCoreRecorder.setWebDeploymentId(deploymentIdIfAny);
            }

            closeTask = new Runnable() {
                @Override
                public synchronized void run() {
                    //guard against this being run twice
                    if (closeTask == this) {
                        boolean isVertxClose = ((VertxInternal) vertx).closeFuture().future().isComplete();
                        int count = 0;
                        if (deploymentIdIfAny != null && vertx.deploymentIDs().contains(deploymentIdIfAny)) {
                            count++;
                        }
                        if (managementServer != null && !isVertxClose) {
                            count++;
                        }
                        if (managementServerDomainSocket != null && !isVertxClose) {
                            count++;
                        }

                        CountDownLatch latch = new CountDownLatch(count);
                        var handler = new Handler<AsyncResult<Void>>() {
                            @Override
                            public void handle(AsyncResult<Void> event) {
                                latch.countDown();
                            }
                        };

                        // shutdown main HTTP server
                        if (deploymentIdIfAny != null) {
                            try {
                                vertx.undeploy(deploymentIdIfAny).onComplete(handler);
                            } catch (Exception e) {
                                if (e instanceof RejectedExecutionException) {
                                    // Shutting down
                                    LOGGER.debug("Failed to undeploy deployment because a task was rejected (due to shutdown)",
                                            e);
                                } else {
                                    LOGGER.warn("Failed to undeploy deployment", e);
                                }
                            }
                        }

                        // shutdown the management interface
                        try {
                            for (Long id : refresTaskIds) {
                                TlsCertificateReloader.unschedule(vertx, id);
                            }
                            if (managementServer != null && !isVertxClose) {
                                managementServer.close().onComplete(handler);
                            }
                            if (managementServerDomainSocket != null && !isVertxClose) {
                                managementServerDomainSocket.close().onComplete(handler);
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Unable to shutdown the management interface quietly", e);
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
            };
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unable to start HTTP server", e);
        }

        setHttpServerTiming(insecureRequestStrategy == InsecureRequests.DISABLED, httpMainServerConfig,
                httpMainSslServerConfig,
                httpMainDomainSocketConfig,
                auxiliaryApplication, httpManagementServerConfig);
    }

    private static void setHttpServerTiming(boolean httpDisabled, HttpServerConfig httpServerConfig,
            HttpServerConfig sslConfig,
            HttpServerConfig domainSocketConfig, boolean auxiliaryApplication, HttpServerConfig managementConfig) {
        StringBuilder serverListeningMessage = new StringBuilder("Listening on: ");
        int socketCount = 0;

        if (!httpDisabled && httpServerConfig != null) {
            serverListeningMessage.append(String.format(
                    "http://%s:%s", getDeveloperFriendlyHostName(httpServerConfig), actualHttpPort));
            socketCount++;
        }

        if (sslConfig != null) {
            if (socketCount > 0) {
                serverListeningMessage.append(" and ");
            }
            serverListeningMessage
                    .append(String.format("https://%s:%s", getDeveloperFriendlyHostName(sslConfig), actualHttpsPort));
            socketCount++;
        }

        if (domainSocketConfig != null) {
            if (socketCount > 0) {
                serverListeningMessage.append(" and ");
            }
            serverListeningMessage.append(String.format("unix:%s", getDeveloperFriendlyHostName(domainSocketConfig)));
        }
        if (managementConfig != null) {
            serverListeningMessage.append(
                    String.format(". Management interface listening on http%s://%s:%s.",
                            httpManagementSslOptions != null ? "s" : "",
                            getDeveloperFriendlyHostName(managementConfig), actualManagementPort));
        }

        Timing.setHttpServer(serverListeningMessage.toString(), auxiliaryApplication);
    }

    /**
     * To improve developer experience in WSL dev/test mode, the server listening message should print "localhost" when
     * the host is set to "0.0.0.0". Otherwise, display the actual host.
     * Do not use this during the actual configuration, use config.getTcpHost() there directly instead.
     */
    private static String getDeveloperFriendlyHostName(HttpServerConfig config) {
        return (LaunchMode.current().isDevOrTest() && "0.0.0.0".equals(config.getTcpHost()) && isWSL()) ? "localhost"
                : config.getTcpHost();
    }

    /**
     * @return {@code true} if the application is running in a WSL (Windows Subsystem for Linux) environment
     */
    private static boolean isWSL() {
        var sysEnv = System.getenv();
        return sysEnv.containsKey("IS_WSL") || sysEnv.containsKey("WSL_DISTRO_NAME");
    }

    public void addRoute(RuntimeValue<Router> router, Function<Router, Route> route, Handler<RoutingContext> handler,
            HandlerType type) {

        Route vr = route.apply(router.getValue());
        if (type == HandlerType.BLOCKING) {
            vr.blockingHandler(handler, false);
        } else if (type == HandlerType.FAILURE) {
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

    private static HttpServer createHttpServer(Vertx vertx, HttpServerConfig config, ServerSSLOptions sslOptions,
            SSLEngineOptions engineOptions) {
        if (engineOptions != null) {
            return vertx.httpServerBuilder()
                    .with(config)
                    .with(sslOptions)
                    .with(engineOptions)
                    .build();
        }
        return vertx.createHttpServer(config, sslOptions);
    }

    private static class WebDeploymentVerticle extends AbstractVerticle implements Resource {
        private final TlsConfigurationRegistry registry;
        private HttpServer httpServer;
        private HttpServer httpsServer;
        private HttpServer domainSocketServer;
        private final HttpServerConfig httpConfig_server;
        private final HttpServerConfig httpsConfig_server;
        private final ServerSSLOptions sslOptions;
        private final SSLEngineOptions sslEngineOptions;
        private final HttpServerConfig domainSocketConfig_server;
        private final LaunchMode launchMode;
        private final InsecureRequests insecureRequests;
        private final AtomicInteger connectionCount;
        private final List<Long> reloadingTasks = new CopyOnWriteArrayList<>();
        private final AtomicBoolean startEventsFired;
        private final VertxHttpBuildTimeConfig httpBuildTimeConfig;
        private final VertxHttpConfig httpConfig;
        private final ValueRegistry valueRegistry;
        private final AtomicBoolean registerHttpServer;
        private final AtomicBoolean registerHttpsServer;

        public WebDeploymentVerticle(
                HttpServerConfig httpConfig,
                HttpServerConfig httpsConfig,
                ServerSSLOptions sslOptions,
                SSLEngineOptions sslEngineOptions,
                HttpServerConfig domainSocketConfig,
                LaunchMode launchMode,
                InsecureRequests insecureRequests,
                AtomicInteger connectionCount,
                TlsConfigurationRegistry registry,
                AtomicBoolean startEventsFired,
                VertxHttpBuildTimeConfig httpBuildTimeConfig,
                VertxHttpConfig vertxHttpConfig,
                AtomicBoolean registerHttpServer,
                AtomicBoolean registerHttpsServer) {

            this.httpConfig_server = httpConfig;
            this.httpsConfig_server = httpsConfig;
            this.sslOptions = sslOptions;
            this.sslEngineOptions = sslEngineOptions;
            this.launchMode = launchMode;
            this.domainSocketConfig_server = domainSocketConfig;
            this.insecureRequests = insecureRequests;
            this.httpConfig = vertxHttpConfig;
            this.connectionCount = connectionCount;
            this.registry = registry;
            this.startEventsFired = startEventsFired;
            this.httpBuildTimeConfig = httpBuildTimeConfig;
            this.registerHttpServer = registerHttpServer;
            this.registerHttpsServer = registerHttpsServer;
            this.valueRegistry = VertxHttpRecorder.valueRegistry != null ? VertxHttpRecorder.valueRegistry.getValue()
                    : ValueRegistryImpl.builder().build();
            if (CracSupport.isEnabled()) {
                org.crac.Core.getGlobalContext().register(this);
            }
        }

        @Override
        public void start(Promise<Void> startFuture) {
            assert Context.isOnEventLoopThread();

            final AtomicInteger remainingCount = new AtomicInteger(0);
            boolean httpServerEnabled = httpConfig_server != null && insecureRequests != InsecureRequests.DISABLED;
            if (httpServerEnabled) {
                remainingCount.incrementAndGet();
            }
            if (httpsConfig_server != null) {
                remainingCount.incrementAndGet();
            }
            if (domainSocketConfig_server != null) {
                remainingCount.incrementAndGet();
            }

            if (remainingCount.get() == 0) {
                startFuture
                        .fail(new IllegalArgumentException("Must configure at least one of http, https or unix domain socket"));
            }

            ArcContainer container = Arc.container();
            boolean notifyStartObservers = container != null ? startEventsFired.compareAndSet(false, true) : false;

            if (httpServerEnabled) {
                httpServer = vertx.createHttpServer(httpConfig_server, null);
                if (insecureRequests == InsecureRequests.ENABLED) {
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
                                                    "https://" + host + ":" + httpsConfig_server.getTcpPort() + req.uri())
                                            .end();
                                }
                            } catch (Exception e) {
                                req.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                            }
                        }
                    });
                }
                setupTcpHttpServer(httpServer, httpConfig_server, false, null, startFuture, remainingCount,
                        connectionCount, container, notifyStartObservers);
            }

            if (domainSocketConfig_server != null) {
                domainSocketServer = vertx.createHttpServer(domainSocketConfig_server, null);
                domainSocketServer.requestHandler(ACTUAL_ROOT);
                setupUnixDomainSocketHttpServer(domainSocketServer, domainSocketConfig_server, startFuture, remainingCount,
                        container, notifyStartObservers);
            }

            if (httpsConfig_server != null) {
                httpsServer = createHttpServer(vertx, httpsConfig_server, sslOptions, sslEngineOptions);
                httpsServer.requestHandler(ACTUAL_ROOT);
                setupTcpHttpServer(httpsServer, httpsConfig_server, true, sslOptions, startFuture, remainingCount,
                        connectionCount, container, notifyStartObservers);
            }
        }

        private void setupUnixDomainSocketHttpServer(HttpServer httpServer, HttpServerConfig config,
                Promise<Void> startFuture,
                AtomicInteger remainingCount, ArcContainer container, boolean notifyStartObservers) {
            httpServer.listen(SocketAddress.domainSocketAddress(config.getTcpHost())).onComplete(event -> {
                if (event.succeeded()) {
                    if (notifyStartObservers) {
                        container.beanManager().getEvent().select(DomainSocketServerStart.class)
                                .fireAsync(new DomainSocketServerStart(config));
                    }
                    if (remainingCount.decrementAndGet() == 0) {
                        startFuture.complete(null);
                    }
                } else {
                    if (event.cause() != null && event.cause().getMessage() != null
                            && event.cause().getMessage().contains("Permission denied")) {
                        startFuture.fail(new IllegalStateException(
                                String.format(
                                        "Unable to bind to Unix Domain Socket (%s) as the application does not have the permission to write in the directory.",
                                        domainSocketConfig_server.getTcpHost())));

                    } else if (event.cause() instanceof IllegalArgumentException) {
                        startFuture.fail(new IllegalArgumentException(
                                String.format(
                                        "Unable to bind to Unix domain socket. Consider adding the 'io.netty:%s' dependency. See the Quarkus Vert.x reference guide for more details.",
                                        Utils.isLinux() ? "netty-transport-native-epoll" : "netty-transport-native-kqueue")));
                    } else {
                        startFuture.fail(event.cause());
                    }
                }
            });
        }

        private void setupTcpHttpServer(HttpServer httpServer, HttpServerConfig config, boolean https,
                ServerSSLOptions sslOptions, Promise<Void> startFuture, AtomicInteger remainingCount,
                AtomicInteger currentConnectionCount, ArcContainer container, boolean notifyStartObservers) {

            if (httpConfig.limits().maxConnections().isPresent() && httpConfig.limits().maxConnections().getAsInt() > 0) {
                var tracker = vertx.isMetricsEnabled()
                        ? ((ExtendedQuarkusVertxHttpMetrics) ((VertxInternal) vertx).metrics()).getHttpConnectionTracker()
                        : ExtendedQuarkusVertxHttpMetrics.NOOP_CONNECTION_TRACKER;

                final int maxConnections = httpConfig.limits().maxConnections().getAsInt();
                tracker.initialize(maxConnections, currentConnectionCount);
                httpServer.connectionHandler(new Handler<HttpConnection>() {

                    @Override
                    public void handle(HttpConnection event) {
                        int current;
                        do {
                            current = currentConnectionCount.get();
                            if (current == maxConnections) {
                                //just close the connection
                                LOGGER.debug("Rejecting connection as there are too many active connections");
                                tracker.onConnectionRejected();
                                event.close();
                                return;
                            }
                        } while (!currentConnectionCount.compareAndSet(current, current + 1));
                        event.closeHandler(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                LOGGER.debug("Connection closed");
                                currentConnectionCount.decrementAndGet();
                            }
                        });
                    }
                });
            }
            SocketAddress address = config.getTcpPort() <= 0 ? SocketAddress.sharedRandomPort(
                    https ? HttpServerOptionsUtils.RANDOM_PORT_MAIN_TLS : HttpServerOptionsUtils.RANDOM_PORT_MAIN_HTTP,
                    config.getTcpHost()) : SocketAddress.inetSocketAddress(config.getTcpPort(), config.getTcpHost());
            httpServer.listen(address).onComplete(new Handler<>() {
                @Override
                public void handle(AsyncResult<HttpServer> event) {
                    if (event.cause() != null) {
                        if (event.cause() instanceof BindException e) {
                            startFuture.fail(new QuarkusBindException(config.getTcpHost(), config.getTcpPort(), e));
                        } else {
                            startFuture.fail(event.cause());
                        }
                    } else {
                        // Port may be random, so set the actual port
                        int actualPort = event.result().actualPort();

                        if (https) {
                            // Note that a new HttpServer is created for each IO thread, but we only want to register the
                            // real ports once. See https://vertx.io/docs/vertx-core/java/#_server_sharing
                            if (registerHttpsServer.compareAndSet(false, true)) {
                                actualHttpsPort = actualPort;
                                validateHttpPorts(actualHttpPort, actualHttpsPort);
                                valueRegistry.register(HTTPS_PORT, actualPort);
                                URI localBaseUri = localBaseUri("https", actualPort);
                                // Someone else may register the local base uri first (lambda extension)
                                // The implemented behaviour is that lambda has priority, but we may want to review that
                                if (!insecureRequests.equals(InsecureRequests.ENABLED)
                                        && !valueRegistry.containsKey(LOCAL_BASE_URI)) {
                                    valueRegistry.register(LOCAL_BASE_URI, localBaseUri);
                                }
                                if (launchMode.isDevOrTest()) {
                                    valueRegistry.register(HTTPS_TEST_PORT, actualPort);
                                    // TODO - Should we register test.url.ssl? We don't use it, or have tests for it
                                }
                            }
                        } else {
                            // Note that a new HttpServer is created for each IO thread, but we only want to register the
                            // real ports once. See https://vertx.io/docs/vertx-core/java/#_server_sharing
                            if (registerHttpServer.compareAndSet(false, true)) {
                                actualHttpPort = actualPort;
                                validateHttpPorts(actualHttpPort, actualHttpsPort);
                                valueRegistry.register(HTTP_PORT, actualPort);
                                URI localBaseUri = localBaseUri("http", actualPort);
                                // Someone else may register the local base uri first (lambda extension)
                                // The implemented behaviour is that lambda has priority, but we may want to review that
                                if (insecureRequests.equals(InsecureRequests.ENABLED)
                                        && !valueRegistry.containsKey(LOCAL_BASE_URI)) {
                                    valueRegistry.register(LOCAL_BASE_URI, localBaseUri);
                                }
                                if (launchMode.isDevOrTest()) {
                                    valueRegistry.register(HTTP_TEST_PORT, actualPort);
                                    // Compatibility with test.url
                                    valueRegistry.register(RuntimeKey.key("test.url"), localBaseUri.toString());
                                }
                                checkPortShadowedByAnotherProcess(options.getHost(), actualPort);
                            }
                        }

                        if (https && (httpConfig.ssl().certificate().reloadPeriod().isPresent())) {
                            try {
                                ClientAuth clientAuth = getTlsClientAuth(httpConfig, httpBuildTimeConfig, launchMode);
                                long l = TlsCertificateReloader.initCertReloadingAction(
                                        vertx, httpsServer, httpsConfig_server, sslOptions, httpConfig.ssl(), registry,
                                        getHttpServerTlsConfigName(httpConfig, httpBuildTimeConfig, launchMode), clientAuth);
                                if (l != -1) {
                                    reloadingTasks.add(l);
                                }
                            } catch (IllegalArgumentException e) {
                                startFuture.fail(e);
                                return;
                            }
                        }

                        if (https) {
                            ClientAuth clientAuth = getTlsClientAuth(httpConfig, httpBuildTimeConfig, launchMode);
                            container.instance(HttpCertificateUpdateEventListener.class).get()
                                    .register(event.result(),
                                            getHttpServerTlsConfigName(httpConfig, httpBuildTimeConfig, launchMode)
                                                    .orElse(TlsConfig.DEFAULT_NAME),
                                            "http server", clientAuth);
                        }

                        if (notifyStartObservers) {
                            Event<Object> startEvent = container.beanManager().getEvent();
                            if (https) {
                                startEvent.select(HttpsServerStart.class)
                                        .fireAsync(new HttpsServerStart(config, sslOptions));
                            } else {
                                startEvent.select(HttpServerStart.class).fireAsync(new HttpServerStart(config));
                            }
                        }

                        if (remainingCount.decrementAndGet() == 0) {
                            //make sure we only complete once
                            startFuture.complete(null);
                        }

                    }
                }

                private void validateHttpPorts(int httpPort, int httpsPort) {
                    if (httpsPort == httpPort) {
                        startFuture
                                .fail(new IllegalArgumentException("Both http and https servers started on port " + httpPort));
                    }
                }

                private URI localBaseUri(String scheme, int actualPort) {
                    SmallRyeConfig smallRyeConfig = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
                    String host = config.getTcpHost();
                    if (host.equals("0.0.0.0")) {
                        host = "localhost";
                    }
                    String rootPath = httpBuildTimeConfig.rootPath();
                    Optional<String> contextPath = smallRyeConfig.getOptionalValue("quarkus.servlet.context-path",
                            String.class);
                    StringBuilder path = new StringBuilder(rootPath);
                    if (!rootPath.endsWith("/")) {
                        path.append("/");
                    }
                    if (!rootPath.startsWith("/")) {
                        path.insert(0, "/");
                    }
                    if (contextPath.isPresent()) {
                        path.append(contextPath.get().startsWith("/") ? contextPath.get().substring(1) : contextPath.get());
                    }
                    if (path.charAt(path.length() - 1) == '/') {
                        path.deleteCharAt(path.length() - 1);
                    }
                    return URI.create(scheme + "://" + host + ":" + actualPort + path);
                }

                private void checkPortShadowedByAnotherProcess(String host, int port) {
                    if (!launchMode.isDevOrTest()) {
                        return;
                    }
                    if (!LOOPBACK_HOSTS.contains(host)) {
                        return;
                    }
                    InetAddress nonLoopback = findNonLoopbackAddress();
                    if (nonLoopback == null) {
                        LOGGER.debug("No non-loopback address found, skipping port shadow check");
                        return;
                    }
                    vertx.executeBlocking(() -> {
                        try (Socket socket = new Socket()) {
                            socket.connect(new InetSocketAddress(nonLoopback, port), PORT_SHADOW_CHECK_TIMEOUT_MS);
                            LOGGER.warnf("Port %d is also in use on network interface %s, possibly by a container. "
                                    + "Quarkus is listening on %s only, which may shadow the other process. "
                                    + "Use 'quarkus.http.host=0.0.0.0' to listen on all interfaces.",
                                    port, nonLoopback.getHostAddress(), host);
                        } catch (IOException e) {
                            LOGGER.debugf("Port shadow check on %s:%d found no conflict: %s",
                                    nonLoopback.getHostAddress(), port, e.getMessage());
                        }
                        return null;
                    });
                }

                private InetAddress findNonLoopbackAddress() {
                    try {
                        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                        while (interfaces.hasMoreElements()) {
                            NetworkInterface ni = interfaces.nextElement();
                            if (ni.isLoopback() || !ni.isUp()) {
                                continue;
                            }
                            Enumeration<InetAddress> addresses = ni.getInetAddresses();
                            while (addresses.hasMoreElements()) {
                                InetAddress addr = addresses.nextElement();
                                if (!addr.isLoopbackAddress()) {
                                    return addr;
                                }
                            }
                        }
                    } catch (SocketException e) {
                        LOGGER.debugf("Failed to enumerate network interfaces: %s", e.getMessage());
                    }
                    return null;
                }
            });
        }

        @Override
        public void stop(Promise<Void> stopFuture) {

            for (Long id : reloadingTasks) {
                TlsCertificateReloader.unschedule(vertx, id);
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
                httpServer.close().onComplete(handleClose);
            }
            if (httpsServer != null) {
                httpsServer.close().onComplete(handleClose);
            }
            if (domainSocketServer != null) {
                domainSocketServer.close().onComplete(handleClose);
            }
        }

        @Override
        public void beforeCheckpoint(org.crac.Context<? extends Resource> context) throws Exception {
            Promise<Void> p = Promise.promise();
            stop(p);
            p.future().toCompletionStage().toCompletableFuture().get();
        }

        @Override
        public void afterRestore(org.crac.Context<? extends Resource> context) throws Exception {
            Promise<Void> p = Promise.promise();
            // The verticle must be started by the event-loop thread; the thread calling
            // afterRestore will likely do so for all suspended verticles, and had we called
            // this directly the verticles would all share the same context (run on the same thread).
            this.context.runOnContext(nil -> start(p));
            p.future().toCompletionStage().toCompletableFuture().get();
        }

    }

    protected static ServerBootstrap virtualBootstrap;
    protected static ChannelFuture virtualBootstrapChannel;
    public static VirtualAddress VIRTUAL_HTTP = new VirtualAddress("netty-virtual-http");

    private static void initializeVirtual(Vertx vertxRuntime) {
        if (virtualBootstrap != null) {
            return;
        }

        VertxInternal vertx = (VertxInternal) vertxRuntime;
        virtualBootstrap = new ServerBootstrap();
        virtualBootstrap.group(vertx.eventLoopGroup())
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
                        // We are already on a Netty Event loop, just ask Vert.x to create a context from it.
                        // This is the root context used by the HTTP connection (read and write MUST be done from
                        // THAT event loop).
                        ContextInternal rootContext = vertx.getOrCreateContext();
                        HttpServerOptions options = createVirtualHttpServerOptions();
                        VertxHandler<Http1ServerConnection> handler = VertxHandler.create(chctx -> {

                            Http1ServerConnection conn = new Http1ServerConnection(
                                    io.vertx.core.ThreadingModel.EVENT_LOOP,
                                    () -> {
                                        ContextInternal duplicated = (ContextInternal) VertxContext
                                                .getOrCreateDuplicatedContext(rootContext);
                                        setContextSafe(duplicated, true);
                                        return duplicated;
                                    },
                                    false,
                                    options.isHandle100ContinueAutomatically(),
                                    null,
                                    null,
                                    options.getMaxFormAttributeSize(),
                                    options.getMaxFormFields(),
                                    options.getMaxFormBufferedBytes(),
                                    new QueryParamDecoderConfig(),
                                    options.getHttp1Config() != null ? options.getHttp1Config() : new Http1ServerConfig(),
                                    options.isRegisterWebSocketWriteHandlers(),
                                    options.getWebSocketConfig() != null ? options.getWebSocketConfig()
                                            : new WebSocketServerConfig(),
                                    chctx,
                                    rootContext,
                                    "localhost",
                                    options.getTracingPolicy(),
                                    null,
                                    null);
                            conn.handler(ACTUAL_ROOT);
                            return conn;
                        });

                        ch.pipeline().addLast("handler", handler);
                    }

                    private static HttpServerOptions createVirtualHttpServerOptions() {
                        var result = new HttpServerOptions();
                        Optional<MemorySize> maybeMaxHeadersSize = ConfigProvider.getConfig()
                                .getOptionalValue("quarkus.http.limits.max-header-size", MemorySize.class);
                        if (maybeMaxHeadersSize.isPresent()) {
                            result.setMaxHeaderSize(maybeMaxHeadersSize.get().asIntValue());
                        }
                        return result;
                    }
                });

        // Start the server.
        try {
            virtualBootstrapChannel = virtualBootstrap.bind(VIRTUAL_HTTP).sync();
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

    private static boolean isGrpc(RoutingContext rc) {
        HttpServerRequest request = rc.request();
        HttpVersion version = request.version();
        if (HttpVersion.HTTP_1_0.equals(version) || HttpVersion.HTTP_1_1.equals(version)) {
            LOGGER.debugf("Expecting %s, received %s - not a gRPC request", HttpVersion.HTTP_2, version);
            return false;
        }
        String header = request.getHeader("content-type");
        return header != null && header.toLowerCase(Locale.ROOT).startsWith("application/grpc"); //See https://chromium.googlesource.com/external/github.com/grpc/grpc/+/HEAD/doc/PROTOCOL-HTTP2.md
    }

    private static Handler<RoutingContext> configureAndGetBody(Optional<MemorySize> maxBodySize, BodyConfig bodyConfig) {
        BodyHandler bodyHandler = BodyHandler.create();
        if (maxBodySize.isPresent()) {
            bodyHandler.setBodyLimit(maxBodySize.get().asLongValue());
        }
        bodyHandler.setHandleFileUploads(bodyConfig.handleFileUploads());
        bodyHandler.setUploadsDirectory(bodyConfig.uploadsDirectory());
        bodyHandler.setDeleteUploadedFilesOnEnd(bodyConfig.deleteUploadedFilesOnEnd());
        bodyHandler.setMergeFormAttributes(bodyConfig.mergeFormAttributes());
        bodyHandler.setPreallocateBodyBuffer(bodyConfig.preallocateBodyBuffer());
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                // Skip gRPC content
                if (isGrpc(event)) {
                    event.next();
                    return;
                }

                if (!Context.isOnEventLoopThread()) {
                    ((ConnectionBase) event.request().connection()).channel().eventLoop().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //this can happen if blocking authentication is involved for get requests
                                if (!event.request().isEnded()) {
                                    event.request().resume();
                                    if (StaticDataHolder.CAN_HAVE_BODY.contains(event.request().method())) {
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
                    if (StaticDataHolder.CAN_HAVE_BODY.contains(event.request().method())) {
                        bodyHandler.handle(event);
                    } else {
                        event.next();
                    }
                }
            }
        };
    }

    public Handler<RoutingContext> createBodyHandler() {
        Optional<MemorySize> maxBodySize = httpConfig.getValue().limits().maxBodySize();
        return configureAndGetBody(maxBodySize, httpConfig.getValue().body());
    }

    public Handler<RoutingContext> createBodyHandlerForManagementInterface() {
        Optional<MemorySize> maxBodySize = managementConfig.getValue().limits().maxBodySize();
        return configureAndGetBody(maxBodySize, managementConfig.getValue().body());
    }

    private BiConsumer<Cookie, HttpServerRequest> processSameSiteConfig(Map<String, SameSiteCookieConfig> cookieConfig) {

        List<BiFunction<Cookie, HttpServerRequest, Boolean>> functions = new ArrayList<>();
        BiFunction<Cookie, HttpServerRequest, Boolean> last = null;

        for (Map.Entry<String, SameSiteCookieConfig> entry : new TreeMap<>(cookieConfig).entrySet()) {
            Pattern p = Pattern.compile(entry.getKey(), entry.getValue().caseSensitive() ? 0 : Pattern.CASE_INSENSITIVE);
            BiFunction<Cookie, HttpServerRequest, Boolean> biFunction = new BiFunction<Cookie, HttpServerRequest, Boolean>() {
                @Override
                public Boolean apply(Cookie cookie, HttpServerRequest request) {
                    if (p.matcher(cookie.getName()).matches()) {
                        if (entry.getValue().value() == CookieSameSite.NONE) {
                            if (entry.getValue().enableClientChecker()) {
                                String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
                                if (userAgent != null
                                        && SameSiteNoneIncompatibleClientChecker.isSameSiteNoneIncompatible(userAgent)) {
                                    return false;
                                }
                            }
                            if (entry.getValue().addSecureForNone()) {
                                cookie.setSecure(true);
                            }
                        }
                        cookie.setSameSite(entry.getValue().value());
                        return true;
                    }
                    return false;
                }
            };
            if (entry.getKey().equals(".*")) {
                //a bit of a hack to make sure the pattern .* is evaluated last
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

    public static class AlwaysCreateBodyHandlerSupplier implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            return true;
        }
    }

    private static class HotReplacementRoutingContextHandler implements Handler<RoutingContext> {
        private final ClassLoader currentCl;

        public HotReplacementRoutingContextHandler(ClassLoader currentCl) {
            this.currentCl = currentCl;
        }

        @Override
        public void handle(RoutingContext event) {
            Thread.currentThread().setContextClassLoader(currentCl);
            hotReplacementHandler.handle(event);
        }
    }

    private static class StaticDataHolder {

        private static final List<HttpMethod> CAN_HAVE_BODY = Arrays.asList(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH,
                HttpMethod.DELETE);
    }
}
