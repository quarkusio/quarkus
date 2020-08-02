package io.quarkus.rest.data.panache.deployment.properties;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.rest.data.panache.deployment.utils.ResourceName;

public class ResourcePropertiesAccessor {

    private static final DotName RESOURCE_PROPERTIES_ANNOTATION = DotName.createSimple(ResourceProperties.class.getName());

    private final IndexView index;

    public ResourcePropertiesAccessor(IndexView index) {
        this.index = index;
    }

    public boolean isHal(ClassInfo classInfo) {
        AnnotationInstance annotation = getAnnotation(classInfo);
        return annotation != null
                && annotation.value("hal") != null
                && annotation.value("hal").asBoolean();
    }

    public String path(ClassInfo classInfo) {
        AnnotationInstance annotation = getAnnotation(classInfo);
        if (annotation == null || annotation.value("path") == null || "".equals(annotation.value("path").asString())) {
            return ResourceName.fromClass(classInfo.simpleName());
        }
        return annotation.value("path").asString();
    }

    public boolean isPaged(ClassInfo classInfo) {
        AnnotationInstance annotation = getAnnotation(classInfo);
        return annotation == null
                || annotation.value("paged") == null
                || annotation.value("paged").asBoolean();
    }

    private AnnotationInstance getAnnotation(ClassInfo classInfo) {
        if (classInfo.classAnnotation(RESOURCE_PROPERTIES_ANNOTATION) != null) {
            return classInfo.classAnnotation(RESOURCE_PROPERTIES_ANNOTATION);
        }
        if (classInfo.superName() != null) {
            ClassInfo superControllerInterface = index.getClassByName(classInfo.superName());
            if (superControllerInterface != null) {
                return getAnnotation(superControllerInterface);
            }
        }
        return null;
    }
}
