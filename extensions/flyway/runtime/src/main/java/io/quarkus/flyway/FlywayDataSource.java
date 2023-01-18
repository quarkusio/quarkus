package io.quarkus.flyway;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;

/**
 * Qualifier used to specify which datasource will be used and therefore which Flyway instance will be injected.
 * <p>
 * Flyway instances can also be qualified by name using @{@link Named}.
 * The name is the datasource name prefixed by "flyway_".
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
    public static final class FlywayDataSourceLiteral extends AnnotationLiteral<FlywayDataSource> implements FlywayDataSource {

        public static final FlywayDataSourceLiteral INSTANCE = of("");

        private static final long serialVersionUID = 1L;

        private final String value;

        public static FlywayDataSourceLiteral of(String value) {
            return new FlywayDataSourceLiteral(value);
        }

        @Override
        public String value() {
            return value;
        }

        private FlywayDataSourceLiteral(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "FlywayDataSourceLiteral [value=" + value + "]";
        }
    }
}
