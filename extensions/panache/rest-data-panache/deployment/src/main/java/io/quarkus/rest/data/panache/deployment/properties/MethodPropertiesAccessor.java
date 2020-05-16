package io.quarkus.rest.data.panache.deployment.properties;

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

import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.deployment.methods.MethodMetadata;

public class MethodPropertiesAccessor {

    private static final DotName OPERATION_PROPERTIES_ANNOTATION = DotName.createSimple(MethodProperties.class.getName());

    private final IndexView index;

    public MethodPropertiesAccessor(IndexView index) {
        this.index = index;
    }

    public boolean isExposed(ClassInfo classInfo, MethodMetadata methodMetadata) {
        AnnotationInstance annotation = getAnnotation(classInfo, methodMetadata);

        return annotation == null
                || annotation.value("exposed") == null
                || annotation.value("exposed").asBoolean();
    }

    public String getPath(ClassInfo classInfo, MethodMetadata methodMetadata) {
        AnnotationInstance annotation = getAnnotation(classInfo, methodMetadata);
        if (annotation == null || annotation.value("path") == null) {
            return "";
        }
        return annotation.value("path").asString();
    }

    public String getPath(ClassInfo classInfo, MethodMetadata methodMetadata, String lastSegment) {
        String path = getPath(classInfo, methodMetadata);
        if (path.endsWith("/")) {
            path = path.substring(0, path.lastIndexOf("/"));
        }
        if (lastSegment.startsWith("/")) {
            lastSegment = lastSegment.substring(1);
        }
        return String.join("/", path, lastSegment);
    }

    private AnnotationInstance getAnnotation(ClassInfo resourceInterface, MethodMetadata methodMetadata) {
        Optional<MethodInfo> optionalMethod = getMethodInfo(resourceInterface, methodMetadata);
        if (optionalMethod.isPresent() && optionalMethod.get().hasAnnotation(OPERATION_PROPERTIES_ANNOTATION)) {
            return optionalMethod.get().annotation(OPERATION_PROPERTIES_ANNOTATION);
        }
        if (resourceInterface.superName() != null) {
            ClassInfo superResourceInterface = index.getClassByName(resourceInterface.superName());
            if (superResourceInterface != null) {
                return getAnnotation(superResourceInterface, methodMetadata);
            }
        }

        return null;
    }

    private Optional<MethodInfo> getMethodInfo(ClassInfo classInfo, MethodMetadata methodMetadata) {
        return classInfo.methods()
                .stream()
                .filter(methodInfo -> methodMetadata.getName().equals(methodInfo.name()))
                .filter(methodInfo -> doParametersMatch(methodInfo, methodMetadata.getParameterTypes()))
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
