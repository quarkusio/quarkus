package org.jboss.resteasy.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows setting the {@code Cache-Control} header automatically.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Cache {

    int maxAge() default -1;

    int sMaxAge() default -1;

    boolean noStore() default false;

    boolean noTransform() default false;

    boolean mustRevalidate() default false;

    boolean proxyRevalidate() default false;

    boolean isPrivate() default false;

    boolean noCache() default false;
}
