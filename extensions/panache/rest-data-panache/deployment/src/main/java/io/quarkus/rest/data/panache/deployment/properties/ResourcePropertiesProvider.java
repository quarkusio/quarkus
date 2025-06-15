package io.quarkus.rest.data.panache.deployment.properties;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.rest.data.panache.deployment.utils.ResourceName;

public class ResourcePropertiesProvider {

    private static final DotName RESOURCE_PROPERTIES_ANNOTATION = DotName
            .createSimple(io.quarkus.rest.data.panache.ResourceProperties.class.getName());

    private static final DotName METHOD_PROPERTIES_ANNOTATION = DotName
            .createSimple(io.quarkus.rest.data.panache.MethodProperties.class.getName());

    private static final List<String> ANNOTATIONS_TO_COPY = List.of(RolesAllowed.class.getPackageName(),
            // To also support `@EndpointDisabled` if used
            "io.quarkus.resteasy.reactive.server");

    private final IndexView index;

    public ResourcePropertiesProvider(IndexView index) {
        this.index = index;
    }

    /**
     * Find resource and method properties annotations used by a given interface and build {@link ResourceProperties}
     * instance.
     */
    public ResourceProperties getFromClass(String resourceClass) {
        DotName resourceClassName = DotName.createSimple(resourceClass);
        AnnotationInstance annotation = findResourcePropertiesAnnotation(resourceClassName);
        return new ResourceProperties(isExposed(annotation), getPath(annotation, resourceClass), isPaged(annotation),
                isHal(annotation), getHalCollectionName(annotation, resourceClass), getRolesAllowed(annotation),
                collectAnnotationsToCopy(resourceClassName), collectMethodProperties(resourceClassName));
    }

    private Collection<AnnotationInstance> collectAnnotationsToCopy(DotName className) {
        Set<AnnotationInstance> annotations = new HashSet<>();
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            return annotations;
        }

        for (AnnotationInstance annotation : classInfo.declaredAnnotations()) {
            if (ANNOTATIONS_TO_COPY.stream().anyMatch(annotation.name().toString()::startsWith)) {
                annotations.add(annotation);
            }
        }

        if (classInfo.superName() != null) {
            annotations.addAll(collectAnnotationsToCopy(classInfo.superName()));
        }

        return annotations;
    }

    private AnnotationInstance findResourcePropertiesAnnotation(DotName className) {
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            return null;
        }
        if (classInfo.declaredAnnotation(RESOURCE_PROPERTIES_ANNOTATION) != null) {
            return classInfo.declaredAnnotation(RESOURCE_PROPERTIES_ANNOTATION);
        }
        if (classInfo.superName() != null) {
            return findResourcePropertiesAnnotation(classInfo.superName());
        }
        return null;
    }

    private Map<String, MethodProperties> collectMethodProperties(DotName className) {
        Map<String, MethodProperties> methodProperties = new HashMap<>();
        ClassInfo classInfo = index.getClassByName(className);
        if (classInfo == null) {
            return methodProperties;
        }

        for (MethodInfo method : classInfo.methods()) {
            AnnotationInstance annotation = method.annotation(METHOD_PROPERTIES_ANNOTATION);
            Set<AnnotationInstance> annotationsToCopy = new HashSet<>();
            for (AnnotationInstance ann : method.annotations()) {
                if (ANNOTATIONS_TO_COPY.stream().anyMatch(ann.name().toString()::startsWith)) {
                    annotationsToCopy.add(ann);
                }
            }

            if (!methodProperties.containsKey(method.name()) && (annotation != null || !annotationsToCopy.isEmpty())) {
                methodProperties.put(method.name(), getMethodProperties(annotation, annotationsToCopy));
            }
        }
        if (classInfo.superName() != null) {
            methodProperties.putAll(collectMethodProperties(classInfo.superName()));
        }

        return methodProperties;
    }

    private MethodProperties getMethodProperties(AnnotationInstance annotation,
            Set<AnnotationInstance> annotationsToCopy) {
        return new MethodProperties(isExposed(annotation), getPath(annotation), getRolesAllowed(annotation),
                annotationsToCopy);
    }

    private boolean isHal(AnnotationInstance annotation) {
        return annotation != null && annotation.value("hal") != null && annotation.value("hal").asBoolean();
    }

    private boolean isPaged(AnnotationInstance annotation) {
        return annotation == null || annotation.value("paged") == null || annotation.value("paged").asBoolean();
    }

    private boolean isExposed(AnnotationInstance annotation) {
        return annotation == null || annotation.value("exposed") == null || annotation.value("exposed").asBoolean();
    }

    private String getPath(AnnotationInstance annotation) {
        if (annotation != null && annotation.value("path") != null) {
            return annotation.value("path").asString();
        }
        return "";
    }

    private String getPath(AnnotationInstance annotation, String resourceClass) {
        if (annotation != null && annotation.value("path") != null) {
            return annotation.value("path").asString();
        }
        return ResourceName.fromClass(resourceClass);
    }

    private String getHalCollectionName(AnnotationInstance annotation, String resourceClass) {
        if (annotation != null && annotation.value("halCollectionName") != null) {
            return annotation.value("halCollectionName").asString();
        }
        return ResourceName.fromClass(resourceClass);
    }

    private String[] getRolesAllowed(AnnotationInstance annotation) {
        if (annotation != null && annotation.value("rolesAllowed") != null) {
            return annotation.value("rolesAllowed").asStringArray();
        }

        return new String[0];
    }
}
