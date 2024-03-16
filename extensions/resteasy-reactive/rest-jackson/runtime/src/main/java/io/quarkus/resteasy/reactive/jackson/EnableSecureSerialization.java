package io.quarkus.resteasy.reactive.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * If placed on a method, then all {@link SecureField} annotations of the response type will be taken into account,
 * even if the class is annotated with {@link DisableSecureSerialization}.
 */
@Experimental(value = "Remains to be determined if this is the best possible API for users to configure security of serialized fields")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface EnableSecureSerialization {
}
