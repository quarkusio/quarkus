package io.quarkus.oidc;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to list Authentication Context Class Reference (ACR) values to enforce a required
 * authentication level for the endpoint classes and methods.
 * <a href="https://datatracker.ietf.org/doc/rfc9470/">OAuth 2.0 Step Up Authentication Challenge Protocol</a>
 * is initiated when the access token does not have sufficient authentication strength.
 */
@Target({ TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticationContext {

    /**
     * Required ACR ('acr') claim values.
     */
    String[] value();

    /**
     * Token age relative to the value of the 'auth_time' claim value.
     *
     * @see io.quarkus.runtime.configuration.DurationConverter#parseDuration(String) for supported duration values
     */
    String maxAge() default "";

}
