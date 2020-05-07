package io.quarkus.panache.rest.common.deployment.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.panache.rest.common.PanacheRestResource;

public class PanacheRestResourceAccessor {

    private static final DotName PANACHE_REST_RESOURCE_ANNOTATION = DotName.createSimple(PanacheRestResource.class.getName());

    private final IndexView index;

    public PanacheRestResourceAccessor(IndexView index) {
        this.index = index;
    }

    public boolean isHal(ClassInfo controllerInterface, String methodName, String... methodParameterTypes) {
        AnnotationInstance annotation = getActiveAnnotation(controllerInterface, methodName, methodParameterTypes);

        return annotation != null
                && annotation.value("hal") != null
                && annotation.value("hal").asBoolean();
    }

    public boolean isExposed(ClassInfo controllerInterface, String methodName, String... methodParameterTypes) {
        AnnotationInstance annotation = getActiveAnnotation(controllerInterface, methodName, methodParameterTypes);

        return annotation == null
                || annotation.value("exposed") == null
                || annotation.value("exposed").asBoolean();
    }

    private AnnotationInstance getActiveAnnotation(ClassInfo controllerInterface, String methodName,
            String... methodParameterTypes) {
        Optional<MethodInfo> optionalMethod = getMethodInfo(controllerInterface, methodName, methodParameterTypes);
        if (optionalMethod.isPresent() && optionalMethod.get().hasAnnotation(PANACHE_REST_RESOURCE_ANNOTATION)) {
            return optionalMethod.get().annotation(PANACHE_REST_RESOURCE_ANNOTATION);
        }
        if (controllerInterface.classAnnotation(PANACHE_REST_RESOURCE_ANNOTATION) != null) {
            return controllerInterface.classAnnotation(PANACHE_REST_RESOURCE_ANNOTATION);
        }
        if (controllerInterface.superName() != null) {
            ClassInfo superControllerInterface = index.getClassByName(controllerInterface.superName());
            if (superControllerInterface != null) {
                return getActiveAnnotation(superControllerInterface, methodName, methodParameterTypes);
            }
        }

        return null;
    }

    private Optional<MethodInfo> getMethodInfo(ClassInfo classInfo, String methodName, String... methodParameterTypes) {
        return classInfo.methods()
                .stream()
                .filter(methodInfo -> methodName.equals(methodInfo.name()))
                .filter(methodInfo -> doParametersMatch(methodInfo, methodParameterTypes))
                .findFirst();
    }

    private boolean doParametersMatch(MethodInfo methodInfo, String... methodParameterTypes) {
        if (methodInfo.parameters().size() != methodParameterTypes.length) {
            return false;
        }

        List<String> actualParameterTypes = methodInfo.parameters()
                .stream()
                .map(Type::name)
                .map(DotName::toString)
                .collect(Collectors.toList());

        return actualParameterTypes.equals(Arrays.asList(methodParameterTypes));
    }
}
