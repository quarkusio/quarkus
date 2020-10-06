package io.quarkus.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Annotation used to indicate that the annotated element should not be executed on the IO thread but on
 * a worker thread.
 *
 * This annotation is used to allow <em>blocking</em> processing in a reactive application.
 * For example, use this annotation if your reactive route or gRPC service need to use blocking APIs such
 * as <em>blocking</em> database access.
 *
 * This annotation is also a CDI Qualifier, must can also be used as a marker annotation.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ TYPE, FIELD, METHOD })
public @interface RunOnWorkerThread {
}
