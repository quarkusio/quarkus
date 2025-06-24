package io.quarkus.vertx.http.deployment.devmode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.deployment.ArcConfig;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.DecoratorInfo;
import io.quarkus.arc.processor.InterceptorInfo;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.devmode.ArcDevRecorder;

public class ArcDevProcessor {
    private static final String BEAN_DEPENDENCIES = "io.quarkus.arc.beanDependencies";

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void registerRoutes(ArcConfig arcConfig, ArcDevRecorder recorder,
            BuildProducer<RouteBuildItem> routes,
            BuildProducer<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationErrorBuildItem> errors) {

        List<BeanInfo> removed = new ArrayList<>();
        Collection<InterceptorInfo> removedInterceptors = validationPhase.getContext()
                .get(BuildExtension.Key.REMOVED_INTERCEPTORS);
        if (removedInterceptors != null) {
            removed.addAll(removedInterceptors);
        }
        Collection<DecoratorInfo> removedDecorators = validationPhase.getContext().get(BuildExtension.Key.REMOVED_DECORATORS);
        if (removedDecorators != null) {
            removed.addAll(removedDecorators);
        }
        List<String[]> removedInterceptorsDecorators;
        if (removed.isEmpty()) {
            removedInterceptorsDecorators = Collections.emptyList();
        } else {
            removedInterceptorsDecorators = new ArrayList<>();
            for (BeanInfo r : removed) {
                removedInterceptorsDecorators.add(new String[] { r.isInterceptor() ? InjectableBean.Kind.INTERCEPTOR.toString()
                        : InjectableBean.Kind.DECORATOR.toString(), r.getImplClazz().name().toString() });
            }
        }

        String basePath = "arc";
        String beansPath = basePath + "/beans";
        String removedBeansPath = basePath + "/removed-beans";
        String observersPath = basePath + "/observers";
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route(basePath)
                .displayOnNotFoundPage("CDI Overview")
                .handler(recorder.createSummaryHandler(getConfigProperties(arcConfig),
                        nonApplicationRootPathBuildItem.getNonApplicationRootPath(),
                        removedInterceptorsDecorators.size()))
                .build());
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route(beansPath)
                .displayOnNotFoundPage("Active CDI Beans")
                .handler(recorder.createBeansHandler(BEAN_DEPENDENCIES)).build());

        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route(removedBeansPath)
                .displayOnNotFoundPage("Removed CDI Beans")
                .handler(recorder.createRemovedBeansHandler(removedInterceptorsDecorators)).build());
        routes.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .route(observersPath)
                .displayOnNotFoundPage("Active CDI Observers")
                .handler(recorder.createObserversHandler()).build());
    }

    // Note that we can't turn ArcConfig into BUILD_AND_RUN_TIME_FIXED because it's referencing IndexDependencyConfig
    // And we can't split the config due to compatibility reasons
    private Map<String, String> getConfigProperties(ArcConfig arcConfig) {
        Map<String, String> props = new HashMap<>();
        props.put("quarkus.arc.remove-unused-beans", arcConfig.removeUnusedBeans());
        props.put("quarkus.arc.unremovable-types", arcConfig.unremovableTypes().map(Object::toString).orElse(""));
        props.put("quarkus.arc.detect-unused-false-positives", "" + arcConfig.detectUnusedFalsePositives());
        props.put("quarkus.arc.transform-unproxyable-classes", "" + arcConfig.transformUnproxyableClasses());
        props.put("quarkus.arc.auto-inject-fields", "" + arcConfig.autoInjectFields());
        props.put("quarkus.arc.auto-producer-methods", "" + arcConfig.autoProducerMethods());
        props.put("quarkus.arc.selected-alternatives", "" + arcConfig.selectedAlternatives().map(Object::toString).orElse(""));
        props.put("quarkus.arc.exclude-types", "" + arcConfig.excludeTypes().map(Object::toString).orElse(""));
        return props;
    }
}
