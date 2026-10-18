package io.quarkus.vertx.web.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;

public class ReactiveRoutesMethodsProcessor {
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem reactiveRoutesMethods() {
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                return method.hasDeclaredAnnotation(DotNames.ROUTE)
                        || method.hasDeclaredAnnotation(DotNames.ROUTES);
            }
        });
    }
}
