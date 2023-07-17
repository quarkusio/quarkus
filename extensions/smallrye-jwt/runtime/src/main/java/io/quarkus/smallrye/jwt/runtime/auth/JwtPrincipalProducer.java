package io.quarkus.smallrye.jwt.runtime.auth;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.auth.cdi.NullJsonWebToken;

@Priority(1)
@Alternative
@RequestScoped
public class JwtPrincipalProducer {

    @Inject
    SecurityIdentity identity;

    /**
     * The producer method for the current JsonWebToken
     *
     * @return JsonWebToken
     */
    @Produces
    @RequestScoped
    JsonWebToken currentJWTPrincipalOrNull() {
        if (identity.isAnonymous()) {
            return new NullJsonWebToken();
        }
        if (identity.getPrincipal() instanceof JsonWebToken) {
            return (JsonWebToken) identity.getPrincipal();
        }
        throw new IllegalStateException("Current principal " + identity.getPrincipal() + " is not a JSON web token");
    }
}
