package io.quarkus.agroal;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Qualifier used to specify which datasource will be injected.
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface DataSource {

    String value();

    public class DataSourceLiteral extends AnnotationLiteral<DataSource> implements DataSource {

        private String name;

        public DataSourceLiteral(String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return name;
        }
    }
}
