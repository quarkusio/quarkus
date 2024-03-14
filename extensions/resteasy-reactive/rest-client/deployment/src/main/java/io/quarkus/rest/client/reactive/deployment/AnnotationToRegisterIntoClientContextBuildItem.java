package io.quarkus.rest.client.reactive.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A Build Item that is used to register annotations that are used by the client to register services into the client context.
 */
public final class AnnotationToRegisterIntoClientContextBuildItem extends MultiBuildItem {

    private final DotName annotation;
    private final Class<?> expectedReturnType;

    public AnnotationToRegisterIntoClientContextBuildItem(DotName annotation, Class<?> expectedReturnType) {
        this.annotation = annotation;
        this.expectedReturnType = expectedReturnType;
    }

    public DotName getAnnotation() {
        return annotation;
    }

    public Class<?> getExpectedReturnType() {
        return expectedReturnType;
    }
}
