package io.quarkus.vertx.http;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used to enable the compression of an HTTP response for a particular method.
 *
 * @see Uncompressed
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Compressed {

}
