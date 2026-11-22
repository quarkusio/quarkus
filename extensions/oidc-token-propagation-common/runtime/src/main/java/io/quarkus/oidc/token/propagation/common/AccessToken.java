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
 * <p>
 * This annotation may also be placed on individual methods of the REST Client interface.
 * When applied to a method, the {@link AccessTokenRequestFilter} will be registered only for that method.
 * <p>
 * When this annotation is added to a SmallRye GraphQL typesafe client interface and the
 * {@code quarkus-oidc-token-propagation-graphql} extension is present, the GraphQL client propagates the token the same way.
 * This annotation is not supported on individual methods of the GraphQL client interface.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AccessToken {

    /**
     * Selects name of the configured OidcClient and activates token exchange for the annotated REST or GraphQL client.
     * Please note that the default OidcClient's name is `Default`. You do not have to enable this attribute
     * if you use the default OidcClient and already have either 'quarkus.resteasy-client-oidc-token-propagation.exchange-token'
     * or 'quarkus.rest-client-oidc-token-propagation.exchange-token' property set to 'true'.
     * These properties only apply to REST clients, a GraphQL client only exchanges the token when this attribute is set.
     */
    String exchangeTokenClient() default "";

}
