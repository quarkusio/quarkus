package io.quarkus.hibernate.orm;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.inject.Qualifier;
import jakarta.persistence.EntityManagerFactory;

/**
 * This is used to qualify the {@link EntityManagerFactory} that should be used for Hibernate Reactive.
 */
@Target({ TYPE, FIELD, METHOD, PARAMETER, PACKAGE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface ReactivePersistenceUnit {
}
