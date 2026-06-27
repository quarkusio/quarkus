package io.quarkus.hibernate.orm.panache.common;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Test-only stand-in for the real annotation in {@code quarkus-hibernate-orm-panache-common}.
 * Keeps {@code quarkus-panache-hibernate-common} free of a test dependency cycle on that module.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ProjectedConstructor {
}
