package io.quarkus.hibernate.orm;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * CDI qualifier for beans representing an "extension" of a persistence unit,
 * i.e. beans injected into the persistence unit as part of its configuration.
 * <p>
 * See the reference documentation for information about extensions that supports this annotation.
 */
@Target({ TYPE, FIELD, METHOD, PARAMETER })
@Retention(RUNTIME)
@Documented
@Qualifier
@Repeatable(PersistenceUnitExtension.List.class)
public @interface PersistenceUnitExtension {

    String value() default PersistenceUnit.DEFAULT;

    class Literal
            extends AnnotationLiteral<PersistenceUnitExtension>
            implements PersistenceUnitExtension {

        private final String name;

        public Literal(String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return name;
        }
    }

    @Target({ TYPE, FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    @Documented
    @interface List {
        PersistenceUnitExtension[] value();
    }
}
