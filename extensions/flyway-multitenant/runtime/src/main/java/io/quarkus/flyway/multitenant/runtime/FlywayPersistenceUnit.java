package io.quarkus.flyway.multitenant.runtime;

import static io.quarkus.flyway.runtime.FlywayCreator.TENANT_ID_DEFAULT;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

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
public @interface FlywayPersistenceUnit {

    String value() default PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

    @Nonbinding
    String tenantId() default TENANT_ID_DEFAULT;

    /**
     * Supports inline instantiation of the {@link FlywayPersistenceUnit} qualifier.
     */
    public static final class FlywayPersistenceUnitLiteral extends AnnotationLiteral<FlywayPersistenceUnit>
            implements FlywayPersistenceUnit {

        public static final FlywayPersistenceUnitLiteral INSTANCE = of("");

        private static final long serialVersionUID = 1L;

        private final String value;

        private final String tenanId;

        public static FlywayPersistenceUnitLiteral of(String value) {
            return of(value, TENANT_ID_DEFAULT);
        }

        public static FlywayPersistenceUnitLiteral of(String value, String tenantId) {
            return new FlywayPersistenceUnitLiteral(value, tenantId);
        }

        public static FlywayPersistenceUnitLiteral ofDefault(String tenantId) {
            return new FlywayPersistenceUnitLiteral(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, tenantId);
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String tenantId() {
            return tenanId;
        }

        private FlywayPersistenceUnitLiteral(String value, String tenantId) {
            this.value = value;
            this.tenanId = tenantId;
        }

        @Override
        public String toString() {
            return "FlywayDataSourceLiteral [value=" + value + "]";
        }
    }
}
