package io.quarkus.oidc.deployment.produi;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.oidc.runtime.produi.OidcProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * Contributes a read-only Prod UI page listing the configured OIDC tenants and
 * their provider metadata (issuer, JWKS and the other discovery endpoints). The
 * Dev UI is not reused: it is a full login / token-acquisition console (grant
 * flows, Keycloak admin, Dev Services) which is not production-safe. A bespoke
 * read-only component is provided instead, backed by a runtime service that never
 * exposes client secrets or credentials. The page is only contributed when an
 * OIDC tenant is enabled and either a client id or an auth server URL is set.
 */
public class OidcProdUIProcessor {

    private static final String TENANT_ENABLED_CONFIG_KEY = "quarkus.oidc.tenant-enabled";
    private static final String CLIENT_ID_CONFIG_KEY = "quarkus.oidc.client-id";
    private static final String AUTH_SERVER_URL_CONFIG_KEY = "quarkus.oidc.auth-server-url";

    @BuildStep
    void createProdUIPage(BuildProducer<JsonRPCProvidersBuildItem> jsonRPCProducer,
            BuildProducer<ProdUIPageBuildItem> prodUIProducer) {
        if (!isOidcEnabled()) {
            return;
        }

        jsonRPCProducer.produce(new JsonRPCProvidersBuildItem(OidcProdUIService.class));

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("OIDC")
                .icon("font-awesome-solid:key")
                .componentLink("pwc-oidc-tenants.js"));
        prodUIProducer.produce(page);
    }

    private static boolean isOidcEnabled() {
        boolean tenantEnabled = ConfigProvider.getConfig()
                .getOptionalValue(TENANT_ENABLED_CONFIG_KEY, Boolean.class).orElse(true);
        return tenantEnabled
                && (ConfigUtils.isPropertyPresent(CLIENT_ID_CONFIG_KEY)
                        || ConfigUtils.isPropertyPresent(AUTH_SERVER_URL_CONFIG_KEY));
    }
}
