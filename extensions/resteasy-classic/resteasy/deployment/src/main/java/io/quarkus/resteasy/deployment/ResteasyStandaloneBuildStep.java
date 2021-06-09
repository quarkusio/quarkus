package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Optional;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.resteasy.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.resteasy.runtime.ResteasyVertxConfig;
import io.quarkus.resteasy.runtime.standalone.ResteasyStandaloneRecorder;
import io.quarkus.resteasy.server.common.deployment.ResteasyDeploymentBuildItem;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.http.deployment.RequireVirtualHttpBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.quarkus.vertx.http.runtime.VertxHttpRecorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class ResteasyStandaloneBuildStep {

    public static final class ResteasyStandaloneBuildItem extends SimpleBuildItem {

        final String deploymentRootPath;

        public ResteasyStandaloneBuildItem(String deploymentRootPath) {
            this.deploymentRootPath = deploymentRootPath.startsWith("/") ? deploymentRootPath : "/" + deploymentRootPath;
        }

    }

    @BuildStep()
    @Record(STATIC_INIT)
    public void staticInit(ResteasyStandaloneRecorder recorder,
            Capabilities capabilities,
            ResteasyDeploymentBuildItem deployment,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ResteasyInjectionReadyBuildItem resteasyInjectionReady,
            HttpBuildTimeConfig httpConfig,
            BuildProducer<ResteasyStandaloneBuildItem> standalone) throws Exception {
        if (capabilities.isPresent(Capability.SERVLET)) {
            return;
        }

        String deploymentRootPath = null;
        // The context path + the resources path
        String rootPath = httpConfig.rootPath;

        if (deployment != null) {
            deploymentRootPath = deployment.getRootPath();
            if (rootPath.endsWith("/")) {
                if (deploymentRootPath.startsWith("/")) {
                    rootPath += deploymentRootPath.substring(1);
                } else {
                    rootPath += deploymentRootPath;
                }
            } else if (!deploymentRootPath.equals("/")) {
                if (!deploymentRootPath.startsWith("/")) {
                    rootPath += "/";
                }
                rootPath += deploymentRootPath;
            }
            recorder.staticInit(deployment.getDeployment(), rootPath);
            standalone.produce(new ResteasyStandaloneBuildItem(deploymentRootPath));
        }
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void boot(ShutdownContextBuildItem shutdown,
            ResteasyStandaloneRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<DefaultRouteBuildItem> defaultRoutes,
            BuildProducer<RouteBuildItem> routes,
            CoreVertxBuildItem vertx,
            BeanContainerBuildItem beanContainer,
            ResteasyStandaloneBuildItem standalone,
            Optional<RequireVirtualHttpBuildItem> requireVirtual,
            ExecutorBuildItem executorBuildItem,
            ResteasyVertxConfig resteasyVertxConfig,
            HttpConfiguration httpConfiguration) throws Exception {

        if (standalone == null) {
            return;
        }
        feature.produce(new FeatureBuildItem(Feature.RESTEASY));

        // Handler used for both the default and non-default deployment path (specified as application path or resteasyConfig.path)
        // Routes use the order VertxHttpRecorder.DEFAULT_ROUTE_ORDER + 1 to ensure the default route is called before the resteasy one
        Handler<RoutingContext> handler = recorder.vertxRequestHandler(vertx.getVertx(), beanContainer.getValue(),
                executorBuildItem.getExecutorProxy(), httpConfiguration, resteasyVertxConfig);
        // Exact match for resources matched to the root path
        routes.produce(
                RouteBuildItem.builder().orderedRoute(standalone.deploymentRootPath, VertxHttpRecorder.DEFAULT_ROUTE_ORDER + 1)
                        .handler(handler).build());
        String matchPath = standalone.deploymentRootPath;
        if (matchPath.endsWith("/")) {
            matchPath += "*";
        } else {
            matchPath += "/*";
        }
        // Match paths that begin with the deployment path
        routes.produce(RouteBuildItem.builder().orderedRoute(matchPath, VertxHttpRecorder.DEFAULT_ROUTE_ORDER + 1)
                .handler(handler).build());

        recorder.start(shutdown, requireVirtual.isPresent());
    }

}
