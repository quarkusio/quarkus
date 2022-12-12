package io.quarkus.deployment.recording;

import java.lang.annotation.Annotation;

public interface RecordingAnnotationsProvider {

    Class<? extends Annotation> ignoredProperty();
}
