package io.quarkus.test.vertx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows test methods to run on a Vert.x thread instead of the main thread. This is useful for testing components that
 * must be run on the Event Loop (like Hibernate Reactive). In most cases users will have to use {@link UniAsserter} in
 * order to verify the results of the Async API under test.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface RunOnVertxContext {

    /**
     * Vert.x provides various types of contexts; for testing purposes the two which can be controlled via this
     * annotation are the global (root) context, and duplicate contexts, which have a narrower scope. Exact scope
     * boundaries are defined by the integration, so this might vary depending on how extensions set this up, but most
     * typically you will have a duplicated "local" scope for the current chain; for example when processing a RestEasy
     * Reactive request the duplicate context will span a single request and data stored in the duplicated scope should
     * not leak across different requests. In most cases you will want to run a test on such duplicate context as that's
     * representative of how most operations will be processed in Quarkus. Set to {@code false} to run on the global
     * (root) context instead.
     *
     * @return {@code true} by default.
     */
    boolean duplicateContext() default true;

    /**
     * If {@code true}, the test method is run on the Event Loop, otherwise it will be run on Vert.x blocking thread
     * pool
     */
    boolean runOnEventLoop() default true;
}
