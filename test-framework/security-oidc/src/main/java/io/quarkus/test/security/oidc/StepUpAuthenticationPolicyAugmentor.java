package io.quarkus.test.security.oidc;

import java.util.Map;
import java.util.function.Consumer;

import jakarta.inject.Singleton;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.TokenVerificationResult;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.security.TestSecurityIdentityAugmentorExtension;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Enforces the {@code @AuthenticationContext} step-up policy for {@code @TestSecurity} tests, which
 * otherwise bypass the OIDC token verification that normally enforces it.
 */
@Singleton
@Unremovable
public class StepUpAuthenticationPolicyAugmentor implements TestSecurityIdentityAugmentorExtension {

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        return Uni.createFrom().item(identity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context,
            Map<String, Object> attributes) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }
        RoutingContext routingContext = HttpSecurityUtils.getRoutingContextAttribute(attributes);
        if (routingContext == null) {
            return Uni.createFrom().item(identity);
        }
        // StepUpAuthenticationPolicy is stored as a Consumer<TokenVerificationResult> that throws
        // AuthenticationFailedException when the required acr values or max token age are not met
        Object policy = routingContext.get("io.quarkus.oidc.runtime.step-up-auth");
        if (policy != null) {
            ((Consumer<TokenVerificationResult>) policy).accept(tokenVerificationResult(identity));
        }
        return Uni.createFrom().item(identity);
    }

    private static TokenVerificationResult tokenVerificationResult(SecurityIdentity identity) {
        io.quarkus.oidc.TokenIntrospection introspection = identity.getAttribute(OidcUtils.INTROSPECTION_ATTRIBUTE);
        if (introspection != null) {
            return new TokenVerificationResult(null, introspection);
        }
        JsonObject claims = null;
        AccessTokenCredential accessToken = identity.getCredential(AccessTokenCredential.class);
        if (accessToken != null && accessToken.getToken() != null) {
            claims = OidcUtils.decodeJwtContent(accessToken.getToken());
        }
        return new TokenVerificationResult(claims != null ? claims : new JsonObject(), null);
    }
}
