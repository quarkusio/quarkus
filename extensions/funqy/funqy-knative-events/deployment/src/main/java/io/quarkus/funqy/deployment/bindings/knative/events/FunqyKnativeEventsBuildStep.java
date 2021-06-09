package io.quarkus.funqy.deployment.bindings.knative.events;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExecutorBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.funqy.deployment.FunctionBuildItem;
import io.quarkus.funqy.deployment.FunctionInitializedBuildItem;
import io.quarkus.funqy.runtime.FunqyConfig;
import io.quarkus.funqy.runtime.bindings.knative.events.FunqyKnativeEventsConfig;
import io.quarkus.funqy.runtime.bindings.knative.events.KnativeEventsBindingRecorder;
import io.quarkus.jackson.runtime.ObjectMapperProducer;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class FunqyKnativeEventsBuildStep {
    private static final Logger log = Logger.getLogger(FunqyKnativeEventsBuildStep.class);
    public static final String FUNQY_KNATIVE_FEATURE = "funqy-knative-events";

    @BuildStep
    public void markObjectMapper(BuildProducer<UnremovableBeanBuildItem> unremovable) {
        unremovable.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(ObjectMapper.class.getName())));
        unremovable.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(ObjectMapperProducer.class.getName())));
    }

    @BuildStep()
    @Record(STATIC_INIT)
    public void staticInit(KnativeEventsBindingRecorder binding,
            List<FunctionBuildItem> functions,
            Optional<FunctionInitializedBuildItem> hasFunctions,
            BeanContainerBuildItem beanContainer // make sure bc is initialized
    ) throws Exception {
        if (!hasFunctions.isPresent() || hasFunctions.get() == null)
            return;
        binding.init();
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void boot(ShutdownContextBuildItem shutdown,
            FunqyConfig funqyConfig,
            FunqyKnativeEventsConfig eventsConfig,
            KnativeEventsBindingRecorder binding,
            Optional<FunctionInitializedBuildItem> hasFunctions,
            List<FunctionBuildItem> functions,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<RouteBuildItem> routes,
            CoreVertxBuildItem vertx,
            BeanContainerBuildItem beanContainer,
            HttpBuildTimeConfig httpConfig,
            ExecutorBuildItem executorBuildItem) throws Exception {
        if (!hasFunctions.isPresent() || hasFunctions.get() == null)
            return;

        feature.produce(new FeatureBuildItem(FUNQY_KNATIVE_FEATURE));

        String rootPath = httpConfig.rootPath;
        if (rootPath == null) {
            rootPath = "/";
        } else if (!rootPath.endsWith("/")) {
            rootPath += "/";
        }

        Handler<RoutingContext> handler = binding.start(rootPath, funqyConfig, eventsConfig, vertx.getVertx(),
                shutdown,
                beanContainer.getValue(),
                executorBuildItem.getExecutorProxy());

        routes.produce(RouteBuildItem.builder().route("/").handler(handler).build());

        for (FunctionBuildItem function : functions) {
            String name = function.getFunctionName() == null ? function.getMethodName() : function.getFunctionName();
            String path = "/" + name;
            routes.produce(RouteBuildItem.builder().route(path).handler(handler).build());
        }
    }
}
