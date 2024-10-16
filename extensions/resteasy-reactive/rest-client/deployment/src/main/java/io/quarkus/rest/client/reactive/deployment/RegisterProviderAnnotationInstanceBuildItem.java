package io.quarkus.rest.client.reactive.deployment;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.jboss.jandex.AnnotationInstance;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A Build Item that is used to capture the information of usages equivalent to {@code @RegisterProvider(SomeProvider.class)}.
 * The use of the build item facilitates support for use cases that need to have the same effect as
 * {@code @RegisterProvider(SomeProvider.class)},
 * but that don't actually use the {@link RegisterProvider} annotation.
 */
public final class RegisterProviderAnnotationInstanceBuildItem extends MultiBuildItem {

    private final String targetClass;
    private final AnnotationInstance annotationInstance;

    public RegisterProviderAnnotationInstanceBuildItem(String targetClass, AnnotationInstance annotationInstance) {
        this.targetClass = targetClass;
        this.annotationInstance = annotationInstance;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public AnnotationInstance getAnnotationInstance() {
        return annotationInstance;
    }
}
