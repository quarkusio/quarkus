package io.quarkus.oidc.runtime;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityUtils.getRoutingContextAttribute;

import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.TenantIdentityProvider;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

final class TenantSpecificOidcIdentityProvider extends OidcIdentityProvider
        implements TenantIdentityProvider {

    private final String tenantId;
    private final BlockingSecurityExecutor blockingExecutor;

    TenantSpecificOidcIdentityProvider(String tenantId, DefaultTenantConfigResolver resolver,
            BlockingSecurityExecutor blockingExecutor) {
        super(resolver, blockingExecutor);
        this.blockingExecutor = blockingExecutor;
        this.tenantId = tenantId;
    }

    TenantSpecificOidcIdentityProvider(String tenantId) {
        this(tenantId, Arc.container().instance(DefaultTenantConfigResolver.class).get(),
                Arc.container().instance(BlockingSecurityExecutor.class).get());
    }

    @Override
    public Uni<SecurityIdentity> authenticate(AccessTokenCredential token) {
        return authenticate(new TokenAuthenticationRequest(token));
    }

    @Override
    protected Uni<TenantConfigContext> resolveTenantConfigContext(TokenAuthenticationRequest request,
            AuthenticationRequestContext context) {
        return tenantResolver.resolveContext(tenantId).onItem().ifNull().failWith(new Supplier<Throwable>() {
            @Override
            public Throwable get() {
                return new OIDCException("Failed to resolve tenant context");
            }
        });
    }

    @Override
    protected Map<String, Object> getRequestData(TokenAuthenticationRequest request) {
        RoutingContext context = getRoutingContextAttribute(request);
        if (context != null) {
            return context.data();
        }
        return Map.of();
    }

    private Uni<SecurityIdentity> authenticate(TokenAuthenticationRequest request) {
        return authenticate(request, new AuthenticationRequestContext() {
            @Override
            public Uni<SecurityIdentity> runBlocking(Supplier<SecurityIdentity> function) {
                return blockingExecutor.executeBlocking(function);
            }
        });
    }
}
