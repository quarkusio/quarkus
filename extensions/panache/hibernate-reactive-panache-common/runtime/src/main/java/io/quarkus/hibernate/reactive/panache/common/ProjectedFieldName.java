package io.quarkus.hibernate.reactive.panache.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a field's path or HQL expression for the SELECT statement when using a projection DTO.
 * <p>
 * It supports:
 * <ul>
 * <li>Simple field names</li>
 * <li>Dot notation for fields in referenced entities (e.g., {@code "owner.name"})</li>
 * <li>Any valid HQL expression, including aggregate functions (e.g., {@code "SUM(amount)"}, {@code "COUNT(id)"})</li>
 * </ul>
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER, ElementType.FIELD })
public @interface ProjectedFieldName {
    String value();
}
