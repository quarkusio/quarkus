package io.quarkus.oidc.runtime;

import java.security.Key;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.crypto.SecretKey;

import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcRedirectFilter;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.Redirect;
import io.smallrye.mutiny.Uni;

public sealed interface TenantConfigContext permits TenantConfigContextImpl, LazyTenantConfigContext {

    /**
     * Tenant configuration
     */
    OidcTenantConfig oidcConfig();

    /**
     * OIDC Provider
     */
    OidcProvider provider();

    boolean ready();

    OidcTenantConfig getOidcTenantConfig();

    OidcConfigurationMetadata getOidcMetadata();

    OidcProviderClientImpl getOidcProviderClient();

    SecretKey getStateCookieEncryptionKey();

    SecretKey getSessionCookieEncryptionKey();

    SecretKey getInternalIdTokenSigningKey();

    Key getTokenDecryptionKey();

    List<OidcRedirectFilter> getOidcRedirectFilters(Redirect.Location loc);

    Map<Redirect.Location, List<OidcRedirectFilter>> getLocationToRedirectFilters();

    /**
     * Only static tenants that are not {@link #ready()} can and need to be initialized.
     *
     * @return self, or in case of not {@link #ready()}, possibly ready self
     */
    default Uni<TenantConfigContext> initialize() {
        return Uni.createFrom().item(this);
    }

    static TenantConfigContext createReady(OidcProvider provider, OidcTenantConfig config) {
        return new TenantConfigContextImpl(provider, config);
    }

    static TenantConfigContext createNotReady(OidcProvider provider, OidcTenantConfig config,
            Supplier<Uni<TenantConfigContext>> staticTenantCreator) {
        var notReadyContext = new TenantConfigContextImpl(provider, config, false);
        return new LazyTenantConfigContext(notReadyContext, staticTenantCreator);
    }
}
