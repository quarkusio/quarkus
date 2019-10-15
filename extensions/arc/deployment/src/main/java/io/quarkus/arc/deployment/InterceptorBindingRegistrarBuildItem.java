package io.quarkus.arc.deployment;

import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.builder.item.MultiBuildItem;

public final class InterceptorBindingRegistrarBuildItem extends MultiBuildItem {
    private final InterceptorBindingRegistrar registrar;

    public InterceptorBindingRegistrarBuildItem(InterceptorBindingRegistrar registrar) {
        this.registrar = registrar;
    }

    public InterceptorBindingRegistrar getInterceptorBindingRegistrar() {
        return registrar;
    }
}