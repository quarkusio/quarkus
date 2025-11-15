package io.quarkus.oidc.runtime;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.crypto.SecretKey;

import org.jboss.logging.Logger;

import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcRedirectFilter;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.Redirect;
import io.smallrye.mutiny.Uni;

final class LazyTenantConfigContext implements TenantConfigContext {

    private static final Logger LOG = Logger.getLogger(LazyTenantConfigContext.class);

    private final Supplier<Uni<TenantConfigContext>> staticTenantCreator;
    private volatile TenantConfigContext delegate;

    LazyTenantConfigContext(TenantConfigContext delegate, Supplier<Uni<TenantConfigContext>> staticTenantCreator) {
        this.staticTenantCreator = staticTenantCreator;
        this.delegate = delegate;
    }

    @Override
    public Uni<TenantConfigContext> initialize() {
        if (!delegate.ready()) {
            LOG.debugf("Tenant '%s' is not initialized yet, trying to create OIDC connection now",
                    delegate.oidcConfig().tenantId().get());
            return staticTenantCreator.get().invoke(ctx -> {
                LazyTenantConfigContext.this.delegate = ctx;
                if (ctx.ready()) {
                    BackChannelLogoutHandler.fireBackChannelLogoutReadyEvent(ctx.oidcConfig());
                    ResourceMetadataHandler.fireResourceMetadataReadyEvent(ctx.oidcConfig());
                }
            });
        }
        return Uni.createFrom().item(delegate);
    }

    @Override
    public OidcTenantConfig oidcConfig() {
        return delegate.oidcConfig();
    }

    @Override
    public OidcProvider provider() {
        return delegate.provider();
    }

    @Override
    public boolean ready() {
        return delegate.ready();
    }

    @Override
    public OidcTenantConfig getOidcTenantConfig() {
        return delegate.getOidcTenantConfig();
    }

    @Override
    public OidcConfigurationMetadata getOidcMetadata() {
        return delegate.getOidcMetadata();
    }

    @Override
    public OidcProviderClientImpl getOidcProviderClient() {
        return delegate.getOidcProviderClient();
    }

    @Override
    public SecretKey getStateCookieEncryptionKey() {
        return delegate.getStateCookieEncryptionKey();
    }

    @Override
    public SecretKey getSessionCookieEncryptionKey() {
        return delegate.getSessionCookieEncryptionKey();
    }

    @Override
    public SecretKey getInternalIdTokenSigningKey() {
        return delegate.getInternalIdTokenSigningKey();
    }

    @Override
    public Key getTokenDecryptionKey() {
        return delegate.getTokenDecryptionKey();
    }

    @Override
    public List<OidcRedirectFilter> getOidcRedirectFilters(Redirect.Location loc) {
        return delegate.getOidcRedirectFilters(loc);
    }

    @Override
    public Map<Redirect.Location, List<OidcRedirectFilter>> getLocationToRedirectFilters() {
        return delegate.getLocationToRedirectFilters();
    }
}
