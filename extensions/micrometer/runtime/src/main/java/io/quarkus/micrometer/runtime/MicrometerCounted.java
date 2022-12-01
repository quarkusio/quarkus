package io.quarkus.micrometer.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

import io.micrometer.core.annotation.Counted;

@Inherited
@InterceptorBinding
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MicrometerCounted {

    /**
     * @see Counted#value()
     */
    @Nonbinding
    String value() default "method.counted";

    /**
     * @see Counted#recordFailuresOnly()
     */
    @Nonbinding
    boolean recordFailuresOnly() default false;

    /**
     * @see Counted#extraTags()
     */
    @Nonbinding
    String[] extraTags() default {};

    /**
     * @see Counted#description()
     */
    @Nonbinding
    String description() default "";
}
