package io.quarkus.arc.test.interceptors.noclassinterceptors;

import io.quarkus.arc.NoClassInterceptors;

public interface Superinterface extends SuperSuperinterface {
    void inheritedClassLevelAndMethodLevel();

    void inheritedMethodLevelOnly();

    void inheritedNoInterceptors();

    @MethodLevel // this should be ignored, only class-level interceptors apply to default methods
    @NoClassInterceptors // and this makes sure that even class-level interceptors do not apply
    default void defaultMethod() {
    }

    // `SuperSuperinterface.inheritedDefaultMethod` has `@NoClassInterceptors`, but this override
    // does not, so class-level interceptors _should_ apply
    @Override
    default void inheritedDefaultMethod() {
    }
}
