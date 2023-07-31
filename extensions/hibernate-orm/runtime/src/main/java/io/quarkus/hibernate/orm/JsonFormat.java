package io.quarkus.hibernate.orm;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import org.hibernate.type.format.FormatMapper;

/**
 * CDI qualifier for beans implementing a {@link FormatMapper}.
 * <p>
 * This mapper will be used by Hibernate ORM for serialization and deserialization of JSON properties.
 * <p>
 * <strong>Must</strong> be used in a combination with a {@link PersistenceUnitExtension} qualifier to define the persistence
 * unit the mapper should be associated with.
 */
@Target({ TYPE, FIELD, METHOD, PARAMETER })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface JsonFormat {
    class Literal extends AnnotationLiteral<JsonFormat> implements JsonFormat {
        public static JsonFormat INSTANCE = new Literal();

        private Literal() {
        }
    }
}
