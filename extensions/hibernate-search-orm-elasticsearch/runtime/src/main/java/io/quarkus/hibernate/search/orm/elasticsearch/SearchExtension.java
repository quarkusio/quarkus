package io.quarkus.hibernate.search.orm.elasticsearch;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import io.quarkus.hibernate.orm.PersistenceUnit;

/**
 * CDI qualifier for beans representing an "extension" of Hibernate Search
 * in a given persistence unit of Hibernate ORM,
 * i.e. beans injected into Hibernate Search as part of its configuration.
 * <p>
 * See the reference documentation for information about extensions that supports this annotation.
 */
@Target({ TYPE, FIELD, METHOD, PARAMETER })
@Retention(RUNTIME)
@Documented
@Qualifier
@Repeatable(SearchExtension.List.class)
public @interface SearchExtension {

    /**
     * @return The name of the persistence unit that the qualified bean should be assigned to.
     */
    String persistenceUnit() default PersistenceUnit.DEFAULT;

    /**
     * @return The name of the Hibernate Search backend that the qualified bean should be assigned to.
     */
    String backend() default "";

    /**
     * @return The name of the Hibernate Search index that the qualified bean should be assigned to.
     */
    String index() default "";

    class Literal
            extends AnnotationLiteral<SearchExtension>
            implements SearchExtension {

        private final String persistenceUnit;
        private final String backend;
        private final String index;

        public Literal(String persistenceUnit, String backend, String index) {
            this.persistenceUnit = persistenceUnit;
            this.backend = backend;
            this.index = index;
        }

        @Override
        public String persistenceUnit() {
            return persistenceUnit;
        }

        @Override
        public String backend() {
            return backend;
        }

        @Override
        public String index() {
            return index;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other instanceof SearchExtension that) {
                return this.persistenceUnit.equals(that.persistenceUnit())
                        && this.backend.equals(that.backend())
                        && this.index.equals(that.index());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int result = 0;
            result += 127 * "persistenceUnit".hashCode() ^ this.persistenceUnit.hashCode();
            result += 127 * "backend".hashCode() ^ this.backend.hashCode();
            result += 127 * "index".hashCode() ^ this.index.hashCode();
            return result;
        }
    }

    @Target({ TYPE, FIELD, METHOD, PARAMETER })
    @Retention(RUNTIME)
    @Documented
    @interface List {
        SearchExtension[] value();
    }
}
