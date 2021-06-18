package io.quarkus.mongodb.panache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define a field's path for the projection document when using a projection DTO.
 * It supports the "dot" notation to reference fields in sub documents.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface ProjectedFieldName {
    String value();
}
