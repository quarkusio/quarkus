package io.quarkus.funqy.deployment.bindings.http;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.Optional;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.funqy.deployment.FunctionInitializedBuildItem;
import io.quarkus.funqy.runtime.bindings.http.FunqyHttpBindingRecorder;
import io.quarkus.jackson.ObjectMapperProducer;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.DefaultRouteBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;

public class FunqyHttpBuildStep {
    private static final Logger log = Logger.getLogger(FunqyHttpBuildStep.class);
    public static final String FUNQY_HTTP_FEATURE = "funqy-http";

    public static final class RootpathBuildItem extends SimpleBuildItem {

        final String deploymentRootPath;

        public RootpathBuildItem(String deploymentRootPath) {
            if (deploymentRootPath != null) {
                this.deploymentRootPath = deploymentRootPath.startsWith("/") ? deploymentRootPath : "/" + deploymentRootPath;
            } else {
                this.deploymentRootPath = null;
            }
        }

    }

    @BuildStep
    public void markObjectMapper(BuildProducer<UnremovableBeanBuildItem> unremovable) {
        unremovable.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(ObjectMapper.class.getName())));
        unremovable.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(ObjectMapperProducer.class.getName())));
    }

    @BuildStep()
    @Record(STATIC_INIT)
    public RootpathBuildItem staticInit(FunqyHttpBindingRecorder binding,
            BeanContainerBuildItem beanContainer, // dependency
            Optional<FunctionInitializedBuildItem> hasFunctions,
            HttpBuildTimeConfig httpConfig) throws Exception {
        if (!hasFunctions.isPresent() || hasFunctions.get() == null)
            return null;

        // The context path + the resources path
        String rootPath = httpConfig.rootPath;
        binding.init(rootPath);
        return new RootpathBuildItem(rootPath);
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void boot(ShutdownContextBuildItem shutdown,
            FunqyHttpBindingRecorder binding,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<DefaultRouteBuildItem> defaultRoutes,
            BuildProducer<RouteBuildItem> routes,
            CoreVertxBuildItem vertx,
            RootpathBuildItem root,
            BeanContainerBuildItem beanContainer,
            ExecutorBuildItem executorBuildItem) throws Exception {

        if (root == null)
            return;
        feature.produce(new FeatureBuildItem(FUNQY_HTTP_FEATURE));

        String rootPath = root.deploymentRootPath;
        boolean isDefaultOrNullDeploymentPath = rootPath.equals("/");
        if (!isDefaultOrNullDeploymentPath) {
            // We need to register a special handler for non-default deployment path (specified as application path or resteasyConfig.path)
            Handler<RoutingContext> handler = binding.vertxRequestHandler(vertx.getVertx(), beanContainer.getValue(),
                    executorBuildItem.getExecutorProxy());
            // Exact match for resources matched to the root path
            routes.produce(new RouteBuildItem(rootPath, handler, false));
            String matchPath = rootPath;
            if (matchPath.endsWith("/")) {
                matchPath += "*";
            } else {
                matchPath += "/*";
            }
            // Match paths that begin with the deployment path
            routes.produce(new RouteBuildItem(matchPath, handler, false));
        } else {
            Consumer<Route> ut = binding.start(vertx.getVertx(),
                    shutdown,
                    beanContainer.getValue(),
                    executorBuildItem.getExecutorProxy());

            defaultRoutes.produce(new DefaultRouteBuildItem(ut));
        }
    }
}
