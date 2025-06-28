package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.common.runtime.OidcConstants.INTROSPECTION_TOKEN_SUB;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRoutingContextAttribute;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TokenIntrospection;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.websockets.next.runtime.spi.security.WebSocketIdentityUpdateRequest;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class WebSocketIdentityUpdateProvider implements IdentityProvider<WebSocketIdentityUpdateRequest> {

    @Inject
    DefaultTenantConfigResolver resolver;

    @Inject
    BlockingSecurityExecutor blockingExecutor;

    WebSocketIdentityUpdateProvider() {
    }

    @Override
    public Class<WebSocketIdentityUpdateRequest> getRequestType() {
        return WebSocketIdentityUpdateRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(WebSocketIdentityUpdateRequest request,
            AuthenticationRequestContext authenticationRequestContext) {
        return authenticate(request.getCredential().getToken(), getRoutingContextAttribute(request))
                .onItem().transformToUni(newIdentity -> {
                    if (newIdentity.getPrincipal() instanceof JsonWebToken newJwt
                            && request.getCurrentSecurityIdentity().getPrincipal() instanceof JsonWebToken previousJwt) {
                        String currentSubject = newJwt.getSubject();
                        String previousSubject = previousJwt.getSubject();
                        if (currentSubject == null || !currentSubject.equals(previousSubject)) {
                            return Uni.createFrom().failure(new AuthenticationFailedException(
                                    "JWT token claim 'sub' value '%s' is different to the previous claim value '%s'"
                                            .formatted(currentSubject, previousSubject)));
                        } else {
                            return Uni.createFrom().item(newIdentity);
                        }
                    }

                    TokenIntrospection introspection = OidcUtils.getAttribute(newIdentity, OidcUtils.INTROSPECTION_ATTRIBUTE);
                    if (introspection != null) {
                        String sub = introspection.getString(INTROSPECTION_TOKEN_SUB);
                        if (sub != null && !sub.isEmpty()) {
                            TokenIntrospection previousIntrospection = OidcUtils
                                    .getAttribute(request.getCurrentSecurityIdentity(), OidcUtils.INTROSPECTION_ATTRIBUTE);
                            if (previousIntrospection == null
                                    || !sub.equals(previousIntrospection.getString(INTROSPECTION_TOKEN_SUB))) {
                                return Uni.createFrom().failure(new AuthenticationFailedException(
                                        "Token introspection result claim 'sub' value '%s' is different to the previous claim value '%s'"
                                                .formatted(sub, previousIntrospection == null ? null
                                                        : previousIntrospection.getString(INTROSPECTION_TOKEN_SUB))));
                            } else {
                                return Uni.createFrom().item(newIdentity);
                            }
                        }
                    }

                    return Uni.createFrom().failure(new AuthenticationFailedException(
                            "Cannot verify that updated identity represents same subject as the 'sub' claim is not available"));
                });
    }

    private Uni<SecurityIdentity> authenticate(String accessToken, RoutingContext routingContext) {
        final OidcTenantConfig tenantConfig = routingContext.get(OidcTenantConfig.class.getName());
        if (tenantConfig == null) {
            return Uni.createFrom().failure(new AuthenticationFailedException(
                    "Cannot update SecurityIdentity because OIDC tenant wasn't resolved for current WebSocket connection"));
        }
        final var tenantId = tenantConfig.tenantId().get();
        final var identityProvider = new TenantSpecificOidcIdentityProvider(tenantId, resolver, blockingExecutor);
        final var credential = new AccessTokenCredential(accessToken);
        return identityProvider.authenticate(credential);
    }
}
