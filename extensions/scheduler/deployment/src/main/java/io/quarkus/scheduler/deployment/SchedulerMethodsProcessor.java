package io.quarkus.scheduler.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;

public class SchedulerMethodsProcessor {
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem schedulerMethods() {
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                return method.hasDeclaredAnnotation(SchedulerDotNames.SCHEDULED_NAME)
                        || method.hasDeclaredAnnotation(SchedulerDotNames.SCHEDULES_NAME);
            }
        });
    }
}
