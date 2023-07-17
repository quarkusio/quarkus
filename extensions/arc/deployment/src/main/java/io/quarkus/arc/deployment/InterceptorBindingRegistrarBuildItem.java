package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * Makes it possible to register annotations that should be considered interceptor bindings but are not annotated with
 * {@code jakarta.interceptor.InterceptorBinding}.
 */
public final class InterceptorBindingRegistrarBuildItem extends MultiBuildItem {

    private final InterceptorBindingRegistrar registrar;

    public InterceptorBindingRegistrarBuildItem(InterceptorBindingRegistrar registrar) {
        this.registrar = registrar;
    }

    public InterceptorBindingRegistrar getInterceptorBindingRegistrar() {
        return registrar;
    }
}
