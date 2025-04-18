package io.quarkus.oidc;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which can be used to enforce required authentication level for the endpoint classes and methods.
 * <a href="https://datatracker.ietf.org/doc/rfc9470/">OAuth 2.0 Step Up Authentication Challenge Protocol</a>
 * is initiated when the access token does not have sufficient authentication strength.
 */
@Target({ TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticationContext {

    /**
     * By default, max age is not checked.
     */
    long NO_MAX_AGE = -1;

    /**
     * Required `acr` (Authentication Context Class Reference) claim values.
     */
    String[] value() default {};

    /**
     * If set, the required maximum token age is determined by adding the value of this attribute to the 'auth_time' claim.
     * The maximum age is set in seconds.
     */
    long maxAge() default NO_MAX_AGE;
}
