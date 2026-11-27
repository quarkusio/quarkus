package io.quarkus.resteasy.reactive.common.deployment;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
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

                Type[] parameterTypes = method.parameterTypes().toArray(new Type[0]);
                Set<DotName> visited = new HashSet<>();
                Deque<ClassInfo> toVisit = new ArrayDeque<>();
                addSupertypes(method.declaringClass(), toVisit);
                while (!toVisit.isEmpty()) {
                    ClassInfo supertype = toVisit.poll();
                    if (!visited.add(supertype.name())) {
                        continue;
                    }
                    if (supertype.hasDeclaredAnnotation(ResteasyReactiveDotNames.PATH)) {
                        return true;
                    }
                    MethodInfo overriddenMethodInfo = supertype.method(method.name(), parameterTypes);
                    if (overriddenMethodInfo != null && isJaxrsResourceMethod(overriddenMethodInfo)) {
                        return true;
                    }
                    addSupertypes(supertype, toVisit);
                }

                return false;
            }

            private void addSupertypes(ClassInfo clazz, Deque<ClassInfo> toVisit) {
                DotName superName = clazz.superName();
                if (superName != null && !ResteasyReactiveDotNames.OBJECT.equals(superName)) {
                    ClassInfo superClass = index.getClassByName(superName);
                    if (superClass != null) {
                        toVisit.add(superClass);
                    }
                }
                for (Type interfaceType : clazz.interfaceTypes()) {
                    ClassInfo interfaceInfo = index.getClassByName(interfaceType.name());
                    if (interfaceInfo != null) {
                        toVisit.add(interfaceInfo);
                    }
                }
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
