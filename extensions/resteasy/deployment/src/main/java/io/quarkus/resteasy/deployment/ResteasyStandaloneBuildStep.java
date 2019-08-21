package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;

import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.resteasy.runtime.standalone.ResteasyStandaloneRecorder;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.quarkus.vertx.web.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.web.deployment.RequireVirtualHttpBuildItem;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public class ResteasyStandaloneBuildStep {
    private static final Logger log = Logger.getLogger("io.quarkus.resteasy");

    @BuildStep
    @Record(RUNTIME_INIT)
    public void boot(ShutdownContextBuildItem shutdown,
            ResteasyStandaloneRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            Capabilities capabilities,
            BuildProducer<DefaultRouteBuildItem> routeProducer,
            ResteasyDeploymentBuildItem deployment,
            VertxBuildItem vertx,
            BeanContainerBuildItem beanContainer,
            Optional<RequireVirtualHttpBuildItem> requireVirtual,
            ResteasyInjectionReadyBuildItem resteasyInjectionReady) throws Exception {
        if (deployment == null || capabilities.isCapabilityPresent(Capabilities.SERVLET)) {
            return;
        }
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY));

        boolean isVirtual = requireVirtual.isPresent();
        Handler<HttpServerRequest> ut = recorder.startResteasy(vertx.getVertx(),
                deployment.getRootPath(),
                deployment.getDeployment(),
                shutdown,
                beanContainer.getValue(),
                isVirtual);

        routeProducer.produce(new DefaultRouteBuildItem(ut));
    }

}
