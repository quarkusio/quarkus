package io.quarkus.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The default main class of a Quarkus application.
 *
 * There are two different ways this annotation can be used. The first is to place it
 * on a class with a Java main method. This main method will be the default entry point of
 * the application. Note that Quarkus will not be started when this method is called,
 * this method must launch Quarkus with the {@link io.quarkus.runtime.Quarkus#run(Class, String...)}
 * method.
 *
 * Alternatively this annotation can be placed on an {@link io.quarkus.runtime.QuarkusApplication}
 * implementation. In this case a main method is automatically generated that will automatically
 * call {@link io.quarkus.runtime.Quarkus#run(Class, String...)} with the provided application.
 *
 * Note that this can be overridden by the presence of the {@literal quarkus.package.main-class}
 * configuration key. If this configuration key is used it can either specify the fully qualified
 * name of a class with a Java main method, the fully qualified name of a {@link io.quarkus.runtime.QuarkusApplication},
 * or the name as given by {@link #name()}.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface QuarkusMain {

    /**
     * The name of this main method, which must be unique.
     */
    String name() default "";
}
