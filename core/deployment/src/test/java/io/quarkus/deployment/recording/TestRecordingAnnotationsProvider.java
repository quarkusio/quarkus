package io.quarkus.deployment.recording;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class TestRecordingAnnotationsProvider implements RecordingAnnotationsProvider {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface TestIgnoreProperty {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.CONSTRUCTOR)
    public @interface TestRecordableConstructor {
    }

    @Override
    public Class<? extends Annotation> ignoredProperty() {
        return TestIgnoreProperty.class;
    }

    @Override
    public Class<? extends Annotation> recordableConstructor() {
        return TestRecordableConstructor.class;
    }
}
