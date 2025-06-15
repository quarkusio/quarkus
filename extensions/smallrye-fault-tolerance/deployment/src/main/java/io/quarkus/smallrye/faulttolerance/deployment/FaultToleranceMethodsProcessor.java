package io.quarkus.smallrye.faulttolerance.deployment;

import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;

// SmallRye Fault Tolerance has made a mistake and allowed `@Blocking` and `@NonBlocking`
// on regular, non-entrypoint methods; this is now deprecated and will be disallowed
// in the future, but for now, we need to allow this too
public class FaultToleranceMethodsProcessor {
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem eventConsumerMethods() {
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                Set<DotName> classAnnotations = method.declaringClass().annotationsMap().keySet();
                return classAnnotations.contains(DotNames.ASYNCHRONOUS) || classAnnotations.contains(DotNames.BULKHEAD)
                        || classAnnotations.contains(DotNames.CIRCUIT_BREAKER)
                        || classAnnotations.contains(DotNames.FALLBACK)
                        || classAnnotations.contains(DotNames.RATE_LIMIT) || classAnnotations.contains(DotNames.RETRY)
                        || classAnnotations.contains(DotNames.TIMEOUT);
            }
        });
    }
}
