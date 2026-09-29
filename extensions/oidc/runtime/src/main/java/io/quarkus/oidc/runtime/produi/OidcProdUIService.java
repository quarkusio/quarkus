package io.quarkus.oidc.runtime.produi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.oidc.OidcConfigurationMetadata;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.oidc.runtime.TenantConfigContext;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;

/**
 * Read-only Prod UI view of the configured OIDC tenants. For each tenant it
 * exposes only non-sensitive configuration - tenant id, application type, auth
 * server URL, discovery flag, client id (a public identifier, not a secret),
 * roles source - together with the discovered provider metadata endpoints
 * (issuer, JWKS, authorization, token, user-info, introspection, end-session).
 * <p>
 * The Dev UI is not reused: it is a full login / token acquisition console
 * (grant flows, Keycloak admin, Dev Services) that is not production-safe. This
 * view deliberately never touches {@code credentials()} (client secrets, JWT
 * keys, passwords) and strips any credentials embedded in URLs, so no secret is
 * ever exposed.
 */
@ApplicationScoped
public class OidcProdUIService {

    private static final String DEFAULT_TENANT = "<default>";

    @Inject
    TenantConfigBean tenantConfigBean;

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the configured OIDC tenants and their provider metadata (no secrets)")
    public List<TenantInfo> getTenants() {
        // Deduplicate by tenant id: the default tenant plus all named static tenants
        Map<String, TenantConfigContext> contexts = new LinkedHashMap<>();
        TenantConfigContext defaultTenant = tenantConfigBean.getDefaultTenant();
        if (defaultTenant != null) {
            contexts.put(tenantId(defaultTenant), defaultTenant);
        }
        for (TenantConfigContext context : tenantConfigBean.getStaticTenantsConfig().values()) {
            if (context != null) {
                contexts.putIfAbsent(tenantId(context), context);
            }
        }

        List<TenantInfo> tenants = new ArrayList<>();
        for (TenantConfigContext context : contexts.values()) {
            tenants.add(toTenantInfo(context));
        }
        tenants.sort(Comparator.comparing(TenantInfo::tenantId));
        return tenants;
    }

    private String tenantId(TenantConfigContext context) {
        return context.getOidcTenantConfig().tenantId().orElse(DEFAULT_TENANT);
    }

    private TenantInfo toTenantInfo(TenantConfigContext context) {
        OidcTenantConfig config = context.getOidcTenantConfig();
        return new TenantInfo(
                config.tenantId().orElse(DEFAULT_TENANT),
                config.tenantEnabled(),
                config.applicationType().map(Enum::name).orElse(null),
                sanitize(config.authServerUrl().orElse(null)),
                config.discoveryEnabled().orElse(null),
                config.clientId().orElse(null),
                config.roles().source().map(Enum::name).orElse(null),
                metadataOf(context));
    }

    private MetadataInfo metadataOf(TenantConfigContext context) {
        OidcConfigurationMetadata metadata;
        try {
            metadata = context.getOidcMetadata();
        } catch (RuntimeException e) {
            // A not-yet-initialized tenant may not have resolved its metadata; treat as absent
            metadata = null;
        }
        if (metadata == null) {
            return null;
        }
        return new MetadataInfo(
                sanitize(metadata.getIssuer()),
                sanitize(metadata.getJsonWebKeySetUri()),
                sanitize(metadata.getAuthorizationUri()),
                sanitize(metadata.getTokenUri()),
                sanitize(metadata.getUserInfoUri()),
                sanitize(metadata.getIntrospectionUri()),
                sanitize(metadata.getEndSessionUri()));
    }

    /**
     * Removes any credentials embedded in a URL (the {@code user:password@}
     * userinfo component) so secrets are never exposed in the Prod UI.
     */
    private String sanitize(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        try {
            URI uri = new URI(url);
            if (uri.getUserInfo() != null) {
                return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                        uri.getPath(), uri.getQuery(), uri.getFragment()).toString();
            }
            return url;
        } catch (URISyntaxException e) {
            // If the URL cannot be parsed, still strip any embedded userinfo defensively
            return url.replaceFirst("://[^/@]*@", "://");
        }
    }

    public record TenantInfo(String tenantId, boolean enabled, String applicationType, String authServerUrl,
            Boolean discoveryEnabled, String clientId, String rolesSource, MetadataInfo metadata) {
    }

    public record MetadataInfo(String issuer, String jwksUri, String authorizationUri, String tokenUri,
            String userInfoUri, String introspectionUri, String endSessionUri) {
    }
}
