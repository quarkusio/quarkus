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

    public boolean isHal(String type) {
        AnnotationInstance annotation = getAnnotation(DotName.createSimple(type));
        return annotation != null
                && annotation.value("hal") != null
                && annotation.value("hal").asBoolean();
    }

    public String path(String type) {
        AnnotationInstance annotation = getAnnotation(DotName.createSimple(type));
        if (annotation == null || annotation.value("path") == null || "".equals(annotation.value("path").asString())) {
            return ResourceName.fromClass(type);
        }
        return annotation.value("path").asString();
    }

    public boolean isPaged(String type) {
        AnnotationInstance annotation = getAnnotation(DotName.createSimple(type));
        return annotation == null
                || annotation.value("paged") == null
                || annotation.value("paged").asBoolean();
    }

    private AnnotationInstance getAnnotation(DotName type) {
        ClassInfo classInfo = index.getClassByName(type);
        if (classInfo == null) {
            return null;
        }
        if (classInfo.classAnnotation(RESOURCE_PROPERTIES_ANNOTATION) != null) {
            return classInfo.classAnnotation(RESOURCE_PROPERTIES_ANNOTATION);
        }
        if (classInfo.superName() != null) {
            return getAnnotation(classInfo.superName());
        }
        return null;
    }
}
