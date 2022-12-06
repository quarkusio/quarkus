package io.quarkus.qute.i18n;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Locale;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Marks a localized message bundle interface.
 *
 * @see MessageBundle
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD, PARAMETER })
public @interface Localized {

    /**
     * @return the locale tag string (IETF)
     * @see Locale#forLanguageTag(String)
     */
    String value();

    public static final class Literal extends AnnotationLiteral<Localized> implements Localized {

        public static Literal of(String value) {
            return new Literal(value);
        }

        private static final long serialVersionUID = 1L;

        private final String value;

        public Literal(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

    }

}
