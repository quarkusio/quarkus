package io.quarkus.liquibase;

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
 * Qualifier used to specify which datasource will be used and therefore which Liquibase instance will be injected.
 * <p>
 * Liquibase instances can also be qualified by name using @{@link Named}.
 * The name is the datasource name prefixed by "liquibase_".
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface LiquibaseDataSource {

    String value();

    /**
     * Supports inline instantiation of the {@link LiquibaseDataSource} qualifier.
     */
    final class LiquibaseDataSourceLiteral extends AnnotationLiteral<LiquibaseDataSource>
            implements LiquibaseDataSource {

        public static final LiquibaseDataSourceLiteral INSTANCE = of("");

        private static final long serialVersionUID = 1L;

        private final String value;

        public static LiquibaseDataSourceLiteral of(String value) {
            return new LiquibaseDataSourceLiteral(value);
        }

        @Override
        public String value() {
            return value;
        }

        private LiquibaseDataSourceLiteral(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "LiquibaseDataSourceLiteral [value=" + value + "]";
        }
    }
}
