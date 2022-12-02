package io.quarkus.hibernate.orm;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import io.quarkus.hibernate.orm.PersistenceUnit.List;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

/**
 * This annotation has two different purposes.
 * It is a qualifier used to specify to which persistence unit the injected {@link EntityManagerFactory} or
 * {@link EntityManager} belongs.
 * <p>
 * This allows for regular CDI bean injection of both interfaces.
 * <p>
 * It is also used to mark packages as part of a given persistence unit.
 */
@Target({ TYPE, FIELD, METHOD, PARAMETER, PACKAGE })
@Retention(RUNTIME)
@Documented
@Qualifier
@Repeatable(List.class)
public @interface PersistenceUnit {

    String DEFAULT = PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

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

    @Target(PACKAGE)
    @Retention(RUNTIME)
    @Documented
    @interface List {

        PersistenceUnit[] value();
    }
}
