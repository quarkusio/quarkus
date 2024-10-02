package io.quarkus.runtime.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.config.ConfigMapping;

/**
 * Specifies the file name where {@code quarkus-extension-processor} will output the documentation in AsciiDoc format.
 * If not specified, the effective file name is derived either from the class name or {@link ConfigMapping#prefix()}.
 */
@Documented
@Retention(RUNTIME)
@Target({ ElementType.TYPE })
public @interface ConfigDocFilename {

    String value();
}
