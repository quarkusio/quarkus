package io.quarkus.smallrye.reactivemessaging.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;

public class ReactiveMessagingMethodsProcessor {
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem reactiveMessagingMethods() {
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                return method.hasDeclaredAnnotation(ReactiveMessagingDotNames.INCOMING)
                        || method.hasDeclaredAnnotation(ReactiveMessagingDotNames.INCOMINGS)
                        || method.hasDeclaredAnnotation(ReactiveMessagingDotNames.OUTGOING)
                        || method.hasDeclaredAnnotation(ReactiveMessagingDotNames.OUTGOINGS);
            }
        });
    }
}
