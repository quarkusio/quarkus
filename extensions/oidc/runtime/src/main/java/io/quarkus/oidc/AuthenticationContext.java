package io.quarkus.oidc;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which can be used to enforce required authentication level for the endpoint classes and methods.
 * If presented Bearer access token offer insufficient authentication strength, Quarkus will challenge the authentication
 * requirements according to the <a href="https://datatracker.ietf.org/doc/rfc9470/">OAuth 2.0 Step Up Authentication
 * Challenge Protocol</a>.
 */
@Target({ TYPE, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticationContext {

    /**
     * Required `acr` (Authentication Context Class Reference) claim values.
     */
    String[] value() default {};

}
