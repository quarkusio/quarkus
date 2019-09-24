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
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.core.deployment.EventLoopCountBuildItem;
import io.quarkus.vertx.core.deployment.InternalWebVertxBuildItem;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.RouterProducer;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.quarkus.vertx.http.runtime.cors.CORSRecorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

class VertxHttpProcessor {

    HttpConfiguration httpConfiguration;

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    FilterBuildItem cors(CORSRecorder recorder, HttpConfiguration configuration) {
        return new FilterBuildItem(recorder.corsHandler(configuration));
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.unremovableOf(RouterProducer.class);
    }

    @BuildStep(onlyIf = IsNormal.class)
    @Record(value = ExecutionTime.RUNTIME_INIT, optional = true)
    public KubernetesPortBuildItem kubernetes(HttpConfiguration config, BuildProducer<KubernetesPortBuildItem> portProducer,
            VertxHttpRecorder recorder) {
        int port = ConfigProvider.getConfig().getOptionalValue("quarkus.http.port", Integer.class).orElse(8080);
        recorder.warnIfPortChanged(config, port);
        return new KubernetesPortBuildItem(config.port, "http");
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    VertxWebRouterBuildItem initializeRouter(VertxHttpRecorder recorder,
            InternalWebVertxBuildItem vertx,
            List<RouteBuildItem> routes,
            List<FilterBuildItem> filters) throws IOException {

        RuntimeValue<Router> router = recorder.initializeRouter(vertx.getVertx());
        List<Handler<RoutingContext>> filterList = filters.stream().map(FilterBuildItem::getHandler)
                .collect(Collectors.toList());
        for (RouteBuildItem route : routes) {
            recorder.addRoute(router, route.getRouteFunction(), route.getHandler(), route.getType(), filterList);
        }

        return new VertxWebRouterBuildItem(router);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem finalizeRouter(
            VertxHttpRecorder recorder, BeanContainerBuildItem beanContainer,
            Optional<RequireVirtualHttpBuildItem> requireVirtual,
            InternalWebVertxBuildItem vertx,
            LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown, List<DefaultRouteBuildItem> defaultRoutes,
            List<FilterBuildItem> filters, VertxWebRouterBuildItem router, EventLoopCountBuildItem eventLoopCountBuildItem)
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
        recorder.finalizeRouter(beanContainer.getValue(), defaultRoute.map(DefaultRouteBuildItem::getHandler).orElse(null),
                filters.stream().map(FilterBuildItem::getHandler).collect(Collectors.toList()),
                launchMode.getLaunchMode(), shutdown, router.getRouter());

        boolean startVirtual = requireVirtual.isPresent() || httpConfiguration.virtual;
        // start http socket in dev/test mode even if virtual http is required
        boolean startSocket = !startVirtual || launchMode.getLaunchMode() != LaunchMode.NORMAL;
        recorder.startServer(vertx.getVertx(), shutdown,
                httpConfiguration, launchMode.getLaunchMode(), startVirtual, startSocket,
                eventLoopCountBuildItem.getEventLoopCount());

        return new ServiceStartBuildItem("vertx-http");
    }
}
