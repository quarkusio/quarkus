package io.quarkus.oidc.token.propagation.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When this annotation is added to a MicroProfile REST Client interface, the {@link AccessTokenRequestFilter} will be added to
 * the request pipeline.
 * The end result is that the request propagates the Bearer token present in the current active request or the token acquired
 * from the Authorization Code Flow,
 * as the HTTP {@code Authorization} header's {@code Bearer} scheme value.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AccessToken {

    /**
     * Selects name of the configured OidcClient and activates token exchange for the annotated REST client.
     * Please note that the default OidcClient's name is `Default`. You do not have to enable this attribute
     * if you use the default OidcClient and already have either 'quarkus.resteasy-client-oidc-token-propagation.exchange-token'
     * or 'quarkus.rest-client-oidc-token-propagation.exchange-token' property set to 'true'
     */
    String exchangeTokenClient() default "";

}
