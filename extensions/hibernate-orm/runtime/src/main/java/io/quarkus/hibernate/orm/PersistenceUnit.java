package io.quarkus.hibernate.orm;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Qualifier used to specify to which persistence unit the injected {@link EntityManagerFactory} or {@link EntityManager}
 * belongs.
 * <p>
 * This allows for regular CDI bean injection of both interfaces.
 */
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface PersistenceUnit {

    String value();

    public class PersistenceUnitLiteral extends AnnotationLiteral<PersistenceUnit> implements PersistenceUnit {

        private String name;

        public PersistenceUnitLiteral(String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return name;
        }
    }
}
