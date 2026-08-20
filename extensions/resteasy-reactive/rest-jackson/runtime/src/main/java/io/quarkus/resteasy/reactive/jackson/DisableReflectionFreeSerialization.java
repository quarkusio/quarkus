package io.quarkus.resteasy.reactive.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes the annotated class from the reflection-free Jackson optimization enabled by the
 * {@code quarkus.rest.jackson.optimization.enable-reflection-free-serializers} configuration property.
 * <p>
 * No serializer and no deserializer are generated at build time for the annotated class, so Jackson falls back to its
 * standard reflection-based serialization and deserialization for it. All other classes keep using their generated
 * serializers and deserializers, including the ones that reference the annotated class in one of their fields.
 * <p>
 * This is useful when a class relies on a Jackson feature that the generated serializers do not reproduce faithfully,
 * and lets an application opt that single class out instead of turning off the optimization altogether.
 * <p>
 * The annotation is not inherited: annotating a class does not exclude its subclasses.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DisableReflectionFreeSerialization {
}
