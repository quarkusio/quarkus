package io.quarkus.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates that this test should enable the Native build for the {@link @QuarkusProdModeTest} extension.
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableNativeBuildOnQuarkusProdMode {
}
