package io.quarkus.oidc.runtime;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRoutingContextAttribute;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.websockets.next.runtime.spi.telemetry.WebSocketIdentityUpdateRequest;
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
        return authenticate(request.getCredential().getToken(), getRoutingContextAttribute(request));
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
