package io.quarkus.deployment.recording;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

import io.quarkus.runtime.annotations.IgnoreProperty;
import io.quarkus.runtime.annotations.RecordableConstructor;

final class RecordingAnnotationsUtil {

    private static final Class<? extends Annotation>[] IGNORED_PROPERTY_ANNOTATIONS;
    private static final Class<? extends Annotation>[] RECORDABLE_CONSTRUCTOR_ANNOTATIONS;

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

        IGNORED_PROPERTY_ANNOTATIONS = ignoredPropertyAnnotations.toArray(new Class[0]);
        RECORDABLE_CONSTRUCTOR_ANNOTATIONS = recordableConstructorAnnotations.toArray(new Class[0]);
    }

    private RecordingAnnotationsUtil() {
    }

    static boolean isIgnored(final AccessibleObject object) {
        return annotationsMatch(object.getDeclaredAnnotations(), IGNORED_PROPERTY_ANNOTATIONS);
    }

    static boolean isRecordableConstructor(final Constructor<?> ctor) {
        return annotationsMatch(ctor.getDeclaredAnnotations(), RECORDABLE_CONSTRUCTOR_ANNOTATIONS);
    }

    private static boolean annotationsMatch(
            final Annotation[] declaredAnnotations,
            final Class<? extends Annotation>[] typesToCheck) {
        for (Class<? extends Annotation> annotation : typesToCheck) {
            for (Annotation declaredAnnotation : declaredAnnotations) {
                if (declaredAnnotation.annotationType().equals(annotation)) {
                    return true;
                }
            }
        }
        return false;
    }

}
