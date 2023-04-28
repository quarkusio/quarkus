package io.quarkus.runtime.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * A qualifier for Initialization tasks that are meant to be executed before the actual appliction startup.
 * The qualifier can be added either on {@link Runnable} types or on methods. The method (run in the {@link Runnable}) case
 * Will be called during the application initalization phase.
 *
 * Each initialization task is also exposed as a Kubernetes Job that needs to succeed before the application can be started.
 * This is taken care by the `quarkus-kubernetes` extension, that will generate the Job and an init container that will wait
 * for the job to succeed.
 *
 * The quarlifier is used by quarkus extensions (e.g. flyway, liquibase, etc) but is also meant to be used by users when they
 * need to perform a custom initialization task.
 **/
@Qualifier
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface PreStart {

    /*
     * Task name
     *
     * @return the name of the initialization task.
     */
    String value() default "";

    public static final class Literal extends AnnotationLiteral<PreStart> implements PreStart, Qualifier {

        public static Literal forName(String name) {
            return new Literal(name);
        }

        private final String name;

        Literal(String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return name;
        }
    }
}
