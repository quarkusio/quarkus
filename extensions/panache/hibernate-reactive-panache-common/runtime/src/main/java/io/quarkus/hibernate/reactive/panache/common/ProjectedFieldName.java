package io.quarkus.hibernate.reactive.panache.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a field's path for the SELECT statement when using a projection DTO.
 * It supports the "dot" notation for fields in referenced entities:
 * the name is composed of the property name for the relationship, followed by a dot ("."), followed by the name of the field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ProjectedFieldName {
    String value();
}
