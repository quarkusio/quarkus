package io.quarkus.test.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Used to signal that a test class or method should be disabled if the version of GraalVM used to build the native binary
 * under test was newer than the supplied version.
 *
 * This annotation should only be used on a test classes annotated with {@link QuarkusIntegrationTest}.
 * If it is used on other test classes, it will have no effect.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisableIfBuiltWithGraalVMNewerThanCondition.class)
public @interface DisableIfBuiltWithGraalVMNewerThan {
    GraalVMVersion value();
}
