package io.quarkus.it.metaannotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.test.junit.QuarkusTest;

/**
 * A composed {@code @QuarkusTest} meta-annotation defined in a separate, Jandex-indexed jar.
 * A test class annotated solely with this annotation must be registered as a CDI bean, just like a
 * class annotated directly with {@code @QuarkusTest}.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/56133">quarkus#56133</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@QuarkusTest
public @interface QuarkusTestFromJar {
}
