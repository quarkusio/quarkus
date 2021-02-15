package io.quarkus.security.jpa;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Indicates that this field or property should be used as a source of password for security. Only
 * supports the {@link String} type.
 * </p>
 * <p>
 * Defaults to considering the password as hashed with bcrypt in the Modular Crypt Format.
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Password {
    /**
     * Sets the password storage type. defaults to {@link PasswordType#MCF}.
     */
    PasswordType value() default PasswordType.MCF;

    /**
     * Sets a custom password provider when the type is {@link PasswordType#CUSTOM}
     */
    Class<? extends PasswordProvider> provider() default PasswordProvider.class;

}
