package io.quarkus.oidc;

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

/**
 * Qualifier used to specify which named tenant is associated with one or more OIDC feature.
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface TenantFeature {
    /**
     * Identifies an OIDC tenant to which a given feature applies.
     */
    String value();

    /**
     * Supports inline instantiation of the {@link TenantFeature} qualifier.
     */
    final class TenantFeatureLiteral extends AnnotationLiteral<TenantFeature> implements TenantFeature {

        private final String value;

        private TenantFeatureLiteral(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return "TenantFeatureLiteral [value=" + value + "]";
        }

        public static TenantFeature of(String value) {
            return new TenantFeatureLiteral(value);
        }
    }
}
