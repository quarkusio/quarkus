package io.quarkus.arc.test.interceptors.noclassinterceptors;

import io.quarkus.arc.NoClassInterceptors;

public interface SuperSuperinterface {
    @NoClassInterceptors
    default void inheritedDefaultMethod() {
    }
}
