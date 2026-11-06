package io.quarkus.opentelemetry.runtime.tracing.cdi;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Internal interceptor binding identifying the method a {@code @WithSpan} span is about when the intercepted method
 * is not the one the user wrote, e.g. the generated {@code $$CDIWrapper} of a REST client interface.
 * <p>
 * It is added at build time by the OpenTelemetry extension. When present, {@link WithSpanInterceptor} derives the
 * default span name and the {@code code.function.name} attribute from it.
 * <p>
 * The annotation is package-private on purpose: it is only ever added by the build-time annotation transformation of
 * the OpenTelemetry extension and must not be used by applications.
 */
@Target(METHOD)
@Retention(RUNTIME)
@interface SpanMethod {

    /**
     * @return the binary name of the class declaring the method, e.g. {@code org.acme.Outer$Client}
     */
    String declaringClass();

    /**
     * @return the name of the method
     */
    String method();
}
