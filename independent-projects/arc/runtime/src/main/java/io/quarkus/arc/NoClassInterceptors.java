package io.quarkus.arc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If a business method or constructor is annotated {@code @NoClassInterceptors}, then interceptors
 * whose interceptor binding annotations are present on a class are ignored for this method or
 * constructor. In other words, the only interceptors that apply to such method/constructor are
 * interceptors declared directly on the method/constructor.
 * <p>
 * This annotation only applies to business method interceptors ({@code @AroundInvoke}) and
 * constructor lifecycle callback interceptors ({@code @AroundConstruct}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
public @interface NoClassInterceptors {
}
