package io.quarkus.it.metaannotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.test.junit.QuarkusTestExtension;

/**
 * A composed meta-annotation defined in a separate, Jandex-indexed jar that marks a Quarkus test via
 * {@code @ExtendWith(QuarkusTestExtension.class)} rather than {@code @QuarkusTest}. A test class
 * annotated solely with this must still be registered as a CDI bean.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/56133">quarkus#56133</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(QuarkusTestExtension.class)
public @interface QuarkusTestExtensionFromJar {
}
