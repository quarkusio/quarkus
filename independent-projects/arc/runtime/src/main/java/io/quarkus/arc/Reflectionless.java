package io.quarkus.arc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InvocationContext;

/**
 * Marks an interceptor (a class annotated {@code @Interceptor}) as <em>reflectionless</em>.
 * If all interceptors that apply to some method or constructor are reflectionless,
 * the {@link InvocationContext#getMethod()} and {@link InvocationContext#getConstructor()} methods
 * will always return {@code null}. That method will not be registered for reflection when
 * compiling to a native image.
 * <p>
 * Some information about the intercepted method or constructor can be obtained more cheaply
 * using {@link ArcInvocationContext#getMethodMetadata()}.
 * <p>
 * This annotation only makes sense for {@code @AroundInvoke} and {@code @AroundConstruct} interceptors.
 * It doesn't affect {@code @PostConstruct} and {@code @PreDestroy} lifecycle callbacks.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Reflectionless {
}
