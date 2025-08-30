package io.quarkus.liquibase.mongodb.runtime;

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
 * Qualifier used to specify which client will be used and therefore which Liquibase MongoDB instance will be injected.
 * <p>
 * Liquibase instances can also be qualified by name using @{@link Named}.
 * The name is the client name prefixed by "liquibase_mongodb_".
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface LiquibaseMongodbClient {

    String value();

    /**
     * Supports inline instantiation of the {@link LiquibaseMongodbClient} qualifier.
     */
    final class LiquibaseMongodbClientLiteral extends AnnotationLiteral<LiquibaseMongodbClient>
            implements LiquibaseMongodbClient {

        private static final long serialVersionUID = 1L;

        private final String value;

        public static LiquibaseMongodbClientLiteral of(String value) {
            return new LiquibaseMongodbClientLiteral(value);
        }

        @Override
        public String value() {
            return value;
        }

        private LiquibaseMongodbClientLiteral(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "LiquibaseMongodbClientLiteral [value=" + value + "]";
        }
    }
}
