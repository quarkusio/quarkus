package io.quarkus.opentelemetry.runtime.tracing.cdi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks that an execution of this method or constructor should result in a new trace by using
 * {@link io.opentelemetry.context.Context#root()} as parent when using
 * {@link io.opentelemetry.extension.annotations.WithSpan} annotation.
 */
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface WithRoot {

    /**
     * When the new trace is started we normally loose all connection to the trace which was active before we switched
     * to root. When we set link to <code>true</code>, the new trace will add a link to the currently active Span.
     * Defaults to <code>false</code>.
     */
    boolean link() default false;
}
