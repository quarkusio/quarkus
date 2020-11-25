package io.quarkus.narayana.jta.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is a meta annotation that indicates that the child annotation defines additional transactional configuration.
 */
@Inherited
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface AdditionalTransactionConfiguration {
}
