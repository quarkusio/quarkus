package io.quarkus.test.junit.vertx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows test methods to run on a Vert.x thread instead of the main thread.
 * This is useful for testing components that must be run on the Event Loop (like Hibernate Reactive)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface RunOnVertxContext {
}
