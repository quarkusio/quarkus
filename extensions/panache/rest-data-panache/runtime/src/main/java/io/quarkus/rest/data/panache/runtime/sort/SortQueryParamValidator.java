package io.quarkus.rest.data.panache.runtime.sort;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface SortQueryParamValidator {

    /**
     * The list of allowed field names for sorting.
     * If empty, only format validation is performed (legacy behavior).
     */
    String[] allowedFields() default {};

    /**
     * Fields that require security role checks before sorting.
     * Format: "fieldName:role1,role2,role3" where any of the roles grants access.
     * If a field is listed here and SecurityIdentity is not available or the user
     * lacks the required roles, sorting by that field will be rejected.
     */
    String[] secureFields() default {};
}
