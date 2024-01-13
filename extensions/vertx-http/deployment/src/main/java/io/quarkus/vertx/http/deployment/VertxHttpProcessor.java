package io.quarkus.vertx.http.deployment;

import static io.quarkus.runtime.TemplateHtmlBuilder.adjustRoot;
import static io.quarkus.vertx.http.deployment.RouteBuildItem.RouteType.FRAMEWORK_ROUTE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationStartBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.netty.runtime.virtual.VirtualServerChannel;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.LiveReloadConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.shutdown.ShutdownConfig;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.core.deployment.EventLoopCountBuildItem;
import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.quarkus.vertx.http.deployment.devmode.HttpRemoteDevClientProvider;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.spi.FrameworkEndpointsBuildItem;
import io.quarkus.vertx.http.deployment.spi.UseManagementInterfaceBuildItem;
import io.quarkus.vertx.http.runtime.BasicRoute;
import io.quarkus.vertx.http.runtime.CurrentRequestProducer;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.VertxConfigBuilder;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.quarkus.vertx.http.runtime.attribute.ExchangeAttributeBuilder;
import io.quarkus.vertx.http.runtime.cors.CORSRecorder;
import io.quarkus.vertx.http.runtime.filters.Filter;
import io.quarkus.vertx.http.runtime.filters.GracefulShutdownFilter;
import io.quarkus.vertx.http.runtime.management.ManagementInterfaceBuildTimeConfig;
import io.vertx.core.Handler;
import io.vertx.core.http.impl.Http1xServerRequest;
import io.vertx.core.impl.VertxImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class VertxHttpProcessor {

    private static final String META_INF_SERVICES_EXCHANGE_ATTRIBUTE_BUILDER = "META-INF/services/io.quarkus.vertx.http.runtime.attribute.ExchangeAttributeBuilder";
    private static final Logger logger = Logger.getLogger(VertxHttpProcessor.class);

    // For enabling HTTPS port in Kubernetes
    private static final String HTTP_SSL_PREFIX = "quarkus.http.ssl.certificate.";
    private static final List<String> HTTP_SSL_PROPERTIES = List.of("key-store-file", "trust-store-file", "files",
            "key-files");

    @BuildStep
    LogCategoryBuildItem logging() {
        //this log is only used to log an error about an incorrect URI, which results in a 400 response
        //we don't want to log this
        return new LogCategoryBuildItem(Http1xServerRequest.class.getName(), Level.OFF);
    }

    @BuildStep
    HttpRootPathBuildItem httpRoot(HttpBuildTimeConfig httpBuildTimeConfig) {
        return new HttpRootPathBuildItem(httpBuildTimeConfig.rootPath);
    }

    @BuildStep
    List<RouteBuildItem> convertRoutes(
            List<io.quarkus.vertx.http.deployment.spi.RouteBuildItem> items,
            HttpRootPathBuildItem httpRootPathBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        List<RouteBuildItem> list = new ArrayList<>();
        for (io.quarkus.vertx.http.deployment.spi.RouteBuildItem item : items) {
            RouteBuildItem converted = RouteConverter.convert(item, httpRootPathBuildItem, nonApplicationRootPathBuildItem);
            list.add(converted);
        }
        return list;
    }

    @BuildStep
    NonApplicationRootPathBuildItem frameworkRoot(HttpBuildTimeConfig httpBuildTimeConfig,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig) {
        String mrp = null;
        if (managementBuildTimeConfig.enabled) {
            mrp = managementBuildTimeConfig.rootPath;
        }
        return new NonApplicationRootPathBuildItem(httpBuildTimeConfig.rootPath, httpBuildTimeConfig.nonApplicationRootPath,
                mrp);
    }

    @BuildStep
    FrameworkEndpointsBuildItem frameworkEndpoints(NonApplicationRootPathBuildItem nonApplicationRootPath,
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig, LaunchModeBuildItem launchModeBuildItem,
            List<RouteBuildItem> routes) {
        List<String> frameworkEndpoints = new ArrayList<>();
        for (RouteBuildItem route : routes) {
            if (FRAMEWORK_ROUTE.equals(route.getRouteType())) {
                if (route.getConfiguredPathInfo() != null) {
                    String endpointPath = route.getConfiguredPathInfo().getEndpointPath(nonApplicationRootPath,
                            managementInterfaceBuildTimeConfig, launchModeBuildItem);
                    frameworkEndpoints.add(endpointPath);
                    continue;
                }
                if (route.getRouteFunction() != null && route.getRouteFunction() instanceof BasicRoute) {
                    BasicRoute basicRoute = (BasicRoute) route.getRouteFunction();
                    if (basicRoute.getPath() != null) {
                        if (basicRoute.getPath().startsWith(nonApplicationRootPath.getNonApplicationRootPath())) {
                            // Do not repeat the non application root path.
                            frameworkEndpoints.add(basicRoute.getPath());
                        } else {
                            // Calling TemplateHtmlBuilder does not see very correct here, but it is the underlying API for ConfiguredPathInfo
                            String adjustRoot = adjustRoot(nonApplicationRootPath.getNonApplicationRootPath(),
                                    basicRoute.getPath());
                            frameworkEndpoints.add(adjustRoot);
                        }

                    }
                }
            }
        }
        return new FrameworkEndpointsBuildItem(frameworkEndpoints);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    FilterBuildItem cors(CORSRecorder recorder) {
        return new FilterBuildItem(recorder.corsHandler(), FilterBuildItem.CORS);
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(CurrentVertxRequest.class)
                .addBeanClass(CurrentRequestProducer.class)
                .build();
    }

    @BuildStep
    UnremovableBeanBuildItem shouldNotRemoveHttpServerOptionsCustomizers() {
        return UnremovableBeanBuildItem.beanTypes(HttpServerOptionsCustomizer.class);
    }

    @BuildStep
    UseManagementInterfaceBuildItem useManagementInterfaceBuildItem(ManagementInterfaceBuildTimeConfig config) {
        if (config.enabled) {
            return new UseManagementInterfaceBuildItem();
        }
        return null;
    }

    /**
     * Workaround for https://github.com/quarkusio/quarkus/issues/4720 by filtering Vertx multiple instance warning in dev
     * mode.
     */
    @BuildStep
    void filterMultipleVertxInstancesWarning(LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<LogCleanupFilterBuildItem> logCleanupFilterBuildItemBuildProducer) {
        if (launchModeBuildItem.getLaunchMode().equals(LaunchMode.DEVELOPMENT)) {
            logCleanupFilterBuildItemBuildProducer.produce(new LogCleanupFilterBuildItem(VertxImpl.class.getName(),
                    "You're already on a Vert.x context, are you sure you want to create a new Vertx instance"));
        }
    }

    @BuildStep
    public void kubernetes(BuildProducer<KubernetesPortBuildItem> kubernetesPorts) {
        kubernetesPorts.produce(KubernetesPortBuildItem.fromRuntimeConfiguration("http", "quarkus.http.port", 8080, true));
        kubernetesPorts.produce(
                KubernetesPortBuildItem.fromRuntimeConfiguration("https", "quarkus.http.ssl-port", 8443, isSslConfigured()));
    }

    @BuildStep
    public KubernetesPortBuildItem kubernetesForManagement(
            ManagementInterfaceBuildTimeConfig managementInterfaceBuildTimeConfig) {
        return KubernetesPortBuildItem.fromRuntimeConfiguration("management", "quarkus.management.port", 9000,
                managementInterfaceBuildTimeConfig.enabled);
    }

    @BuildStep
    void notFoundRoutes(
            List<RouteBuildItem> routes,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> notFound) {
        for (RouteBuildItem i : routes) {
            if (i.getNotFoundPageDisplayableEndpoint() != null) {
                notFound.produce(i.getNotFoundPageDisplayableEndpoint());
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void preinitializeRouter(CoreVertxBuildItem vertx, VertxHttpRecorder recorder,
            BuildProducer<InitialRouterBuildItem> initialRouter, BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        // We need to initialize the routers that are exposed as synthetic beans in a separate build step to avoid cycles in the build chain
        RuntimeValue<Router> httpRouteRouter = recorder.initializeRouter(vertx.getVertx());
        RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter = recorder.createMutinyRouter(httpRouteRouter);
        initialRouter.produce(new InitialRouterBuildItem(httpRouteRouter, mutinyRouter));

        // Also note that we need a client proxy to handle the use case where a bean also @Observes Router
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(Router.class)
                .scope(BuiltinScope.APPLICATION.getInfo())
                .setRuntimeInit()
                .runtimeValue(httpRouteRouter).done());
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(io.vertx.mutiny.ext.web.Router.class)
                .scope(BuiltinScope.APPLICATION.getInfo())
                .setRuntimeInit()
                .runtimeValue(mutinyRouter).done());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    VertxWebRouterBuildItem initializeRouter(VertxHttpRecorder recorder,
            InitialRouterBuildItem initialRouter,
            CoreVertxBuildItem vertx,
            List<RouteBuildItem> routes,
            HttpBuildTimeConfig httpBuildTimeConfig,
            ManagementInterfaceBuildTimeConfig managementBuildTimeConfig,
            NonApplicationRootPathBuildItem nonApplicationRootPath,
            ShutdownContextBuildItem shutdown) {

        RuntimeValue<Router> httpRouteRouter = initialRouter.getHttpRouter();
        RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter = initialRouter.getMutinyRouter();
        RuntimeValue<Router> frameworkRouter = null;
        RuntimeValue<Router> mainRouter = null;
        RuntimeValue<Router> managementRouter = null;

        List<RouteBuildItem> redirectRoutes = new ArrayList<>();
        boolean frameworkRouterCreated = false;
        boolean mainRouterCreated = false;
        boolean managementRouterCreated = false;

        boolean isManagementInterfaceEnabled = managementBuildTimeConfig.enabled;

        for (RouteBuildItem route : routes) {
            if (route.isManagement() && isManagementInterfaceEnabled) {
                if (!managementRouterCreated) {
                    managementRouter = recorder.initializeRouter(vertx.getVertx());
                    managementRouterCreated = true;
                }
                recorder.addRoute(managementRouter, route.getRouteFunction(), route.getHandler(), route.getType());
            } else if (nonApplicationRootPath.isDedicatedRouterRequired() && route.isRouterFramework()) {
                // Non-application endpoints on a separate path
                if (!frameworkRouterCreated) {
                    frameworkRouter = recorder.initializeRouter(vertx.getVertx());
                    frameworkRouterCreated = true;
                }
                recorder.addRoute(frameworkRouter, route.getRouteFunction(), route.getHandler(), route.getType());
            } else if (route.isRouterAbsolute()) {
                // Add Route to "/"
                if (!mainRouterCreated) {
                    mainRouter = recorder.initializeRouter(vertx.getVertx());
                    mainRouterCreated = true;
                }
                recorder.addRoute(mainRouter, route.getRouteFunction(), route.getHandler(), route.getType());
            } else {
                // Add Route to "/${quarkus.http.root-path}/
                recorder.addRoute(httpRouteRouter, route.getRouteFunction(), route.getHandler(), route.getType());
            }
        }

        /*
         * To create mainrouter when `${quarkus.http.root-path}` is not {@literal /}
         * Refer https://github.com/quarkusio/quarkus/issues/34261
         */
        if (!httpBuildTimeConfig.rootPath.equals("/") && !mainRouterCreated) {
            mainRouter = recorder.initializeRouter(vertx.getVertx());
        }

        if (frameworkRouterCreated) {
            if (redirectRoutes.size() > 0) {
                recorder.setNonApplicationRedirectHandler(nonApplicationRootPath.getNonApplicationRootPath(),
                        nonApplicationRootPath.getNormalizedHttpRootPath());

                redirectRoutes.forEach(route -> recorder.addRoute(httpRouteRouter, route.getRouteFunction(),
                        recorder.getNonApplicationRedirectHandler(),
                        route.getType()));
            }
        }

        return new VertxWebRouterBuildItem(httpRouteRouter, mainRouter, frameworkRouter, managementRouter, mutinyRouter);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    BodyHandlerBuildItem bodyHandler(VertxHttpRecorder recorder) {
        return new BodyHandlerBuildItem(recorder.createBodyHandler());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem finalizeRouter(
            VertxHttpRecorder recorder, BeanContainerBuildItem beanContainer, CoreVertxBuildItem vertx,
            LaunchModeBuildItem launchMode,
            List<DefaultRouteBuildItem> defaultRoutes,
            List<FilterBuildItem> filters,
            List<ManagementInterfaceFilterBuildItem> managementInterfacefilters,
            VertxWebRouterBuildItem httpRouteRouter,
            HttpRootPathBuildItem httpRootPathBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            HttpBuildTimeConfig httpBuildTimeConfig,
            List<RequireBodyHandlerBuildItem> requireBodyHandlerBuildItems,
            BodyHandlerBuildItem bodyHandlerBuildItem,
            BuildProducer<ShutdownListenerBuildItem> shutdownListenerBuildItemBuildProducer,
            ShutdownConfig shutdownConfig,
            LiveReloadConfig lrc,
            CoreVertxBuildItem core, // Injected to be sure that Vert.x has been produced before calling this method.
            ExecutorBuildItem executorBuildItem)
            throws BuildException, IOException {

        Optional<DefaultRouteBuildItem> defaultRoute;
        if (defaultRoutes == null || defaultRoutes.isEmpty()) {
            defaultRoute = Optional.empty();
        } else {
            if (defaultRoutes.size() > 1) {
                // this should never happen
                throw new BuildException("Too many default routes.", Collections.emptyList());
            } else {
                defaultRoute = Optional.of(defaultRoutes.get(0));
            }
        }

        HttpRemoteDevClientProvider.liveReloadConfig = lrc;
        GracefulShutdownFilter gracefulShutdownFilter = recorder.createGracefulShutdownHandler();
        shutdownListenerBuildItemBuildProducer.produce(new ShutdownListenerBuildItem(gracefulShutdownFilter));

        List<Filter> listOfFilters = filters.stream()
                .filter(f -> f.getHandler() != null)
                .map(FilterBuildItem::toFilter).collect(Collectors.toList());

        List<Filter> listOfManagementInterfaceFilters = managementInterfacefilters.stream()
                .filter(f -> f.getHandler() != null)
                .map(ManagementInterfaceFilterBuildItem::toFilter).collect(Collectors.toList());

        //if the body handler is required then we know it is installed for all routes, so we don't need to register it here
        Handler<RoutingContext> bodyHandler = !requireBodyHandlerBuildItems.isEmpty() ? bodyHandlerBuildItem.getHandler()
                : null;

        Optional<RuntimeValue<Router>> mainRouter = httpRouteRouter.getMainRouter() != null
                ? Optional.of(httpRouteRouter.getMainRouter())
                : Optional.empty();

        if (httpRouteRouter.getFrameworkRouter() != null) {
            if (nonApplicationRootPathBuildItem.isAttachedToMainRouter()) {
                // Mount nested framework router
                recorder.mountFrameworkRouter(httpRouteRouter.getHttpRouter(),
                        httpRouteRouter.getFrameworkRouter(),
                        nonApplicationRootPathBuildItem.getVertxRouterPath());
            } else {
                // Create main router, not mounted under application router
                if (!mainRouter.isPresent()) {
                    mainRouter = Optional.of(recorder.initializeRouter(vertx.getVertx()));
                }
                // Mount independent framework router under new main router
                recorder.mountFrameworkRouter(mainRouter.get(), httpRouteRouter.getFrameworkRouter(),
                        nonApplicationRootPathBuildItem.getVertxRouterPath());
            }
        }

        recorder.finalizeRouter(beanContainer.getValue(),
                defaultRoute.map(DefaultRouteBuildItem::getRoute).orElse(null),
                listOfFilters, listOfManagementInterfaceFilters,
                vertx.getVertx(), lrc, mainRouter, httpRouteRouter.getHttpRouter(),
                httpRouteRouter.getMutinyRouter(), httpRouteRouter.getFrameworkRouter(),
                httpRouteRouter.getManagementRouter(),
                httpRootPathBuildItem.getRootPath(),
                nonApplicationRootPathBuildItem.getNonApplicationRootPath(),
                launchMode.getLaunchMode(),
                !requireBodyHandlerBuildItems.isEmpty(), bodyHandler, gracefulShutdownFilter,
                shutdownConfig, executorBuildItem.getExecutorProxy());

        return new ServiceStartBuildItem("vertx-http");
    }

    @BuildStep
    void config(BuildProducer<RunTimeConfigBuilderBuildItem> runtimeConfigBuilder) {
        runtimeConfigBuilder.produce(new RunTimeConfigBuilderBuildItem(VertxConfigBuilder.class));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void openSocket(ApplicationStartBuildItem start,
            LaunchModeBuildItem launchMode,
            CoreVertxBuildItem vertx,
            ShutdownContextBuildItem shutdown,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            HttpBuildTimeConfig httpBuildTimeConfig,
            Optional<RequireVirtualHttpBuildItem> requireVirtual,
            EventLoopCountBuildItem eventLoopCount,
            List<WebsocketSubProtocolsBuildItem> websocketSubProtocols,
            Capabilities capabilities,
            VertxHttpRecorder recorder) throws IOException {
        boolean startVirtual = requireVirtual.isPresent() || httpBuildTimeConfig.virtual;
        if (startVirtual) {
            reflectiveClass
                    .produce(ReflectiveClassBuildItem.builder(VirtualServerChannel.class)
                            .build());
        }
        boolean startSocket = (!startVirtual || launchMode.getLaunchMode() != LaunchMode.NORMAL)
                && (requireVirtual.isEmpty() || !requireVirtual.get().isAlwaysVirtual());
        recorder.startServer(vertx.getVertx(), shutdown,
                launchMode.getLaunchMode(), startVirtual, startSocket,
                eventLoopCount.getEventLoopCount(),
                websocketSubProtocols.stream().map(bi -> bi.getWebsocketSubProtocols())
                        .collect(Collectors.toList()),
                launchMode.isAuxiliaryApplication(), !capabilities.isPresent(Capability.VERTX_WEBSOCKETS));
    }

    @BuildStep
    void configureNativeCompilation(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        runtimeInitializedClasses
                .produce(new RuntimeInitializedClassBuildItem("io.vertx.ext.web.handler.sockjs.impl.XhrTransport"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem("io.vertx.ext.auth.impl.jose.JWT"));
    }

    /**
     * Register the {@link ExchangeAttributeBuilder} services for native image consumption
     *
     * @param exchangeAttributeBuilderService
     * @throws BuildException
     */
    @BuildStep
    void registerExchangeAttributeBuilders(final BuildProducer<ServiceProviderBuildItem> exchangeAttributeBuilderService)
            throws BuildException {
        // get hold of the META-INF/services/io.quarkus.vertx.http.runtime.attribute.ExchangeAttributeBuilder
        // from within the jar containing the ExchangeAttributeBuilder class
        final List<ClassPathElement> elements = QuarkusClassLoader.getElements(META_INF_SERVICES_EXCHANGE_ATTRIBUTE_BUILDER,
                false);
        if (elements.isEmpty()) {
            logger.debug("Skipping registration of service providers for " + ExchangeAttributeBuilder.class);
            return;
        }
        for (ClassPathElement cpe : elements) {
            cpe.apply(tree -> {
                tree.accept(META_INF_SERVICES_EXCHANGE_ATTRIBUTE_BUILDER, visit -> {
                    if (visit == null) {
                        logger.debug("Skipping registration of service providers for " + ExchangeAttributeBuilder.class
                                + " since no service descriptor file found");
                    } else {
                        // we register all the listed providers since the access log configuration is a runtime construct
                        // and we won't know at build time which attributes the user application will choose
                        final ServiceProviderBuildItem serviceProviderBuildItem;
                        try {
                            serviceProviderBuildItem = ServiceProviderBuildItem
                                    .allProviders(ExchangeAttributeBuilder.class.getName(), visit.getPath());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        exchangeAttributeBuilderService.produce(serviceProviderBuildItem);
                    }
                });
                return null;
            });
        }
    }

    /**
     * This method will return true if:
     * <1> "quarkus.http.insecure-requests" is not explicitly disabled
     * <2> any of the http SSL runtime properties are set at build time
     * <p>
     * If any of the above rules applied, the port "https" will be generated as part of the Kubernetes resources.
     */
    private static boolean isSslConfigured() {
        Config config = ConfigProvider.getConfig();
        HttpConfiguration.InsecureRequests insecureRequests = config
                .getOptionalValue("quarkus.http.insecure-requests", HttpConfiguration.InsecureRequests.class)
                .orElse(HttpConfiguration.InsecureRequests.ENABLED);
        if (insecureRequests == HttpConfiguration.InsecureRequests.DISABLED) {
            return false;
        }

        for (String sslProperty : HTTP_SSL_PROPERTIES) {
            Optional<List<String>> property = config.getOptionalValues(HTTP_SSL_PREFIX + sslProperty,
                    String.class);
            if (property.isPresent()) {
                return true;
            }
        }

        return false;
    }
}
