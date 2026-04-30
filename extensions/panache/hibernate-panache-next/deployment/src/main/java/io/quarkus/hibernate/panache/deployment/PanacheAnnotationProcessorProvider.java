package io.quarkus.hibernate.panache.deployment;

import java.util.Collections;
import java.util.List;

import io.quarkus.deployment.dev.AnnotationProcessorProvider;

public class PanacheAnnotationProcessorProvider implements AnnotationProcessorProvider {

    @Override
    public List<String> getAnnotationProcessors() {
        return Collections.singletonList("org.hibernate.orm:hibernate-processor");
    }
}
