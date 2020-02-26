package io.quarkus.agroal.runtime;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * Defines which database the JDBC driver is compatible with.
 */
@Target({ METHOD, FIELD, PARAMETER, TYPE })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface JdbcDriver {

    String name();

    class JdbcDriverLiteral extends AnnotationLiteral<io.quarkus.agroal.runtime.JdbcDriver>
            implements io.quarkus.agroal.runtime.JdbcDriver {

        private String name;

        public JdbcDriverLiteral(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }

}
