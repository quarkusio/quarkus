package io.quarkus.grpc.runtime.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import io.grpc.BindableService;

/**
 * Qualifier used to indicate that the annotated {@link BindableService} should not be executed on the IO thread but on
 * a worker thread.
 *
 * Use this annotation if your service implementation need to use blocking APIs such as <em>traditional</em> database
 * access.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ TYPE, FIELD, METHOD })
public @interface Blocking {
}
