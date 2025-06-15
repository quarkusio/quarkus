package io.quarkus.mailer;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Marker annotation to select the Mailer. For example, if the Mailer is configured like so in
 * {@code application.properties}:
 *
 * <pre>
 * quarkus.mailer.client1.host = smtp.example.com
 * </pre>
 *
 * Then to inject the proper {@code Mailer}, you would need to use {@code MailerName} like indicated below:
 *
 * <pre>
 *     &#64Inject
 *     &#64MailerName("client1")
 *     Mailer mailer;
 * </pre>
 */
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface MailerName {

    /**
     * The Mailer name.
     */
    String value() default "";

    class Literal extends AnnotationLiteral<MailerName> implements MailerName {

        public static Literal of(String value) {
            return new Literal(value);
        }

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
