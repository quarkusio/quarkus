package io.quarkus.rest.client.reactive.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to easily define a custom object mapper for the specific REST Client on which it's used.
 *
 * The annotation MUST be placed on a method of the REST Client interface that meets the following criteria:
 * <ul>
 * <li>Is a {@code static} method</li>
 * </ul>
 *
 * An example method could look like the following:
 *
 * <pre>
 * {@code
 * @ClientObjectMapper
 * static ObjectMapper objectMapper() {
 *     return new ObjectMapper();
 * }
 *
 * }
 * </pre>
 *
 * Moreover, we can inject the default ObjectMapper instance to create a copy of it by doing:
 *
 * <pre>
 * {@code
 * &#64;ClientObjectMapper
 * static ObjectMapper objectMapper(ObjectMapper defaultObjectMapper) {
 *     return defaultObjectMapper.copy() <3>
 *             .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
 *             .disable(DeserializationFeature.UNWRAP_ROOT_VALUE);
 * }
 *
 * }
 * </pre>
 *
 * Remember that the default object mapper instance should NEVER be modified, but instead always use copy if they pan to
 * inherit the default settings.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ClientObjectMapper {
}
