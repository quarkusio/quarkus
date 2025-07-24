package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Defines where the JsonRPC method should be available.
 */
@Retention(RUNTIME)
@Target({ METHOD })
@Documented
public @interface JsonRpcUsage {
    Usage[] value();
}