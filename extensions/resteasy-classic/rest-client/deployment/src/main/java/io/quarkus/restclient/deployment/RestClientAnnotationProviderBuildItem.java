package io.quarkus.restclient.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to mark a custom annotation and its associated JAX-RS client provider
 */
public final class RestClientAnnotationProviderBuildItem extends MultiBuildItem {

    private final DotName annotationName;
    private final Class<?> providerClass;

    public RestClientAnnotationProviderBuildItem(DotName annotationName, Class<?> providerClass) {
        this.annotationName = annotationName;
        this.providerClass = providerClass;
    }

    public RestClientAnnotationProviderBuildItem(String annotationName, Class<?> providerClass) {
        this.annotationName = DotName.createSimple(annotationName);
        this.providerClass = providerClass;
    }

    public DotName getAnnotationName() {
        return annotationName;
    }

    public Class<?> getProviderClass() {
        return providerClass;
    }

}
