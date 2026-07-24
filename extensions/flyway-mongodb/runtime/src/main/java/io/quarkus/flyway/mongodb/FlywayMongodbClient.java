package io.quarkus.flyway.mongodb;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.Serial;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Qualifier used to identify the Flyway/FlywayMongodbContainer bean for a specific MongoDB client.
 * <p>
 * Usage: {@code @Inject @FlywayMongodbClient("analytics") Flyway flyway}
 */
@Documented
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD, PARAMETER })
public @interface FlywayMongodbClient {

    String value();

    /**
     * Supports inline instantiation of the {@link FlywayMongodbClient} qualifier.
     */
    final class Literal extends AnnotationLiteral<FlywayMongodbClient> implements FlywayMongodbClient {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String value;

        public static Literal of(String value) {
            return new Literal(value);
        }

        private Literal(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return "FlywayMongodbClient.Literal [value=" + value + "]";
        }
    }
}
