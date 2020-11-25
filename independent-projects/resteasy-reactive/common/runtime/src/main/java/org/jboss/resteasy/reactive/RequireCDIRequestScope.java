package org.jboss.resteasy.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that control whether the CDI request scope be activated should be activated for a Resource
 * It can be placed on a Resource class in which case it affects all Resource methods, or an specific methods.
 * The absence of the annotation on a class is the same as placing {@code @RequireCDIRequestScope(true)} on it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface RequireCDIRequestScope {

    /**
     * true if this endpoint requires the CDI request scope be activated.
     */
    boolean value() default true;
}
