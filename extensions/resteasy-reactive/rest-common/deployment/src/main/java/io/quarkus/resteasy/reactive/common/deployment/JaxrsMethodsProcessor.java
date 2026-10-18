package io.quarkus.resteasy.reactive.common.deployment;

import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;

public class JaxrsMethodsProcessor {
    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem jaxrsMethods(BeanArchiveIndexBuildItem beanArchiveIndex) {
        IndexView index = beanArchiveIndex.getIndex();
        return new ExecutionModelAnnotationsAllowedBuildItem(new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo method) {
                // looking for `@Path` on the declaring class is enough
                // to avoid having to process inherited JAX-RS annotations
                if (method.declaringClass().hasDeclaredAnnotation(ResteasyReactiveDotNames.PATH)) {
                    return true;
                }
                if (isJaxrsResourceMethod(method)) {
                    return true;
                }

                // also look at interfaces implemented by the method's declaringClass
                for (Type interfaceType : method.declaringClass().interfaceTypes()) {
                    ClassInfo interfaceInfo = index.getClassByName(interfaceType.name());
                    if (interfaceInfo != null) {
                        if (interfaceInfo.hasDeclaredAnnotation(ResteasyReactiveDotNames.PATH)) {
                            return true;
                        }
                        MethodInfo overriddenMethodInfo = interfaceInfo.method(method.name(),
                                method.parameterTypes().toArray(new Type[0]));
                        if (overriddenMethodInfo != null && isJaxrsResourceMethod(overriddenMethodInfo)) {
                            return true;
                        }
                    }
                }

                return false;
            }

            private boolean isJaxrsResourceMethod(MethodInfo method) {
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
