package io.quarkus.test.common;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Uses to define arguments of for {@code QuarkusTestResource}
 *
 * see {@link QuarkusTestResource#initArgs()}
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResourceArg {

    String name();

    String value();
}
