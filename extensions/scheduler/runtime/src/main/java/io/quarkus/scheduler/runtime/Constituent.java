package io.quarkus.scheduler.runtime;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;

import io.quarkus.scheduler.Scheduler;

/**
 * This qualifier is used to mark a constituent of a composite {@link Scheduler}, i.e. to distinguish various scheduler
 * implementations.
 */
@Qualifier
@Documented
@Retention(RUNTIME)
@Target({ TYPE, PARAMETER, FIELD })
public @interface Constituent {

}
