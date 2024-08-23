package io.quarkus.resteasy.common.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.MethodInfo;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyDotNames;

public class JaxrsMethodsProcessor {
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem jaxrsMethods() {
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                // looking for `@Path` on the declaring class is enough
                // to avoid having to process inherited JAX-RS annotations
                if (method.declaringClass().hasDeclaredAnnotation(ResteasyDotNames.PATH)) {
                    return true;
                }

                // we currently don't handle custom @HttpMethod annotations, should be fine most of the time
                return method.hasDeclaredAnnotation(ResteasyDotNames.PATH)
                        || method.hasDeclaredAnnotation(ResteasyDotNames.GET)
                        || method.hasDeclaredAnnotation(ResteasyDotNames.POST)
                        || method.hasDeclaredAnnotation(ResteasyDotNames.PUT)
                        || method.hasDeclaredAnnotation(ResteasyDotNames.DELETE)
                        || method.hasDeclaredAnnotation(ResteasyDotNames.PATCH)
                        || method.hasDeclaredAnnotation(ResteasyDotNames.HEAD)
                        || method.hasDeclaredAnnotation(ResteasyDotNames.OPTIONS);
            }
        });
    }
}
