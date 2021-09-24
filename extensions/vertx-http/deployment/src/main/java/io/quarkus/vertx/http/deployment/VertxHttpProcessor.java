package io.quarkus.vertx.http.deployment;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationStartBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceBuildItem;
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
import io.quarkus.vertx.http.deployment.devmode.HttpRemoteDevClientProvider;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.runtime.CurrentRequestProducer;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.HttpHostConfigSource;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.quarkus.vertx.http.runtime.attribute.ExchangeAttributeBuilder;
import io.quarkus.vertx.http.runtime.cors.CORSRecorder;
import io.quarkus.vertx.http.runtime.filters.Filter;
import io.quarkus.vertx.http.runtime.filters.GracefulShutdownFilter;
import io.quarkus.vertx.http.runtime.security.IdentityProviderManagerBuilderCustomizerProducer;
import io.vertx.core.Handler;
import io.vertx.core.http.impl.Http1xServerRequest;
import io.vertx.core.impl.VertxImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class VertxHttpProcessor {

    private static final Logger logger = Logger.getLogger(VertxHttpProcessor.class);

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
    NonApplicationRootPathBuildItem frameworkRoot(HttpBuildTimeConfig httpBuildTimeConfig) {
        return new NonApplicationRootPathBuildItem(httpBuildTimeConfig.rootPath, httpBuildTimeConfig.nonApplicationRootPath);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    FilterBuildItem cors(CORSRecorder recorder, HttpConfiguration configuration) {
        return new FilterBuildItem(recorder.corsHandler(configuration), FilterBuildItem.CORS);
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClass(CurrentVertxRequest.class)
                .addBeanClass(CurrentRequestProducer.class)
                .addBeanClass(IdentityProviderManagerBuilderCustomizerProducer.class)
                .build();
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
    public KubernetesPortBuildItem kubernetes() {
        int port = ConfigProvider.getConfig().getOptionalValue("quarkus.http.port", Integer.class).orElse(8080);
        return new KubernetesPortBuildItem(port, "http");
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
            NonApplicationRootPathBuildItem nonApplicationRootPath,
            ShutdownContextBuildItem shutdown) {

        RuntimeValue<Router> httpRouteRouter = initialRouter.getHttpRouter();
        RuntimeValue<io.vertx.mutiny.ext.web.Router> mutinyRouter = initialRouter.getMutinyRouter();
        RuntimeValue<Router> frameworkRouter = null;
        RuntimeValue<Router> mainRouter = null;

        List<RouteBuildItem> redirectRoutes = new ArrayList<>();
        boolean frameworkRouterCreated = false;
        boolean mainRouterCreated = false;

        for (RouteBuildItem route : routes) {
            if (nonApplicationRootPath.isDedicatedRouterRequired() && route.isFrameworkRoute()) {
                // Non-application endpoints on a separate path
                if (!frameworkRouterCreated) {
                    frameworkRouter = recorder.initializeRouter(vertx.getVertx());
                    frameworkRouterCreated = true;
                }

                recorder.addRoute(frameworkRouter, route.getRouteFunction(), route.getHandler(), route.getType());
            } else if (route.isAbsoluteRoute()) {
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

        if (frameworkRouterCreated) {
            if (redirectRoutes.size() > 0) {
                recorder.setNonApplicationRedirectHandler(nonApplicationRootPath.getNonApplicationRootPath(),
                        nonApplicationRootPath.getNormalizedHttpRootPath());

                redirectRoutes.forEach(route -> recorder.addRoute(httpRouteRouter, route.getRouteFunction(),
                        recorder.getNonApplicationRedirectHandler(),
                        route.getType()));
            }
        }

        return new VertxWebRouterBuildItem(httpRouteRouter, mainRouter, frameworkRouter, mutinyRouter);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    BodyHandlerBuildItem bodyHandler(VertxHttpRecorder recorder, HttpConfiguration httpConfiguration) {
        return new BodyHandlerBuildItem(recorder.createBodyHandler(httpConfiguration));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem finalizeRouter(
            VertxHttpRecorder recorder, BeanContainerBuildItem beanContainer, CoreVertxBuildItem vertx,
            LaunchModeBuildItem launchMode,
            List<DefaultRouteBuildItem> defaultRoutes, List<FilterBuildItem> filters,
            VertxWebRouterBuildItem httpRouteRouter,
            HttpRootPathBuildItem httpRootPathBuildItem,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            HttpBuildTimeConfig httpBuildTimeConfig, HttpConfiguration httpConfiguration,
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
                listOfFilters, vertx.getVertx(), lrc, mainRouter, httpRouteRouter.getHttpRouter(),
                httpRouteRouter.getMutinyRouter(),
                httpRootPathBuildItem.getRootPath(),
                launchMode.getLaunchMode(),
                !requireBodyHandlerBuildItems.isEmpty(), bodyHandler, httpConfiguration, gracefulShutdownFilter,
                shutdownConfig, executorBuildItem.getExecutorProxy());

        return new ServiceStartBuildItem("vertx-http");
    }

    @BuildStep
    void hostDefault(BuildProducer<RunTimeConfigurationSourceBuildItem> serviceProviderBuildItem) {
        serviceProviderBuildItem
                .produce(new RunTimeConfigurationSourceBuildItem(HttpHostConfigSource.class.getName(),
                        OptionalInt.of(Integer.MIN_VALUE)));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void openSocket(ApplicationStartBuildItem start,
            LaunchModeBuildItem launchMode,
            CoreVertxBuildItem vertx,
            ShutdownContextBuildItem shutdown,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            HttpBuildTimeConfig httpBuildTimeConfig, HttpConfiguration httpConfiguration,
            Optional<RequireVirtualHttpBuildItem> requireVirtual,
            EventLoopCountBuildItem eventLoopCount,
            List<WebsocketSubProtocolsBuildItem> websocketSubProtocols,
            VertxHttpRecorder recorder) throws IOException {
        boolean startVirtual = requireVirtual.isPresent() || httpBuildTimeConfig.virtual;
        if (startVirtual) {
            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, false, false, VirtualServerChannel.class));
        }
        boolean startSocket = (!startVirtual || launchMode.getLaunchMode() != LaunchMode.NORMAL)
                && (requireVirtual.isEmpty() || !requireVirtual.get().isAlwaysVirtual());
        recorder.startServer(vertx.getVertx(), shutdown,
                httpBuildTimeConfig, httpConfiguration, launchMode.getLaunchMode(), startVirtual, startSocket,
                eventLoopCount.getEventLoopCount(),
                websocketSubProtocols.stream().map(bi -> bi.getWebsocketSubProtocols())
                        .collect(Collectors.toList()),
                launchMode.isAuxiliaryApplication());
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
        final CodeSource codeSource = ExchangeAttributeBuilder.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            logger.debug("Skipping registration of service providers for " + ExchangeAttributeBuilder.class);
            return;
        }
        try (final FileSystem jarFileSystem = ZipUtils.newFileSystem(
                new URI("jar", codeSource.getLocation().toURI().toString(), null),
                Collections.emptyMap())) {
            final Path serviceDescriptorFilePath = jarFileSystem.getPath("META-INF", "services",
                    "io.quarkus.vertx.http.runtime.attribute.ExchangeAttributeBuilder");
            if (!Files.exists(serviceDescriptorFilePath)) {
                logger.debug("Skipping registration of service providers for " + ExchangeAttributeBuilder.class
                        + " since no service descriptor file found");
                return;
            }
            // we register all the listed providers since the access log configuration is a runtime construct
            // and we won't know at build time which attributes the user application will choose
            final ServiceProviderBuildItem serviceProviderBuildItem;
            serviceProviderBuildItem = ServiceProviderBuildItem.allProviders(ExchangeAttributeBuilder.class.getName(),
                    serviceDescriptorFilePath);
            exchangeAttributeBuilderService.produce(serviceProviderBuildItem);
        } catch (IOException | URISyntaxException e) {
            throw new BuildException(e, Collections.emptyList());
        }
    }

}
