package io.quarkus.spring.security.deployment;

import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build Item recording all the methods that have been effectively annotated with
 * {@code @PreAuthorize}
 */
public final class SpringPreAuthorizeAnnotatedMethodBuildItem extends SimpleBuildItem {

    private final Map<MethodInfo, AnnotationInstance> methodToInstanceMap;

    public SpringPreAuthorizeAnnotatedMethodBuildItem(Map<MethodInfo, AnnotationInstance> methodToInstanceMap) {
        this.methodToInstanceMap = methodToInstanceMap;
    }

    public Map<MethodInfo, AnnotationInstance> getMethodToInstanceMap() {
        return methodToInstanceMap;
    }
}
