package io.quarkus.liquibase.mongodb.runtime;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;

/**
 * Qualifier used to specify which datasource will be used and therefore which Liquibase MongoDB instance will be injected.
 * <p>
 * Liquibase instances can also be qualified by name using @{@link Named}.
 * The name is the datasource name prefixed by "liquibase_mongodb_".
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface LiquibaseMongodbDataSource {

    String value();

    /**
     * Supports inline instantiation of the {@link LiquibaseMongodbDataSource} qualifier.
     */
    final class LiquibaseMongodbDataSourceLiteral extends AnnotationLiteral<LiquibaseMongodbDataSource>
            implements LiquibaseMongodbDataSource {

        public static final LiquibaseMongodbDataSourceLiteral INSTANCE = of("");

        private static final long serialVersionUID = 1L;

        private final String value;

        public static LiquibaseMongodbDataSourceLiteral of(String value) {
            return new LiquibaseMongodbDataSourceLiteral(value);
        }

        @Override
        public String value() {
            return value;
        }

        private LiquibaseMongodbDataSourceLiteral(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "LiquibaseMongodbDataSourceLiteral [value=" + value + "]";
        }
    }
}
