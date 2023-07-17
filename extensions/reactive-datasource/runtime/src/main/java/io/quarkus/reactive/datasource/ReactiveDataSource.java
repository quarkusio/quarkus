package io.quarkus.reactive.datasource;

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
 * Qualifier used to specify which reactive datasource will be injected.
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface ReactiveDataSource {

    String value();

    public class ReactiveDataSourceLiteral extends AnnotationLiteral<ReactiveDataSource> implements ReactiveDataSource {

        private String name;

        public ReactiveDataSourceLiteral(String name) {
            this.name = name;
        }

        @Override
        public String value() {
            return name;
        }
    }
}
