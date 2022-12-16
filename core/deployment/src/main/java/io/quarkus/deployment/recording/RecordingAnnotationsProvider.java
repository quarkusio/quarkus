package io.quarkus.deployment.recording;

import java.lang.annotation.Annotation;

public interface RecordingAnnotationsProvider {

    default Class<? extends Annotation> ignoredProperty() {
        return null;
    }

    default Class<? extends Annotation> recordableConstructor() {
        return null;
    }
}
