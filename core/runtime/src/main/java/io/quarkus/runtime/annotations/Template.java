package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the given type is a template that can be used by the bytecode recorder.
 * <p>
 * Templates classes must be non final and have a public no-arg constructor.
 * <p>
 * At deployment time proxies of the templates can be injected into BuildStep methods that
 * have been annotated with {@code @Record}. Any invocations made on these proxies will be
 * recorded, and bytecode will be written out to be executed at runtime to make the same
 * sequence of invocations with the same parameters on the actual template objects.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Template {
}
