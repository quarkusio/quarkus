package io.quarkus.vertx.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;

public class EventConsumerMethodsProcessor {
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem eventConsumerMethods() {
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                return method.hasDeclaredAnnotation(VertxConstants.CONSUME_EVENT);
            }
        });
    }
}
