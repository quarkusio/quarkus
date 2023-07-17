package io.quarkus.oidc.token.propagation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When this annotation is added to a MicroProfile REST Client interface, the {@link JsonWebTokenRequestFilter} will be added to
 * the request pipeline.
 * The end result is that the request propagates the JWT token present in the current active request or the token acquired from
 * the Authorization Code Flow,
 * as the HTTP {@code Authorization} header's {@code Bearer} scheme value.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonWebToken {
}
