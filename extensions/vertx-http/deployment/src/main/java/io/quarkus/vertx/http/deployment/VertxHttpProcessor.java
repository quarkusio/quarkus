package io.quarkus.vertx.http.deployment;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
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
import io.quarkus.vertx.core.deployment.InternalWebVertxBuildItem;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.RouterProducer;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.quarkus.vertx.http.runtime.cors.CORSRecorder;
import io.vertx.ext.web.Router;

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
            LaunchModeBuildItem launchMode,
            ShutdownContextBuildItem shutdown,
            InternalWebVertxBuildItem vertx,
            Optional<RequireVirtualHttpBuildItem> requireVirtual) throws IOException {

        boolean startVirtual = requireVirtual.isPresent() || httpConfiguration.virtual;
        // start http socket in dev/test mode even if virtual http is required
        boolean startSocket = !startVirtual || launchMode.getLaunchMode() != LaunchMode.NORMAL;
        RuntimeValue<Router> router = recorder.initializeRouter(vertx.getVertx(), shutdown,
                httpConfiguration, launchMode.getLaunchMode(), startVirtual, startSocket);

        return new VertxWebRouterBuildItem(router);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem finalizeRouter(VertxHttpRecorder recorder, BeanContainerBuildItem beanContainer,
            LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown, Optional<DefaultRouteBuildItem> defaultRoute,
            List<FilterBuildItem> filters, VertxWebRouterBuildItem router,
            List<AdditionalRoutesInstalledBuildItem> additionalRoutesInstalled) {
        recorder.finalizeRouter(beanContainer.getValue(), defaultRoute.map(DefaultRouteBuildItem::getHandler).orElse(null),
                filters.stream().map(FilterBuildItem::getHandler).collect(Collectors.toList()),
                launchMode.getLaunchMode(), shutdown);

        return new ServiceStartBuildItem("vertx-http");
    }
}
