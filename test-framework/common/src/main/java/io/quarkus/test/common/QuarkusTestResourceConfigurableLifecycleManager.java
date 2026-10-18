package io.quarkus.test.common;

import java.lang.annotation.Annotation;

public interface QuarkusTestResourceConfigurableLifecycleManager<ConfigAnnotation extends Annotation>
        extends QuarkusTestResourceLifecycleManager {
    default void init(ConfigAnnotation annotation) {
    }
}
