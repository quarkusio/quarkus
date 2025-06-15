package io.quarkus.vertx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;

/**
 * Indicates that the annotated method should be invoked on a safe duplicated context. This interceptor binding is a
 * declarative alternative to the {@link VertxContextSafetyToggle}.
 * <p>
 * <strong>Important:</strong> You must only use this annotation if the annotated method does not access the context
 * concurrently.
 * <p>
 * If the method is not run on a duplicated context, the interceptor fails. If the method is invoked on an unmarked
 * duplicated context, the context is marked as `safe` and the method is called. If the method is invoked on a `safe`
 * duplicated context, the method is called. If the method is invoked on an `unsafe` duplicated context, the interceptor
 * fails, except if {@link #force()} is set to {@code true}. In this case, the duplicated context is marked as `safe`
 * and the method is called.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
@Inherited
public @interface SafeVertxContext {

    /**
     * @return whether the current safety flag of the current context must be overridden.
     */
    @Nonbinding
    boolean force() default false;
}
