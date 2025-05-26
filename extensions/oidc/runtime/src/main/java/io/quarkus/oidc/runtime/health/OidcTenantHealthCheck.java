package io.quarkus.oidc.runtime.health;

import java.util.Map.Entry;

import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.oidc.runtime.OidcProviderClientImpl;
import io.quarkus.oidc.runtime.OidcUtils;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.oidc.runtime.TenantConfigContext;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

@Readiness
public class OidcTenantHealthCheck implements HealthCheck {
    private static final String HEALTH_CHECK_NAME = "OIDC Provider Health Check";

    private static final String OK_STATUS = "OK";
    private static final String ERROR_STATUS = "Error";
    private static final String DISABLED_STATUS = "Disabled";
    private static final String UNKNOWN_STATUS = "Unknown";
    private static final String NOT_READY_STATUS = "Not Ready";

    @Inject
    TenantConfigBean tenantConfigBean;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder()
                .name(HEALTH_CHECK_NAME)
                .up();

        String status = checkTenant(builder, OidcUtils.DEFAULT_TENANT_ID, tenantConfigBean.getDefaultTenant());
        boolean atLeastOneTenantIsReady = OK_STATUS.equals(status);

        for (Entry<String, TenantConfigContext> entry : tenantConfigBean.getStaticTenantsConfig().entrySet()) {
            status = checkTenant(builder, entry.getKey(), entry.getValue());
            if (!atLeastOneTenantIsReady) {
                atLeastOneTenantIsReady = OK_STATUS.equals(status);
            }
        }

        if (!atLeastOneTenantIsReady) {
            builder.down();
        }
        return builder.build();
    }

    private static String checkTenant(HealthCheckResponseBuilder builder, String tenantId,
            TenantConfigContext tenantConfigContext) {

        if (tenantConfigContext.oidcConfig() == null) {
            return null;
        }
        String name = tenantConfigContext.oidcConfig().clientName().orElse(tenantId);

        String status = null;
        if (tenantConfigContext.getOidcProviderClient() == null) {
            if (!tenantConfigContext.oidcConfig().tenantEnabled()) {
                status = DISABLED_STATUS;
            } else if (!tenantConfigContext.ready()) {
                status = NOT_READY_STATUS;
            }
        } else if (tenantConfigContext.getOidcMetadata().getDiscoveryUri() == null) {
            // We may introduce a metadata health property
            status = UNKNOWN_STATUS;
        } else {
            try {
                status = checkHealth(tenantConfigContext.getOidcProviderClient(),
                        tenantConfigContext.getOidcMetadata().getDiscoveryUri()).await().indefinitely();
            } catch (Exception e) {
                status = ERROR_STATUS + ": " + e.getMessage();
            }
        }
        if (status != null) {
            builder.withData(name, status);
        }
        return status;
    }

    private static Uni<String> checkHealth(OidcProviderClientImpl oidcClient, String healthUri) {

        HttpRequest<Buffer> request = oidcClient.getWebClient().headAbs(healthUri);
        return request.send().onItem().transform(resp -> {
            Buffer buffer = resp.body();
            if (resp.statusCode() == 200) {
                return OK_STATUS;
            } else {
                String errorMessage = buffer != null ? buffer.toString() : null;
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    return ERROR_STATUS + ": " + errorMessage;
                } else {
                    return ERROR_STATUS;
                }

            }
        });

    }
}
