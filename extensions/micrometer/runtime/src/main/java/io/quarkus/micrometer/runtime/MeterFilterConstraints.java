package io.quarkus.micrometer.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD })
public @interface MeterFilterConstraints {
    MeterFilterConstraint[] value();
}
