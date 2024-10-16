package io.quarkus.resteasy.reactive.common.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;

public class JaxrsMethodsProcessor {
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem jaxrsMethods() {
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                // looking for `@Path` on the declaring class is enough
                // to avoid having to process inherited JAX-RS annotations
                if (method.declaringClass().hasDeclaredAnnotation(ResteasyReactiveDotNames.PATH)) {
                    return true;
                }

                // we currently don't handle custom @HttpMethod annotations, should be fine most of the time
                return method.hasDeclaredAnnotation(ResteasyReactiveDotNames.PATH)
                        || method.hasDeclaredAnnotation(ResteasyReactiveDotNames.GET)
                        || method.hasDeclaredAnnotation(ResteasyReactiveDotNames.POST)
                        || method.hasDeclaredAnnotation(ResteasyReactiveDotNames.PUT)
                        || method.hasDeclaredAnnotation(ResteasyReactiveDotNames.DELETE)
                        || method.hasDeclaredAnnotation(ResteasyReactiveDotNames.PATCH)
                        || method.hasDeclaredAnnotation(ResteasyReactiveDotNames.HEAD)
                        || method.hasDeclaredAnnotation(ResteasyReactiveDotNames.OPTIONS);
            }
        });
    }
}
