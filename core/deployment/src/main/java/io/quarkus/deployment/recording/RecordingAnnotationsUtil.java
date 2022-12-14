package io.quarkus.deployment.recording;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import io.quarkus.runtime.annotations.IgnoreProperty;
import io.quarkus.runtime.annotations.RecordableConstructor;

final class RecordingAnnotationsUtil {

    static final List<Class<? extends Annotation>> IGNORED_PROPERTY_ANNOTATIONS;
    static final List<Class<? extends Annotation>> RECORDABLE_CONSTRUCTOR_ANNOTATIONS;

    static {
        Set<Class<? extends Annotation>> ignoredPropertyAnnotations = new HashSet<>();
        ignoredPropertyAnnotations.add(IgnoreProperty.class);
        Set<Class<? extends Annotation>> recordableConstructorAnnotations = new HashSet<>();
        recordableConstructorAnnotations.add(RecordableConstructor.class);

        for (RecordingAnnotationsProvider provider : ServiceLoader.load(RecordingAnnotationsProvider.class)) {
            Class<? extends Annotation> ignoredProperty = provider.ignoredProperty();
            if (ignoredProperty != null) {
                ignoredPropertyAnnotations.add(ignoredProperty);
            }
            Class<? extends Annotation> recordableConstructor = provider.recordableConstructor();
            if (recordableConstructor != null) {
                recordableConstructorAnnotations.add(recordableConstructor);
            }
        }

        IGNORED_PROPERTY_ANNOTATIONS = List.copyOf(ignoredPropertyAnnotations);
        RECORDABLE_CONSTRUCTOR_ANNOTATIONS = List.copyOf(recordableConstructorAnnotations);
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

    static boolean isRecordableConstructor(Constructor<?> ctor) {
        for (int i = 0; i < RECORDABLE_CONSTRUCTOR_ANNOTATIONS.size(); i++) {
            Class<? extends Annotation> annotation = RECORDABLE_CONSTRUCTOR_ANNOTATIONS.get(i);
            if (ctor.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        return false;
    }
}
