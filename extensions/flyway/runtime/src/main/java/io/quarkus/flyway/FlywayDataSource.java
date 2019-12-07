package io.quarkus.flyway;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Named;
import javax.inject.Qualifier;

/**
 * Qualifier used to specify which datasource will be used and therefore which flyway instance will be injected.<p<
 * 
 * Flyway instance can also be qualified by name using @{@link Named}.
 * The name is the datasource name followed by "_flyway".
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface FlywayDataSource {

    String value();

    /**
     * Supports inline instantiation of the {@link FlywayDataSource} qualifier.
     */
    public static final class Literal extends AnnotationLiteral<FlywayDataSource> implements FlywayDataSource {

        public static final Literal INSTANCE = of("");

        private static final long serialVersionUID = 1L;

        private final String value;

        public static Literal of(String value) {
            return new Literal(value);
        }

        @Override
        public String value() {
            return value;
        }

        private Literal(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "FlywayDataSourceLiteral [value=" + value + "]";
        }
    }
}