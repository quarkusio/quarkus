package io.quarkus.mailer.runtime;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RUNTIME)
@Documented
public @interface MailTemplateMailerName {

    /**
     * The Mailer name.
     */
    String value() default "";
}
