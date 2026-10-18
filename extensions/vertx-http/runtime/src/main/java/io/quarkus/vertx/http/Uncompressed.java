package io.quarkus.vertx.http;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used to disable the compression of an HTTP response for a particular method.
 *
 * @see Compressed
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Uncompressed {

}
