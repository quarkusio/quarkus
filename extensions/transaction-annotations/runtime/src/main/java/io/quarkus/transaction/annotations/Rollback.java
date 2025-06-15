package io.quarkus.transaction.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If you annotate your exception with this annotation, the transactional interceptor will use the exception's
 * instructions to force a rollback or not.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Rollback {
    /**
     * Specify whether the annotated exception should cause a rollback or not. Defaults to true.
     *
     * @return true if the annotated exception should cause a rollback or not.
     */
    boolean value() default true;
}
