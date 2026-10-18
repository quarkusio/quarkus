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

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import io.quarkus.hibernate.orm.PersistenceUnit.List;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

/**
 * This annotation has multiple purposes:
 * <ul>
 * <li>It is a qualifier used to specify to which persistence unit the injected {@link EntityManagerFactory} or
 * {@link EntityManager} belongs. This allows for regular CDI bean injection of both interfaces.</li>
 * <li>It is used to mark packages as part of a given persistence unit.</li>
 * <li>It is used to mark classes (entities, mapped superclasses, embeddables) as part of a given persistence unit.</li>
 * </ul>
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

    @Target({ TYPE, PACKAGE })
    @Retention(RUNTIME)
    @Documented
    @interface List {

        PersistenceUnit[] value();
    }
}
