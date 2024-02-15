package io.quarkus.vertx.http.runtime;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;
import static io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils.getInsecureRequestStrategy;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.regex.Pattern;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Default;

import org.crac.Resource;
import org.jboss.logging.Logger;
import org.wildfly.common.cpu.ProcessorInfo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.netty.runtime.virtual.VirtualAddress;
import io.quarkus.netty.runtime.virtual.VirtualChannel;
import io.quarkus.netty.runtime.virtual.VirtualServerChannel;
import io.quarkus.runtime.*;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigInstantiator;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.MemorySize;
import io.quarkus.runtime.shutdown.ShutdownConfig;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.config.VertxConfiguration;
import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.quarkus.vertx.http.ManagementInterface;
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
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceConfiguration;
import io.quarkus.vertx.http.runtime.options.HttpServerCommonHandlers;
import io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils;
import io.quarkus.vertx.http.runtime.options.TlsCertificateReloadUtils;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
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

    private static final List<Long> taskIds = new CopyOnWriteArrayList<>();
    final HttpBuildTimeConfig httpBuildTimeConfig;
    final ManagementInterfaceBuildTimeConfig managementBuildTimeConfig;
    final RuntimeValue<HttpConfiguration> httpConfiguration;

    final RuntimeValue<ManagementInterfaceConfiguration> managementConfiguration;
    private static volatile Handler<HttpServerRequest> managementRouter;

    public VertxHttpRecorder(HttpBuildTimeConfig httpBuildTimeConfig,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            RuntimeValue<HttpConfiguration> httpConfiguration,
            RuntimeValue<ManagementInterfaceConfiguration> managementConfiguration) {
        this.httpBuildTimeConfig = httpBuildTimeConfig;
        this.httpConfiguration = httpConfiguration;
        this.managementBuildTimeConfig = managementBuildTimeConfig;
        this.managementConfiguration = managementConfiguration;
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
        if (supplier == null) {
            VertxConfiguration vertxConfiguration = ConfigUtils.emptyConfigBuilder()
                    .addDiscoveredSources()
                    .withMapping(VertxConfiguration.class)
                    .build().getConfigMapping(VertxConfiguration.class);

            ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig();
            ConfigInstantiator.handleObject(threadPoolConfig);

            vertx = VertxCoreRecorder.recoverFailedStart(vertxConfiguration, threadPoolConfig).get();
        } else {
            vertx = supplier.get();
        }

        try {
            HttpBuildTimeConfig buildConfig = new HttpBuildTimeConfig();
            ConfigInstantiator.handleObject(buildConfig);
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig = new ManagementInterfaceBuildTimeConfig();
            ConfigInstantiator.handleObject(managementBuildTimeConfig);
            HttpConfiguration config = new HttpConfiguration();
            ConfigInstantiator.handleObject(config);
            ManagementInterfaceConfiguration managementConfig = new ManagementInterfaceConfiguration();
            ConfigInstantiator.handleObject(managementConfig);
            if (config.host == null) {
                //HttpHostConfigSource does not come into play here
                config.host = "localhost";
            }
            Router router = Router.router(vertx);
            if (hotReplacementHandler != null) {
                router.route().order(RouteConstants.ROUTE_ORDER_HOT_REPLACEMENT).blockingHandler(hotReplacementHandler);
            }

            Handler<HttpServerRequest> root = router;
            LiveReloadConfig liveReloadConfig = new LiveReloadConfig();
            ConfigInstantiator.handleObject(liveReloadConfig);
            if (liveReloadConfig.password.isPresent()
                    && hotReplacementContext.getDevModeType() == DevModeType.REMOTE_SERVER_SIDE) {
                root = remoteSyncHandler = new RemoteSyncHandler(liveReloadConfig.password.get(), root, hotReplacementContext);
            }
            rootHandler = root;

            var insecureRequestStrategy = getInsecureRequestStrategy(buildConfig, config.insecureRequests);
            //we can't really do
            doServerStart(vertx, buildConfig, managementBuildTimeConfig, null, config, managementConfig, LaunchMode.DEVELOPMENT,
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
        HttpConfiguration httpConfiguration = this.httpConfiguration.getValue();
        ManagementInterfaceConfiguration managementConfig = this.managementConfiguration == null ? null
                : this.managementConfiguration.getValue();
        if (startSocket && (httpConfiguration.hostEnabled || httpConfiguration.domainSocketEnabled
                || (managementConfig != null && managementConfig.hostEnabled)
                || (managementConfig != null && managementConfig.domainSocketEnabled))) {
            // Start the server
            if (closeTask == null) {
                var insecureRequestStrategy = getInsecureRequestStrategy(httpBuildTimeConfig,
                        httpConfiguration.insecureRequests);
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

    public void finalizeRouter(BeanContainer container, Consumer<Route> defaultRouteHandler,
            List<Filter> filterList, List<Filter> managementInterfaceFilterList, Supplier<Vertx> vertx,
            LiveReloadConfig liveReloadConfig, Optional<RuntimeValue<Router>> mainRouterRuntimeValue,
            RuntimeValue<Router> httpRouterRuntimeValue, RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter,
            RuntimeValue<Router> frameworkRouter, RuntimeValue<Router> managementRouter,
            String rootPath, String nonRootPath,
            LaunchMode launchMode, boolean requireBodyHandler,
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

        applyCompression(httpBuildTimeConfig.enableCompression, httpRouteRouter);
        httpRouteRouter.route().last().failureHandler(
                new QuarkusErrorHandler(launchMode.isDevOrTest(), httpConfiguration.unhandledErrorContentTypeDefault));

        if (requireBodyHandler) {
            //if this is set then everything needs the body handler installed
            //TODO: config etc
            httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_BODY_HANDLER).handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext routingContext) {
                    routingContext.request().resume();
                    bodyHandler.handle(routingContext);
                }
            });
        }

        HttpServerCommonHandlers.enforceMaxBodySize(httpConfiguration.limits, httpRouteRouter);
        // Filter Configuration per path
        var filtersInConfig = httpConfiguration.filter;
        HttpServerCommonHandlers.applyFilters(filtersInConfig, httpRouteRouter);
        // Headers sent on any request, regardless of the response
        HttpServerCommonHandlers.applyHeaders(httpConfiguration.header, httpRouteRouter);

        Handler<HttpServerRequest> root;
        if (rootPath.equals("/")) {
            if (hotReplacementHandler != null) {
                //recorders are always executed in the current CL
                ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
                httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_HOT_REPLACEMENT)
                        .handler(new Handler<RoutingContext>() {
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
                mainRouter.route().order(RouteConstants.ROUTE_ORDER_HOT_REPLACEMENT).handler(new Handler<RoutingContext>() {
                    @Override
                    public void handle(RoutingContext event) {
                        Thread.currentThread().setContextClassLoader(currentCl);
                        hotReplacementHandler.handle(event);
                    }
                });
            }
            root = mainRouter;
        }

        warnIfProxyAddressForwardingAllowedWithMultipleHeaders(httpConfiguration.proxy);
        root = HttpServerCommonHandlers.applyProxy(httpConfiguration.proxy, root, vertx);

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
        root = HttpServerCommonHandlers.enforceDuplicatedContext(delegate);
        if (httpConfiguration.recordRequestStartTime) {
            httpRouteRouter.route().order(RouteConstants.ROUTE_ORDER_RECORD_START_TIME).handler(new Handler<RoutingContext>() {
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

        if (managementRouter != null && managementRouter.getValue() != null) {
            // Add body handler and cors handler
            var mr = managementRouter.getValue();

            mr.route().last().failureHandler(
                    new QuarkusErrorHandler(launchMode.isDevOrTest(), httpConfiguration.unhandledErrorContentTypeDefault));

            mr.route().order(RouteConstants.ROUTE_ORDER_BODY_HANDLER_MANAGEMENT)
                    .handler(createBodyHandlerForManagementInterface());
            // We can use "*" here as the management interface is not expected to be used publicly.
            mr.route().order(RouteConstants.ROUTE_ORDER_CORS_MANAGEMENT).handler(CorsHandler.create().addOrigin("*"));

            HttpServerCommonHandlers.applyFilters(managementConfiguration.getValue().filter, mr);
            for (Filter filter : managementInterfaceFilterList) {
                mr.route().order(filter.getPriority()).handler(filter.getHandler());
            }

            HttpServerCommonHandlers.applyHeaders(managementConfiguration.getValue().header, mr);
            applyCompression(managementBuildTimeConfig.enableCompression, mr);

            Handler<HttpServerRequest> handler = HttpServerCommonHandlers.enforceDuplicatedContext(mr);
            handler = HttpServerCommonHandlers.applyProxy(managementConfiguration.getValue().proxy, handler, vertx);

            event.select(ManagementInterface.class).fire(new ManagementInterfaceImpl(managementRouter.getValue()));

            VertxHttpRecorder.managementRouter = handler;
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
        boolean proxyAddressForwardingActivated = proxyConfig.proxyAddressForwarding;
        boolean forwardedActivated = proxyConfig.allowForwarded;
        boolean xForwardedActivated = proxyConfig.allowXForwarded.orElse(!forwardedActivated);

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
            ManagementInterfaceConfiguration managementConfig,
            List<String> websocketSubProtocols) {
        CompletableFuture<HttpServer> managementInterfaceDomainSocketFuture = new CompletableFuture<>();
        if (!managementBuildTimeConfig.enabled || managementRouter == null || managementConfig == null) {
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
            ManagementInterfaceConfiguration managementConfig,
            LaunchMode launchMode,
            List<String> websocketSubProtocols) throws IOException {
        httpManagementServerOptions = null;
        CompletableFuture<HttpServer> managementInterfaceFuture = new CompletableFuture<>();
        if (!managementBuildTimeConfig.enabled || managementRouter == null || managementConfig == null) {
            managementInterfaceFuture.complete(null);
            return managementInterfaceFuture;
        }

        HttpServerOptions httpServerOptionsForManagement = createHttpServerOptionsForManagementInterface(
                managementBuildTimeConfig, managementConfig, launchMode,
                websocketSubProtocols);
        httpManagementServerOptions = HttpServerOptionsUtils.createSslOptionsForManagementInterface(
                managementBuildTimeConfig, managementConfig, launchMode,
                websocketSubProtocols);
        if (httpManagementServerOptions != null && httpManagementServerOptions.getKeyCertOptions() == null) {
            httpManagementServerOptions = httpServerOptionsForManagement;
        }

        if (httpManagementServerOptions != null) {

            vertx.createHttpServer(httpManagementServerOptions)
                    .requestHandler(managementRouter)
                    .listen(ar -> {
                        if (ar.failed()) {
                            managementInterfaceFuture.completeExceptionally(
                                    new IllegalStateException("Unable to start the management interface", ar.cause()));
                        } else {
                            if (httpManagementServerOptions.isSsl()
                                    && managementConfig.ssl.certificate.reloadPeriod.isPresent()) {
                                long l = TlsCertificateReloadUtils.handleCertificateReloading(
                                        vertx, ar.result(), httpManagementServerOptions, managementConfig.ssl);
                                if (l != -1) {
                                    taskIds.add(l);
                                }
                            }

                            actualManagementPort = ar.result().actualPort();
                            managementInterfaceFuture.complete(ar.result());
                        }
                    });
        } else {
            managementInterfaceFuture.complete(null);
        }
        return managementInterfaceFuture;
    }

    private static CompletableFuture<String> initializeMainHttpServer(Vertx vertx, HttpBuildTimeConfig httpBuildTimeConfig,
            HttpConfiguration httpConfiguration,
            LaunchMode launchMode,
            Supplier<Integer> eventLoops, List<String> websocketSubProtocols, InsecureRequests insecureRequestStrategy)
            throws IOException {

        if (!httpConfiguration.hostEnabled && !httpConfiguration.domainSocketEnabled) {
            return CompletableFuture.completedFuture(null);
        }

        // Http server configuration
        httpMainServerOptions = createHttpServerOptions(httpBuildTimeConfig, httpConfiguration, launchMode,
                websocketSubProtocols);
        httpMainDomainSocketOptions = createDomainSocketOptions(httpBuildTimeConfig, httpConfiguration,
                websocketSubProtocols);
        HttpServerOptions tmpSslConfig = HttpServerOptionsUtils.createSslOptions(httpBuildTimeConfig, httpConfiguration,
                launchMode,
                websocketSubProtocols);

        // Customize
        if (Arc.container() != null) {
            List<InstanceHandle<HttpServerOptionsCustomizer>> instances = Arc.container()
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

        if (insecureRequestStrategy != HttpConfiguration.InsecureRequests.ENABLED
                && httpMainSslServerOptions == null) {
            throw new IllegalStateException("Cannot set quarkus.http.insecure-requests without enabling SSL.");
        }

        int eventLoopCount = eventLoops.get();
        final int ioThreads;
        if (httpConfiguration.ioThreads.isPresent()) {
            ioThreads = Math.min(httpConfiguration.ioThreads.getAsInt(), eventLoopCount);
        } else if (launchMode.isDevOrTest()) {
            ioThreads = Math.min(2, eventLoopCount); //Don't start ~100 threads to run a couple unit tests
        } else {
            ioThreads = eventLoopCount;
        }
        CompletableFuture<String> futureResult = new CompletableFuture<>();

        AtomicInteger connectionCount = new AtomicInteger();
        vertx.deployVerticle(new Supplier<Verticle>() {
            @Override
            public Verticle get() {
                return new WebDeploymentVerticle(httpMainServerOptions, httpMainSslServerOptions, httpMainDomainSocketOptions,
                        launchMode,
                        insecureRequestStrategy, httpConfiguration, connectionCount);
            }
        }, new DeploymentOptions().setInstances(ioThreads), new Handler<AsyncResult<String>>() {
            @Override
            public void handle(AsyncResult<String> event) {
                if (event.failed()) {
                    Throwable effectiveCause = event.cause();
                    if (effectiveCause instanceof BindException) {
                        List<Integer> portsUsed = Collections.emptyList();

                        if ((httpMainSslServerOptions == null) && (httpMainServerOptions != null)) {
                            portsUsed = List.of(httpMainServerOptions.getPort());
                        } else if ((insecureRequestStrategy == InsecureRequests.DISABLED)
                                && (httpMainSslServerOptions != null)) {
                            portsUsed = List.of(httpMainSslServerOptions.getPort());
                        } else if ((httpMainSslServerOptions != null)
                                && (insecureRequestStrategy == InsecureRequests.ENABLED)
                                && (httpMainServerOptions != null)) {
                            portsUsed = List.of(httpMainServerOptions.getPort(), httpMainSslServerOptions.getPort());
                        }

                        effectiveCause = new QuarkusBindException((BindException) effectiveCause, portsUsed);
                    }
                    futureResult.completeExceptionally(effectiveCause);
                } else {
                    futureResult.complete(event.result());
                }
            }
        });

        return futureResult;
    }

    private static void doServerStart(Vertx vertx, HttpBuildTimeConfig httpBuildTimeConfig,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig, Handler<HttpServerRequest> managementRouter,
            HttpConfiguration httpConfiguration, ManagementInterfaceConfiguration managementConfig,
            LaunchMode launchMode,
            Supplier<Integer> eventLoops, List<String> websocketSubProtocols,
            InsecureRequests insecureRequestStrategy,
            boolean auxiliaryApplication) throws IOException {

        var mainServerFuture = initializeMainHttpServer(vertx, httpBuildTimeConfig, httpConfiguration, launchMode, eventLoops,
                websocketSubProtocols, insecureRequestStrategy);
        var managementInterfaceFuture = initializeManagementInterface(vertx, managementBuildTimeConfig, managementRouter,
                managementConfig, launchMode, websocketSubProtocols);
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
                            for (Long id : taskIds) {
                                vertx.cancelTimer(id);
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
                    "http://%s:%s", httpServerOptions.getHost(), actualHttpPort));
            socketCount++;
        }

        if (sslConfig != null) {
            if (socketCount > 0) {
                serverListeningMessage.append(" and ");
            }
            serverListeningMessage.append(String.format("https://%s:%s", sslConfig.getHost(), actualHttpsPort));
            socketCount++;
        }

        if (domainSocketOptions != null) {
            if (socketCount > 0) {
                serverListeningMessage.append(" and ");
            }
            serverListeningMessage.append(String.format("unix:%s", domainSocketOptions.getHost()));
        }
        if (managementConfig != null) {
            serverListeningMessage.append(
                    String.format(". Management interface listening on http%s://%s:%s.", managementConfig.isSsl() ? "s" : "",
                            managementConfig.getHost(), managementConfig.getPort()));
        }

        Timing.setHttpServer(serverListeningMessage.toString(), auxiliaryApplication);
    }

    private static HttpServerOptions createHttpServerOptions(
            HttpBuildTimeConfig buildTimeConfig, HttpConfiguration httpConfiguration,
            LaunchMode launchMode, List<String> websocketSubProtocols) {
        if (!httpConfiguration.hostEnabled) {
            return null;
        }
        // TODO other config properties
        HttpServerOptions options = new HttpServerOptions();
        int port = httpConfiguration.determinePort(launchMode);
        options.setPort(port == 0 ? -1 : port);

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfiguration, websocketSubProtocols);

        return options;
    }

    private static HttpServerOptions createHttpServerOptionsForManagementInterface(
            ManagementInterfaceBuildTimeConfig buildTimeConfig, ManagementInterfaceConfiguration httpConfiguration,
            LaunchMode launchMode, List<String> websocketSubProtocols) {
        if (!httpConfiguration.hostEnabled) {
            return null;
        }
        HttpServerOptions options = new HttpServerOptions();
        int port = httpConfiguration.determinePort(launchMode);
        options.setPort(port == 0 ? -1 : port);

        HttpServerOptionsUtils.applyCommonOptionsForManagementInterface(options, buildTimeConfig, httpConfiguration,
                websocketSubProtocols);

        return options;
    }

    private static HttpServerOptions createDomainSocketOptions(
            HttpBuildTimeConfig buildTimeConfig, HttpConfiguration httpConfiguration,
            List<String> websocketSubProtocols) {
        if (!httpConfiguration.domainSocketEnabled) {
            return null;
        }
        HttpServerOptions options = new HttpServerOptions();

        HttpServerOptionsUtils.applyCommonOptions(options, buildTimeConfig, httpConfiguration, websocketSubProtocols);
        // Override the host (0.0.0.0 by default) with the configured domain socket.
        options.setHost(httpConfiguration.domainSocket);

        // Check if we can write into the domain socket directory
        // We can do this check using a blocking API as the execution is done from the main thread (not an I/O thread)
        File file = new File(httpConfiguration.domainSocket);
        if (!file.getParentFile().canWrite()) {
            LOGGER.warnf(
                    "Unable to write in the domain socket directory (`%s`). Binding to the socket is likely going to fail.",
                    httpConfiguration.domainSocket);
        }

        return options;
    }

    private static HttpServerOptions createDomainSocketOptionsForManagementInterface(
            ManagementInterfaceBuildTimeConfig buildTimeConfig, ManagementInterfaceConfiguration httpConfiguration,
            List<String> websocketSubProtocols) {
        if (!httpConfiguration.domainSocketEnabled) {
            return null;
        }
        HttpServerOptions options = new HttpServerOptions();

        HttpServerOptionsUtils.applyCommonOptionsForManagementInterface(options, buildTimeConfig, httpConfiguration,
                websocketSubProtocols);
        // Override the host (0.0.0.0 by default) with the configured domain socket.
        options.setHost(httpConfiguration.domainSocket);

        // Check if we can write into the domain socket directory
        // We can do this check using a blocking API as the execution is done from the main thread (not an I/O thread)
        File file = new File(httpConfiguration.domainSocket);
        if (!file.getParentFile().canWrite()) {
            LOGGER.warnf(
                    "Unable to write in the domain socket directory (`%s`). Binding to the socket is likely going to fail.",
                    httpConfiguration.domainSocket);
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
        private final HttpConfiguration.InsecureRequests insecureRequests;
        private final HttpConfiguration quarkusConfig;
        private final AtomicInteger connectionCount;
        private final List<Long> reloadingTasks = new CopyOnWriteArrayList<>();

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
            org.crac.Core.getGlobalContext().register(this);
        }

        @Override
        public void start(Promise<Void> startFuture) {
            assert Context.isOnEventLoopThread();

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
                Promise<Void> startFuture, AtomicInteger remainingCount, AtomicInteger currentConnectionCount) {

            if (quarkusConfig.limits.maxConnections.isPresent() && quarkusConfig.limits.maxConnections.getAsInt() > 0) {
                var tracker = vertx.isMetricsEnabled()
                        ? ((ExtendedQuarkusVertxHttpMetrics) ((VertxInternal) vertx).metricsSPI()).getHttpConnectionTracker()
                        : ExtendedQuarkusVertxHttpMetrics.NOOP_CONNECTION_TRACKER;

                final int maxConnections = quarkusConfig.limits.maxConnections.getAsInt();
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
                        startFuture.fail(event.cause());
                    } else {
                        // Port may be random, so set the actual port
                        int actualPort = event.result().actualPort();

                        if (https) {
                            actualHttpsPort = actualPort;
                        } else {
                            actualHttpPort = actualPort;
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

                        if (https && quarkusConfig.ssl.certificate.reloadPeriod.isPresent()) {
                            long l = TlsCertificateReloadUtils.handleCertificateReloading(
                                    vertx, httpsServer, httpsOptions, quarkusConfig.ssl);
                            if (l != -1) {
                                reloadingTasks.add(l);
                            }
                        }

                        if (remainingCount.decrementAndGet() == 0) {
                            //make sure we only complete once
                            startFuture.complete(null);
                        }

                    }
                }
            });
        }

        @Override
        public void stop(Promise<Void> stopFuture) {

            for (Long id : reloadingTasks) {
                vertx.cancelTimer(id);
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
                                    new HttpServerOptions(),
                                    chctx,
                                    rootContext,
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

    public Handler<RoutingContext> createBodyHandler() {
        Optional<MemorySize> maxBodySize = httpConfiguration.getValue().limits.maxBodySize;
        return configureAndGetBody(maxBodySize, httpConfiguration.getValue().body);
    }

    public Handler<RoutingContext> createBodyHandlerForManagementInterface() {
        Optional<MemorySize> maxBodySize = managementConfiguration.getValue().limits.maxBodySize;
        return configureAndGetBody(maxBodySize, managementConfiguration.getValue().body);
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
}
