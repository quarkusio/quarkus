package io.quarkus.resteasy.reactive.jackson.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * Qualifier Annotation that specializes production, customization, and injection of the REST Client ObjectMapper. If an ObjectMapper
 * is produced with this qualifier, it is used by the quarkus-rest-client-reactive extension for serialization and
 * deserialization. Otherwise, quarkus-rest-client-reactive uses the unqualified `ObjectMapper` instance.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE })
public @interface RestClientObjectMapper {
    final class Literal extends AnnotationLiteral<RestClientObjectMapper> implements RestClientObjectMapper {
        public static final RestClientObjectMapper.Literal INSTANCE = new Literal();
        private static final long serialVersionUID = 1L;
    }
}
