package io.quarkus.spring.data.rest.deployment;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import io.quarkus.rest.data.panache.deployment.properties.MethodProperties;
import io.quarkus.rest.data.panache.deployment.properties.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.ResourceName;

public abstract class ResourcePropertiesProvider {

    private static final DotName REST_RESOURCE_ANNOTATION = DotName.createSimple(RestResource.class.getName());

    private static final DotName REPOSITORY_REST_RESOURCE_ANNOTATION = DotName
            .createSimple(RepositoryRestResource.class.getName());

    private final IndexView index;

    private final boolean paged;

    public ResourcePropertiesProvider(IndexView index, boolean paged) {
        this.index = index;
        this.paged = paged;
    }

    protected abstract Map<String, Predicate<MethodInfo>> getMethodPredicates();

    public ResourceProperties getResourceProperties(String interfaceName) {
        DotName repositoryInterfaceName = DotName.createSimple(interfaceName);
        AnnotationInstance annotation = findClassAnnotation(repositoryInterfaceName);
        String resourcePath = getPath(annotation, ResourceName.fromClass(interfaceName));
        String halCollectionName = getHalCollectionName(annotation, ResourceName.fromClass(interfaceName));

        return new ResourceProperties(isExposed(annotation), resourcePath, paged, true, halCollectionName,
                getMethodProperties(repositoryInterfaceName));
    }

    private Map<String, MethodProperties> getMethodProperties(DotName interfaceName) {
        Map<String, MethodProperties> methodPropertiesMap = new HashMap<>();
        for (Map.Entry<String, Predicate<MethodInfo>> method : getMethodPredicates().entrySet()) {
            AnnotationInstance annotation = findMethodAnnotation(interfaceName, method.getValue());
            if (annotation != null) {
                methodPropertiesMap.putIfAbsent(method.getKey(), getMethodProperties(annotation));
            }
        }
        return methodPropertiesMap;
    }

    private MethodProperties getMethodProperties(AnnotationInstance annotation) {
        return new MethodProperties(isExposed(annotation), getPath(annotation, ""));
    }

    private AnnotationInstance findClassAnnotation(DotName interfaceName) {
        ClassInfo classInfo = index.getClassByName(interfaceName);
        if (classInfo == null) {
            return null;
        }
        if (classInfo.classAnnotation(REPOSITORY_REST_RESOURCE_ANNOTATION) != null) {
            return classInfo.classAnnotation(REPOSITORY_REST_RESOURCE_ANNOTATION);
        }
        if (classInfo.classAnnotation(REST_RESOURCE_ANNOTATION) != null) {
            return classInfo.classAnnotation(REST_RESOURCE_ANNOTATION);
        }
        if (classInfo.superName() != null) {
            return findClassAnnotation(classInfo.superName());
        }
        return null;
    }

    private AnnotationInstance findMethodAnnotation(DotName interfaceName, Predicate<MethodInfo> methodPredicate) {
        ClassInfo classInfo = index.getClassByName(interfaceName);
        if (classInfo == null) {
            return null;
        }
        for (MethodInfo method : classInfo.methods()) {
            if (methodPredicate.test(method)) {
                if (method.hasAnnotation(REPOSITORY_REST_RESOURCE_ANNOTATION)) {
                    return method.annotation(REPOSITORY_REST_RESOURCE_ANNOTATION);
                } else if (method.hasAnnotation(REST_RESOURCE_ANNOTATION)) {
                    return method.annotation(REST_RESOURCE_ANNOTATION);
                }
            }
        }
        if (classInfo.superName() != null) {
            return findMethodAnnotation(classInfo.superName(), methodPredicate);
        }
        return null;
    }

    private boolean isExposed(AnnotationInstance annotation) {
        return annotation == null
                || annotation.value("exported") == null
                || annotation.value("exported").asBoolean();
    }

    private String getPath(AnnotationInstance annotation, String defaultValue) {
        if (annotation != null && annotation.value("path") != null) {
            return annotation.value("path").asString();
        }
        return defaultValue;
    }

    private String getHalCollectionName(AnnotationInstance annotation, String defaultValue) {
        if (annotation != null && annotation.value("collectionResourceRel") != null) {
            return annotation.value("collectionResourceRel").asString();
        }
        return defaultValue;
    }
}
