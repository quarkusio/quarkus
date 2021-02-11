package io.quarkus.resteasy.reactive.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.function.BiFunction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.smallrye.common.annotation.Experimental;

/**
 * Annotation that can be used on RESTEasy Reactive Resource method to allow users to configure Jackson serialization
 * for that method only, without affecting the global Jackson configuration.
 */
@Experimental(value = "Remains to be determined if this is the best possible API for users to configure per Resource Method Serialization")
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface CustomSerialization {

    /**
     * A {@code BiFunction} that converts the global {@code ObjectMapper} and type for which a custom {@code ObjectWriter} is
     * needed
     * (this type will be a generic type if the method returns such a generic type) and returns the instance of the custom
     * {@code ObjectWriter}.
     *
     * Quarkus will construct one instance of this {@code BiFunction} for each JAX-RS resource method that is annotated with
     * {@code CustomSerialization} and once an instance is created it will be cached for subsequent usage by that resource
     * method.
     *
     * The class MUST contain a no-args constructor and it is advisable that it contains no state that is updated outside
     * of its constructor.
     * Furthermore, the {@code ObjectMapper} should NEVER be changed any way as it is the global ObjectMapper that is
     * accessible to the entire Quarkus application.
     */
    Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>> value();
}
