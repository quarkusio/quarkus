package io.quarkus.vertx.http.deployment;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationStartBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.ShutdownListenerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.netty.runtime.virtual.VirtualServerChannel;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.shutdown.ShutdownConfig;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.core.deployment.EventLoopCountBuildItem;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.RouterProducer;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.quarkus.vertx.http.runtime.cors.CORSRecorder;
import io.quarkus.vertx.http.runtime.filters.Filter;
import io.quarkus.vertx.http.runtime.filters.GracefulShutdownFilter;
import io.vertx.core.Handler;
import io.vertx.core.impl.VertxImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class VertxHttpProcessor {

    @BuildStep
    HttpRootPathBuildItem httpRoot(HttpBuildTimeConfig httpBuildTimeConfig) {
        return new HttpRootPathBuildItem(httpBuildTimeConfig.rootPath);
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
                .addBeanClass(RouterProducer.class)
                .addBeanClass(CurrentVertxRequest.class)
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

    @BuildStep(onlyIf = IsNormal.class)
    public KubernetesPortBuildItem kubernetes() {
        int port = ConfigProvider.getConfig().getOptionalValue("quarkus.http.port", Integer.class).orElse(8080);
        return new KubernetesPortBuildItem(port, "http");
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    VertxWebRouterBuildItem initializeRouter(VertxHttpRecorder recorder,
            CoreVertxBuildItem vertx,
            List<RouteBuildItem> routes, LaunchModeBuildItem launchModeBuildItem,
            ShutdownContextBuildItem shutdown) {

        RuntimeValue<Router> router = recorder.initializeRouter(vertx.getVertx(), launchModeBuildItem.getLaunchMode(),
                shutdown);
        for (RouteBuildItem route : routes) {
            recorder.addRoute(router, route.getRouteFunction(), route.getHandler(), route.getType());
        }

        return new VertxWebRouterBuildItem(router);
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
            VertxWebRouterBuildItem router,
            HttpBuildTimeConfig httpBuildTimeConfig, HttpConfiguration httpConfiguration,
            List<RequireBodyHandlerBuildItem> requireBodyHandlerBuildItems,
            BodyHandlerBuildItem bodyHandlerBuildItem,
            BuildProducer<ShutdownListenerBuildItem> shutdownListenerBuildItemBuildProducer,
            ShutdownConfig shutdownConfig,
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

        GracefulShutdownFilter gracefulShutdownFilter = recorder.createGracefulShutdownHandler();
        shutdownListenerBuildItemBuildProducer.produce(new ShutdownListenerBuildItem(gracefulShutdownFilter));

        List<Filter> listOfFilters = filters.stream()
                .filter(f -> f.getHandler() != null)
                .map(FilterBuildItem::toFilter).collect(Collectors.toList());

        //if the body handler is required then we know it is installed for all routes, so we don't need to register it here
        Handler<RoutingContext> bodyHandler = !requireBodyHandlerBuildItems.isEmpty() ? bodyHandlerBuildItem.getHandler()
                : null;

        recorder.finalizeRouter(beanContainer.getValue(),
                defaultRoute.map(DefaultRouteBuildItem::getRoute).orElse(null),
                listOfFilters, vertx.getVertx(), router.getRouter(), httpBuildTimeConfig.rootPath, launchMode.getLaunchMode(),
                !requireBodyHandlerBuildItems.isEmpty(), bodyHandler, httpConfiguration, gracefulShutdownFilter,
                shutdownConfig, executorBuildItem.getExecutorProxy());

        return new ServiceStartBuildItem("vertx-http");
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
        // start http socket in dev/test mode even if virtual http is required
        boolean startSocket = !startVirtual || launchMode.getLaunchMode() != LaunchMode.NORMAL;
        recorder.startServer(vertx.getVertx(), shutdown,
                httpConfiguration, launchMode.getLaunchMode(), startVirtual, startSocket,
                eventLoopCount.getEventLoopCount(),
                websocketSubProtocols.stream().map(bi -> bi.getWebsocketSubProtocols())
                        .collect(Collectors.joining(",")));
    }

    @BuildStep
    RuntimeInitializedClassBuildItem configureNativeCompilation() {
        return new RuntimeInitializedClassBuildItem("io.vertx.ext.web.handler.sockjs.impl.XhrTransport");
    }
}
