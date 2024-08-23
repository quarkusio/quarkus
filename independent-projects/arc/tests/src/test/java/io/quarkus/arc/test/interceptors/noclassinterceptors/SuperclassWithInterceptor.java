package io.quarkus.arc.test.interceptors.noclassinterceptors;

import io.quarkus.arc.NoClassInterceptors;

@InheritedClassLevel
public class SuperclassWithInterceptor {
    public void inheritedClassLevel() {
    }

    @MethodLevel
    public void inheritedClassLevelAndMethodLevel() {
    }

    @MethodLevel
    @NoClassInterceptors
    public void inheritedMethodLevelOnly() {
    }

    @NoClassInterceptors
    public void inheritedNoInterceptors() {
    }
}
