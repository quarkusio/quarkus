package io.quarkus.vertx.http.runtime;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;
import static io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils.RANDOM_PORT_MAIN_HTTP;
import static io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils.RANDOM_PORT_MANAGEMENT;
import static io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils.getInsecureRequestStrategy;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.logging.LogBuildTimeConfig;
import io.quarkus.runtime.shutdown.ShutdownConfig;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.tls.runtime.config.TlsConfig;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.quarkus.vertx.http.DomainSocketServerStart;
import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.quarkus.vertx.http.HttpServerStart;
import io.quarkus.vertx.http.HttpsServerStart;
import io.quarkus.vertx.http.ManagementInterface;
import io.quarkus.vertx.http.runtime.VertxHttpConfig.InsecureRequests;
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
import io.vertx.core.Closeable;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.Http1xServerConnection;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.Utils;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.core.net.impl.VertxHandler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

@Recorder
public class VertxHttpRecorder {

    /**
     * The key that the request start time is stored under
     */
    public static final String REQUEST_START_TIME = "io.quarkus.request-start-time";

    public static final String MAX_REQUEST_SIZE_KEY = "io.quarkus.max-request-size";

    private static final String DISABLE_WEBSOCKETS_PROP_NAME = "vertx.disableWebsockets";

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
            if (DISABLE_URI_VALIDATION) {
                return true;
            }
            try {
                // we simply need to know if the URI is valid
                new URI(httpServerRequest.uri());
                return true;
            } catch (URISyntaxException e) {
                return false;
            }
        }
    };
    private static HttpServerOptions httpMainSslServerOptions;
    private static HttpServerOptions httpMainServerOptions;
    private static HttpServerOptions httpMainDomainSocketOptions;
    private static HttpServerOptions httpManagementServerOptions;

    private static final List<Long> refresTaskIds = new CopyOnWriteArrayList<>();

    final VertxHttpBuildTimeConfig httpBuildTimeConfig;
    final ManagementInterfaceBuildTimeConfig managementBuildTimeConfig;
    final RuntimeValue<VertxHttpConfig> httpConfig;
    final RuntimeValue<ManagementConfig> managementConfig;
    final RuntimeValue<ShutdownConfig> shutdownConfig;

    private static volatile Handler<HttpServerRequest> managementRouter;
    private static volatile Handler<HttpServerRequest> managementRouterDelegate;

    public VertxHttpRecorder(
            VertxHttpBuildTimeConfig httpBuildTimeConfig,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            RuntimeValue<VertxHttpConfig> httpConfig,
            RuntimeValue<ManagementConfig> managementConfig,
            RuntimeValue<ShutdownConfig> shutdownConfig) {
        this.httpBuildTimeConfig = httpBuildTimeConfig;
        this.httpConfig = httpConfig;
        this.managementBuildTimeConfig = managementBuildTimeConfig;
        this.managementConfig = managementConfig;
        this.shutdownConfig = shutdownConfig;
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

            var insecureRequestStrategy = getInsecureRequestStrategy(httpBuildConfig, httpConfig.insecureRequests());
            //we can't really do
            doServerStart(vertx, httpBuildConfig, managementBuildTimeConfig, null, httpConfig, managementConfig,
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
                var insecureRequestStrategy = getInsecureRequestStrategy(httpBuildTimeConfig,
                        httpConfiguration.insecureRequests());
                doServerStart(vertx.get(), httpBuildTimeConfig, managementBuildTimeConfig, managementRouter,
                        httpConfiguration, managementConfig, launchMode, ioThreads, websocketSubProtocols,
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
        mainRouter.getValue().mountSubRouter(frameworkPath, frameworkRouter.getValue());
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
            mainRouter.mountSubRouter(rootPath, httpRouteRouter);

            addHotReplacementHandlerIfNeeded(mainRouter);
            root = mainRouter;
        }

        warnIfProxyAddressForwardingAllowedWithMultipleHeaders(httpConfig.proxy());
        root = HttpServerCommonHandlers.applyProxy(httpConfig.proxy(), root, vertx);

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

            mr.route().order(RouteConstants.ROUTE_ORDER_BODY_HANDLER_MANAGEMENT)
                    .handler(createBodyHandlerForManagementInterface());
            // We can use "*" here as the management interface is not expected to be used publicly.
            mr.route().order(RouteConstants.ROUTE_ORDER_CORS_MANAGEMENT).handler(CorsHandler.create().addOrigin("*"));

            HttpServerCommonHandlers.applyFilters(managementConfig.getValue().filter(), mr);
            for (Filter filter : managementInterfaceFilterList) {
                mr.route().order(filter.getPriority()).handler(filter.getHandler());
            }

            HttpServerCommonHandlers.applyHeaders(managementConfig.getValue().header(), mr);
            applyCompression(managementBuildTimeConfig.enableCompression(), mr);

            Handler<HttpServerRequest> handler = HttpServerCommonHandlers.enforceDuplicatedContext(mr, mustResumeRequest);
            handler = HttpServerCommonHandlers.applyProxy(managementConfig.getValue().proxy(), handler, vertx);

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
            frameworkRouter.getValue().route().order(RouteConstants.ROUTE_ORDER_ACCESS_LOG_HANDLER).handler(handler);
        } else {
            httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_ACCESS_LOG_HANDLER).handler(handler);
            frameworkRouter.getValue().route().order(RouteConstants.ROUTE_ORDER_ACCESS_LOG_HANDLER).handler(handler);
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

        HttpServerOptions domainSocketOptionsForManagement = createDomainSocketOptionsForManagementInterface(
                managementBuildTimeConfig, managementConfig,
                websocketSubProtocols);
        if (domainSocketOptionsForManagement != null) {
            vertx.createHttpServer(domainSocketOptionsForManagement)
                    .requestHandler(managementRouter)
                    .listen(ar -> {
                        if (ar.failed()) {
                            managementInterfaceDomainSocketFuture.completeExceptionally(
                                    new IllegalStateException(
                                            "Unable to start the management interface on the "
                                                    + domainSocketOptionsForManagement.getHost() + " domain socket",
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

    private static CompletableFuture<HttpServer> initializeManagementInterface(Vertx vertx,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig, Handler<HttpServerRequest> managementRouter,
            ManagementConfig managementConfig,
            LaunchMode launchMode,
            List<String> websocketSubProtocols, TlsConfigurationRegistry registry) throws IOException {
        httpManagementServerOptions = null;
        CompletableFuture<HttpServer> managementInterfaceFuture = new CompletableFuture<>();
        if (!managementBuildTimeConfig.enabled() || managementRouter == null || managementConfig == null) {
            managementInterfaceFuture.complete(null);
            return managementInterfaceFuture;
        }

        HttpServerOptions httpServerOptionsForManagement = createHttpServerOptionsForManagementInterface(
                managementBuildTimeConfig, managementConfig, launchMode,
                websocketSubProtocols);
        httpManagementServerOptions = HttpServerOptionsUtils.createSslOptionsForManagementInterface(
                managementBuildTimeConfig, managementConfig, launchMode,
                websocketSubProtocols, registry);
        if (httpManagementServerOptions != null && httpManagementServerOptions.getKeyCertOptions() == null) {
            httpManagementServerOptions = httpServerOptionsForManagement;
        }

        if (httpManagementServerOptions != null) {
            vertx.createHttpServer(httpManagementServerOptions)
                    .requestHandler(managementRouter)
                    .listen(ar -> {
                        if (ar.failed()) {
                            managementInterfaceFuture.completeExceptionally(
                                    new IllegalStateException("Unable to start the management interface on "
                                            + httpManagementServerOptions.getHost() + ":"
                                            + httpManagementServerOptions.getPort(), ar.cause()));
                        } else {
                            if (httpManagementServerOptions.isSsl()
                                    && (managementConfig.ssl().certificate().reloadPeriod().isPresent())) {
                                try {
                                    long l = TlsCertificateReloader.initCertReloadingAction(
                                            vertx, ar.result(), httpManagementServerOptions, managementConfig.ssl(), registry,
                                            managementConfig.tlsConfigurationName());
                                    if (l != -1) {
                                        refresTaskIds.add(l);
                                    }
                                } catch (IllegalArgumentException e) {
                                    managementInterfaceFuture.completeExceptionally(e);
                                    return;
                                }
                            }

                            if (httpManagementServerOptions.isSsl()) {
                                CDI.current().select(HttpCertificateUpdateEventListener.class).get()
                                        .register(ar.result(),
                                                managementConfig.tlsConfigurationName().orElse(TlsConfig.DEFAULT_NAME),
                                                "management interface");
                            }

                            actualManagementPort = ar.result().actualPort();
                            if (actualManagementPort != httpManagementServerOptions.getPort()) {
                                var managementPortSystemProperties = new PortSystemProperties();
                                managementPortSystemProperties.set("management", actualManagementPort, launchMode);
                                ((VertxInternal) vertx).addCloseHook(new Closeable() {
                                    @Override
                                    public void close(Promise<Void> completion) {
                                        managementPortSystemProperties.restore();
                                        completion.complete();
                                    }
                                });
                            }
                            managementInterfaceFuture.complete(ar.result());
                        }
                    });

        } else {
            managementInterfaceFuture.complete(null);
        }
        return managementInterfaceFuture;
    }

    private static CompletableFuture<String> initializeMainHttpServer(Vertx vertx, VertxHttpBuildTimeConfig httpBuildTimeConfig,
            VertxHttpConfig httpConfig,
            LaunchMode launchMode,
            Supplier<Integer> eventLoops, List<String> websocketSubProtocols, InsecureRequests insecureRequestStrategy,
            TlsConfigurationRegistry registry)
            throws IOException {

        if (!httpConfig.hostEnabled() && !httpConfig.domainSocketEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        // Http server configuration
        httpMainServerOptions = createHttpServerOptions(httpBuildTimeConfig, httpConfig, launchMode,
                websocketSubProtocols);
        httpMainDomainSocketOptions = createDomainSocketOptions(httpBuildTimeConfig, httpConfig,
                websocketSubProtocols);
        HttpServerOptions tmpSslConfig = HttpServerOptionsUtils.createSslOptions(httpBuildTimeConfig, httpConfig,
                launchMode, websocketSubProtocols, registry);

        // Customize
        ArcContainer container = Arc.container();
        if (container != null) {
            List<InstanceHandle<HttpServerOptionsCustomizer>> instances = container
                    .listAll(HttpServerOptionsCustomizer.class);
            for (InstanceHandle<HttpServerOptionsCustomizer> instance : instances) {
                HttpServerOptionsCustomizer customizer = instance.get();
                if (httpMainServerOptions != null) {
                    customizer.customizeHttpServer(httpMainServerOptions);
                }
                if (tmpSslConfig != null) {
                    customizer.customizeHttpsServer(tmpSslConfig);
                }
                if (httpMainDomainSocketOptions != null) {
                    customizer.customizeDomainSocketServer(httpMainDomainSocketOptions);
                }
            }
        }

        // Disable TLS if certificate options are still missing after customize hooks.
        if (tmpSslConfig != null && tmpSslConfig.getKeyCertOptions() == null) {
            tmpSslConfig = null;
        }
        httpMainSslServerOptions = tmpSslConfig;

        if (insecureRequestStrategy != InsecureRequests.ENABLED
                && httpMainSslServerOptions == null) {
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

        // Note that a new HttpServer is created for each IO thread but we only want to fire the events (HttpServerStart etc.) once,
        // for the first server that started listening
        // See https://vertx.io/docs/vertx-core/java/#_server_sharing for more information
        AtomicBoolean startEventsFired = new AtomicBoolean();

        vertx.deployVerticle(new Supplier<>() {
            @Override
            public Verticle get() {
                return new WebDeploymentVerticle(httpMainServerOptions, httpMainSslServerOptions, httpMainDomainSocketOptions,
                        launchMode, insecureRequestStrategy, httpConfig, connectionCount, registry, startEventsFired);
            }
        }, new DeploymentOptions().setInstances(ioThreads), new Handler<>() {
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

    private static void doServerStart(Vertx vertx, VertxHttpBuildTimeConfig httpBuildTimeConfig,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig, Handler<HttpServerRequest> managementRouter,
            VertxHttpConfig httpConfig, ManagementConfig managementConfig,
            LaunchMode launchMode,
            Supplier<Integer> eventLoops, List<String> websocketSubProtocols,
            InsecureRequests insecureRequestStrategy,
            boolean auxiliaryApplication) throws IOException {

        TlsConfigurationRegistry registry = null;
        if (Arc.container() != null) {
            registry = Arc.container().select(TlsConfigurationRegistry.class).orNull();
        }

        var mainServerFuture = initializeMainHttpServer(vertx, httpBuildTimeConfig, httpConfig, launchMode, eventLoops,
                websocketSubProtocols, insecureRequestStrategy, registry);
        var managementInterfaceFuture = initializeManagementInterface(vertx, managementBuildTimeConfig, managementRouter,
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
                                vertx.undeploy(deploymentIdIfAny, handler);
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
                                managementServer.close(handler);
                            }
                            if (managementServerDomainSocket != null && !isVertxClose) {
                                managementServerDomainSocket.close(handler);
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

        setHttpServerTiming(insecureRequestStrategy == InsecureRequests.DISABLED, httpMainServerOptions,
                httpMainSslServerOptions,
                httpMainDomainSocketOptions,
                auxiliaryApplication, httpManagementServerOptions);
    }

    private static void setHttpServerTiming(boolean httpDisabled, HttpServerOptions httpServerOptions,
            HttpServerOptions sslConfig,
            HttpServerOptions domainSocketOptions, boolean auxiliaryApplication, HttpServerOptions managementConfig) {
        StringBuilder serverListeningMessage = new StringBuilder("Listening on: ");
        int socketCount = 0;

        if (!httpDisabled && httpServerOptions != null) {
            serverListeningMessage.append(String.format(
                    "http://%s:%s", getDeveloperFriendlyHostName(httpServerOptions), actualHttpPort));
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

        if (domainSocketOptions != null) {
            if (socketCount > 0) {
                serverListeningMessage.append(" and ");
            }
            serverListeningMessage.append(String.format("unix:%s", getDeveloperFriendlyHostName(domainSocketOptions)));
        }
        if (managementConfig != null) {
            serverListeningMessage.append(
                    String.format(". Management interface listening on http%s://%s:%s.", managementConfig.isSsl() ? "s" : "",
                            getDeveloperFriendlyHostName(managementConfig), actualManagementPort));
        }

        Timing.setHttpServer(serverListeningMessage.toString(), auxiliaryApplication);
    }

    /**
     * To improve developer experience in WSL dev/test mode, the server listening message should print "localhost" when
     * the host is set to "0.0.0.0". Otherwise, display the actual host.
     * Do not use this during the actual configuration, use options.getHost() there directly instead.
     */
    private static String getDeveloperFriendlyHostName(HttpServerOptions options) {
        return (LaunchMode.current().isDevOrTest() && "0.0.0.0".equals(options.getHost()) && isWSL()) ? "localhost"
                : options.getHost();
    }

    /**
     * @return {@code true} if the application is running in a WSL (Windows Subsystem for Linux) environment
     */
    private static boolean isWSL() {
        var sysEnv = System.getenv();
        return sysEnv.containsKey("IS_WSL") || sysEnv.containsKey("WSL_DISTRO_NAME");
    }

    private static HttpServerOptions createHttpServerOptions(
            VertxHttpBuildTimeConfig buildTimeConfig, VertxHttpConfig httpConfig,
            LaunchMode launchMode, List<String> websocketSubProtocols) {
        if (!httpConfig.hostEnabled()) {
            return null;
        }
        // TODO other config properties
        HttpServerOptions options = new HttpServerOptions();
        int port = httpConfig.determinePort(launchMode);
        options.setPort(port == 0 ? RANDOM_PORT_MAIN_HTTP : port);

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, websocketSubProtocols);

        httpConfig.websocketServer().maxFrameSize().ifPresent(s -> options.setMaxWebSocketFrameSize(s));
        httpConfig.websocketServer().maxMessageSize().ifPresent(s -> options.setMaxWebSocketMessageSize(s));

        return options;
    }

    private static HttpServerOptions createHttpServerOptionsForManagementInterface(
            ManagementInterfaceBuildTimeConfig buildTimeConfig, ManagementConfig httpConfig,
            LaunchMode launchMode, List<String> websocketSubProtocols) {
        if (!httpConfig.hostEnabled()) {
            return null;
        }
        HttpServerOptions options = new HttpServerOptions();
        int port = httpConfig.determinePort(launchMode);
        options.setPort(port == 0 ? RANDOM_PORT_MANAGEMENT : port);

        HttpServerOptionsUtils.applyCommonOptionsForManagementInterface(options, buildTimeConfig, httpConfig,
                websocketSubProtocols);

        return options;
    }

    private static HttpServerOptions createDomainSocketOptions(
            VertxHttpBuildTimeConfig buildTimeConfig, VertxHttpConfig httpConfig,
            List<String> websocketSubProtocols) {
        if (!httpConfig.domainSocketEnabled()) {
            return null;
        }
        HttpServerOptions options = new HttpServerOptions();

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfig, websocketSubProtocols);
        // Override the host (0.0.0.0 by default) with the configured domain socket.
        options.setHost(httpConfig.domainSocket());

        // Check if we can write into the domain socket directory
        // We can do this check using a blocking API as the execution is done from the main thread (not an I/O thread)
        File file = new File(httpConfig.domainSocket());
        if (!file.getParentFile().canWrite()) {
            LOGGER.warnf(
                    "Unable to write in the domain socket directory (`%s`). Binding to the socket is likely going to fail.",
                    httpConfig.domainSocket());
        }

        return options;
    }

    private static HttpServerOptions createDomainSocketOptionsForManagementInterface(
            ManagementInterfaceBuildTimeConfig buildTimeConfig, ManagementConfig managementConfig,
            List<String> websocketSubProtocols) {
        if (!managementConfig.domainSocketEnabled()) {
            return null;
        }
        HttpServerOptions options = new HttpServerOptions();

        HttpServerOptionsUtils.applyCommonOptionsForManagementInterface(options, buildTimeConfig, managementConfig,
                websocketSubProtocols);
        // Override the host (0.0.0.0 by default) with the configured domain socket.
        options.setHost(managementConfig.domainSocket());

        // Check if we can write into the domain socket directory
        // We can do this check using a blocking API as the execution is done from the main thread (not an I/O thread)
        File file = new File(managementConfig.domainSocket());
        if (!file.getParentFile().canWrite()) {
            LOGGER.warnf(
                    "Unable to write in the domain socket directory (`%s`). Binding to the socket is likely going to fail.",
                    managementConfig.domainSocket());
        }

        return options;
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

    private static class WebDeploymentVerticle extends AbstractVerticle implements Resource {

        private final TlsConfigurationRegistry registry;
        private HttpServer httpServer;
        private HttpServer httpsServer;
        private HttpServer domainSocketServer;
        private final HttpServerOptions httpOptions;
        private final HttpServerOptions httpsOptions;
        private final HttpServerOptions domainSocketOptions;
        private final LaunchMode launchMode;
        private volatile boolean clearHttpProperty = false;
        private volatile boolean clearHttpsProperty = false;
        private volatile PortSystemProperties portSystemProperties;
        private final InsecureRequests insecureRequests;
        private final VertxHttpConfig quarkusConfig;
        private final AtomicInteger connectionCount;
        private final List<Long> reloadingTasks = new CopyOnWriteArrayList<>();
        private final AtomicBoolean startEventsFired;

        public WebDeploymentVerticle(HttpServerOptions httpOptions, HttpServerOptions httpsOptions,
                HttpServerOptions domainSocketOptions, LaunchMode launchMode,
                InsecureRequests insecureRequests, VertxHttpConfig httpConfig, AtomicInteger connectionCount,
                TlsConfigurationRegistry registry, AtomicBoolean startEventsFired) {
            this.httpOptions = httpOptions;
            this.httpsOptions = httpsOptions;
            this.launchMode = launchMode;
            this.domainSocketOptions = domainSocketOptions;
            this.insecureRequests = insecureRequests;
            this.quarkusConfig = httpConfig;
            this.connectionCount = connectionCount;
            this.registry = registry;
            this.startEventsFired = startEventsFired;
            org.crac.Core.getGlobalContext().register(this);
        }

        @Override
        public void start(Promise<Void> startFuture) {
            assert Context.isOnEventLoopThread();

            final AtomicInteger remainingCount = new AtomicInteger(0);
            boolean httpServerEnabled = httpOptions != null && insecureRequests != InsecureRequests.DISABLED;
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

            ArcContainer container = Arc.container();
            boolean notifyStartObservers = container != null ? startEventsFired.compareAndSet(false, true) : false;

            if (httpServerEnabled) {
                httpServer = vertx.createHttpServer(httpOptions);
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
                                                    "https://" + host + ":" + httpsOptions.getPort() + req.uri())
                                            .end();
                                }
                            } catch (Exception e) {
                                req.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).end();
                            }
                        }
                    });
                }
                setupTcpHttpServer(httpServer, httpOptions, false, startFuture, remainingCount, connectionCount,
                        container, notifyStartObservers);
            }

            if (domainSocketOptions != null) {
                domainSocketServer = vertx.createHttpServer(domainSocketOptions);
                domainSocketServer.requestHandler(ACTUAL_ROOT);
                setupUnixDomainSocketHttpServer(domainSocketServer, domainSocketOptions, startFuture, remainingCount,
                        container, notifyStartObservers);
            }

            if (httpsOptions != null) {
                httpsServer = vertx.createHttpServer(httpsOptions);
                httpsServer.requestHandler(ACTUAL_ROOT);
                setupTcpHttpServer(httpsServer, httpsOptions, true, startFuture, remainingCount, connectionCount,
                        container, notifyStartObservers);
            }
        }

        private void setupUnixDomainSocketHttpServer(HttpServer httpServer, HttpServerOptions options,
                Promise<Void> startFuture,
                AtomicInteger remainingCount, ArcContainer container, boolean notifyStartObservers) {
            httpServer.listen(SocketAddress.domainSocketAddress(options.getHost()), event -> {
                if (event.succeeded()) {
                    if (notifyStartObservers) {
                        container.beanManager().getEvent().select(DomainSocketServerStart.class)
                                .fireAsync(new DomainSocketServerStart(options));
                    }
                    if (remainingCount.decrementAndGet() == 0) {
                        startFuture.complete(null);
                    }
                } else {
                    if (event.cause() != null && event.cause().getMessage() != null
                            && event.cause().getMessage().contains("Permission denied")) {
                        startFuture.fail(new IllegalStateException(
                                String.format(
                                        "Unable to bind to Unix domain socket (%s) as the application does not have the permission to write in the directory.",
                                        domainSocketOptions.getHost())));
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

        private void setupTcpHttpServer(HttpServer httpServer, HttpServerOptions options, boolean https,
                Promise<Void> startFuture, AtomicInteger remainingCount, AtomicInteger currentConnectionCount,
                ArcContainer container, boolean notifyStartObservers) {

            if (quarkusConfig.limits().maxConnections().isPresent() && quarkusConfig.limits().maxConnections().getAsInt() > 0) {
                var tracker = vertx.isMetricsEnabled()
                        ? ((ExtendedQuarkusVertxHttpMetrics) ((VertxInternal) vertx).metricsSPI()).getHttpConnectionTracker()
                        : ExtendedQuarkusVertxHttpMetrics.NOOP_CONNECTION_TRACKER;

                final int maxConnections = quarkusConfig.limits().maxConnections().getAsInt();
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
            httpServer.listen(options.getPort(), options.getHost(), new Handler<>() {
                @Override
                public void handle(AsyncResult<HttpServer> event) {
                    if (event.cause() != null) {
                        if (event.cause() instanceof BindException e) {
                            startFuture.fail(new QuarkusBindException(options.getHost(), options.getPort(), e));
                        } else {
                            startFuture.fail(event.cause());
                        }
                    } else {
                        // Port may be random, so set the actual port
                        int actualPort = event.result().actualPort();

                        if (https) {
                            actualHttpsPort = actualPort;
                            validateHttpPorts(actualHttpPort, actualHttpsPort);
                        } else {
                            actualHttpPort = actualPort;
                            validateHttpPorts(actualHttpPort, actualHttpsPort);
                        }
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
                            portSystemProperties = new PortSystemProperties();
                            portSystemProperties.set(schema, actualPort, launchMode);
                        }

                        if (https && (quarkusConfig.ssl().certificate().reloadPeriod().isPresent())) {
                            try {
                                long l = TlsCertificateReloader.initCertReloadingAction(
                                        vertx, httpsServer, httpsOptions, quarkusConfig.ssl(), registry,
                                        quarkusConfig.tlsConfigurationName());
                                if (l != -1) {
                                    reloadingTasks.add(l);
                                }
                            } catch (IllegalArgumentException e) {
                                startFuture.fail(e);
                                return;
                            }
                        }

                        if (https) {
                            container.instance(HttpCertificateUpdateEventListener.class).get()
                                    .register(event.result(),
                                            quarkusConfig.tlsConfigurationName().orElse(TlsConfig.DEFAULT_NAME),
                                            "http server");
                        }

                        if (notifyStartObservers) {
                            Event<Object> startEvent = container.beanManager().getEvent();
                            if (https) {
                                startEvent.select(HttpsServerStart.class).fireAsync(new HttpsServerStart(options));
                            } else {
                                startEvent.select(HttpServerStart.class).fireAsync(new HttpServerStart(options));
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

                    if (clearHttpProperty) {
                        String portPropertyName = launchMode == LaunchMode.TEST ? "quarkus.http.test-port"
                                : "quarkus.http.port";
                        System.clearProperty(portPropertyName);
                        if (launchMode.isDevOrTest()) {
                            System.clearProperty(propertyWithProfilePrefix(portPropertyName));
                        }

                    }
                    if (clearHttpsProperty) {
                        String portPropertyName = launchMode == LaunchMode.TEST ? "quarkus.http.test-ssl-port"
                                : "quarkus.http.ssl-port";
                        System.clearProperty(portPropertyName);
                        if (launchMode.isDevOrTest()) {
                            System.clearProperty(propertyWithProfilePrefix(portPropertyName));
                        }
                    }
                    if (portSystemProperties != null) {
                        portSystemProperties.restore();
                    }

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
                        ContextInternal rootContext = vertx.createEventLoopContext();
                        VertxHandler<Http1xServerConnection> handler = VertxHandler.create(chctx -> {

                            Http1xServerConnection conn = new Http1xServerConnection(
                                    () -> {
                                        ContextInternal duplicated = (ContextInternal) VertxContext
                                                .getOrCreateDuplicatedContext(rootContext);
                                        setContextSafe(duplicated, true);
                                        return duplicated;
                                    },
                                    null,
                                    createVirtualHttpServerOptions(),
                                    chctx,
                                    rootContext,
                                    "localhost",
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
                            result.setMaxHeaderSize(maybeMaxHeadersSize.get().asBigInteger().intValueExact());
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

    public Handler<RoutingContext> createBodyHandler() {
        Optional<MemorySize> maxBodySize = httpConfig.getValue().limits().maxBodySize();
        return configureAndGetBody(maxBodySize, httpConfig.getValue().body());
    }

    public Handler<RoutingContext> createBodyHandlerForManagementInterface() {
        Optional<MemorySize> maxBodySize = managementConfig.getValue().limits().maxBodySize();
        return configureAndGetBody(maxBodySize, managementConfig.getValue().body());
    }

    private static final List<HttpMethod> CAN_HAVE_BODY = Arrays.asList(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH,
            HttpMethod.DELETE);

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
}
