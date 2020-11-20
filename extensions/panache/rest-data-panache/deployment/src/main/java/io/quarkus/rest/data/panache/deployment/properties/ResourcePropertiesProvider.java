package io.quarkus.rest.data.panache.deployment.properties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.rest.data.panache.deployment.utils.ResourceName;

public class ResourcePropertiesProvider {

    private static final DotName RESOURCE_PROPERTIES_ANNOTATION = DotName
            .createSimple(io.quarkus.rest.data.panache.ResourceProperties.class.getName());

    private static final DotName METHOD_PROPERTIES_ANNOTATION = DotName.createSimple(
            io.quarkus.rest.data.panache.MethodProperties.class.getName());

    private final IndexView index;

    public ResourcePropertiesProvider(IndexView index) {
        this.index = index;
    }

    /**
     * Find resource and method properties annotations used by a given interface
     * and build {@link ResourceProperties} instance.
     */
    public ResourceProperties getForInterface(String resourceInterface) {
        DotName resourceInterfaceName = DotName.createSimple(resourceInterface);
        AnnotationInstance annotation = findResourcePropertiesAnnotation(resourceInterfaceName);

        return new ResourceProperties(isHal(annotation), getPath(annotation, resourceInterface),
                isPaged(annotation), getHalCollectionName(annotation, resourceInterface),
                getMethodPropertiesInfoMap(resourceInterfaceName));
    }

    private AnnotationInstance findResourcePropertiesAnnotation(DotName className) {
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            return null;
        }
        if (classInfo.classAnnotation(RESOURCE_PROPERTIES_ANNOTATION) != null) {
            return classInfo.classAnnotation(RESOURCE_PROPERTIES_ANNOTATION);
        }
        if (classInfo.superName() != null) {
            return findResourcePropertiesAnnotation(classInfo.superName());
        }
        return null;
    }

    private Map<String, MethodProperties> getMethodPropertiesInfoMap(DotName className) {
        Set<String> methodNames = new HashSet<>();
        Map<String, AnnotationInstance> annotations = new HashMap<>();
        collectMethods(className, methodNames, annotations);

        Map<String, MethodProperties> methodPropertiesInfoMap = new HashMap<>();
        for (String methodName : methodNames) {
            methodPropertiesInfoMap.put(methodName, getMethodPropertiesInfo(annotations.get(methodName)));
        }

        return methodPropertiesInfoMap;
    }

    private void collectMethods(DotName className, Set<String> methodNames,
            Map<String, AnnotationInstance> annotations) {
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            return;
        }
        for (MethodInfo method : classInfo.methods()) {
            if (method.hasAnnotation(METHOD_PROPERTIES_ANNOTATION)) {
                annotations.putIfAbsent(method.name(), method.annotation(METHOD_PROPERTIES_ANNOTATION));
            }
            methodNames.add(method.name());
        }
        if (classInfo.superName() != null) {
            collectMethods(classInfo.superName(), methodNames, annotations);
        }
    }

    private MethodProperties getMethodPropertiesInfo(AnnotationInstance annotation) {
        return new MethodProperties(isExposed(annotation), getPath(annotation));
    }

    private boolean isHal(AnnotationInstance annotation) {
        return annotation != null
                && annotation.value("hal") != null
                && annotation.value("hal").asBoolean();
    }

    private boolean isPaged(AnnotationInstance annotation) {
        return annotation == null
                || annotation.value("paged") == null
                || annotation.value("paged").asBoolean();
    }

    private boolean isExposed(AnnotationInstance annotation) {
        return annotation == null
                || annotation.value("exposed") == null
                || annotation.value("exposed").asBoolean();
    }

    private String getPath(AnnotationInstance annotation) {
        if (annotation != null && annotation.value("path") != null) {
            return annotation.value("path").asString();
        }
        return "";
    }

    private String getPath(AnnotationInstance annotation, String resourceInterface) {
        if (annotation != null && annotation.value("path") != null) {
            return annotation.value("path").asString();
        }
        return ResourceName.fromClass(resourceInterface);
    }

    private String getHalCollectionName(AnnotationInstance annotation, String resourceInterface) {
        if (annotation != null && annotation.value("halCollectionName") != null) {
            return annotation.value("halCollectionName").asString();
        }
        return ResourceName.fromClass(resourceInterface);
    }
}
