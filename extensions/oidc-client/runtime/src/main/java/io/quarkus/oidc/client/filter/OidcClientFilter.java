package io.quarkus.oidc.client.filter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OidcClientFilter {

    /**
     * @return name of the OIDC client that should be used to acquire the tokens.
     */
    String value() default "";

}
