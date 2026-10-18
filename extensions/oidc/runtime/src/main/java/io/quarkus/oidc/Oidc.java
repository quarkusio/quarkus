package io.quarkus.oidc;

import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;

/**
 * A CDI event that facilitates programmatic OIDC tenant setup.
 * OIDC tenants must be created from an observer method like in the example below:
 *
 * <pre>
 * {@code
 * public class OidcSetup {
 *
 *     void createDefaultTenant(@Observes Oidc oidc) {
 *         var defaultTenant = OidcTenantConfig.authServerUrl("https://oidc-provider-hostname/").build();
 *         oidc.create(defaultTenant);
 *     }
 * }
 * }
 * </pre>
 *
 * The example above is equivalent to configuring {@code quarkus.oidc.auth-server-url=https://oidc-provider-hostname/}
 * in the application.properties.
 */
public interface Oidc {

    /**
     * Creates OIDC tenant.
     *
     * @param tenantConfig tenant config; must not be null
     */
    void create(OidcTenantConfig tenantConfig);

    /**
     * Creates default OIDC tenant with the {@link ApplicationType#SERVICE} application type.
     *
     * @param authServerUrl {@link OidcTenantConfig#authServerUrl()}
     */
    void createServiceApp(String authServerUrl);

    /**
     * Creates default OIDC tenant with the {@link ApplicationType#WEB_APP} application type.
     *
     * @param authServerUrl {@link OidcTenantConfig#authServerUrl()}
     * @param clientId {@link OidcTenantConfig#clientId()}
     * @param clientSecret {@link OidcClientCommonConfig.Credentials#secret()}
     */
    void createWebApp(String authServerUrl, String clientId, String clientSecret);
}
