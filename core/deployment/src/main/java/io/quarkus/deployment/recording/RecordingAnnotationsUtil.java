package io.quarkus.deployment.recording;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import io.quarkus.runtime.annotations.IgnoreProperty;

final class RecordingAnnotationsUtil {

    static final List<Class<? extends Annotation>> IGNORED_PROPERTY_ANNOTATIONS;

    static {
        Set<Class<? extends Annotation>> ignoredPropertyAnnotations = new HashSet<>();
        ignoredPropertyAnnotations.add(IgnoreProperty.class);
        for (RecordingAnnotationsProvider provider : ServiceLoader.load(RecordingAnnotationsProvider.class)) {
            Class<? extends Annotation> ignoredProperty = provider.ignoredProperty();
            if (ignoredProperty != null) {
                ignoredPropertyAnnotations.add(ignoredProperty);
            }
        }
        IGNORED_PROPERTY_ANNOTATIONS = List.copyOf(ignoredPropertyAnnotations);
    }

    private RecordingAnnotationsUtil() {
    }

    static boolean isIgnored(AccessibleObject object) {
        for (int i = 0; i < IGNORED_PROPERTY_ANNOTATIONS.size(); i++) {
            Class<? extends Annotation> annotation = IGNORED_PROPERTY_ANNOTATIONS.get(i);
            if (object.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }
}
