package io.quarkus.oidc.client.runtime;

import static io.quarkus.oidc.client.runtime.OidcClientsConfig.DEFAULT_CLIENT_KEY;
import static io.quarkus.oidc.common.runtime.OidcCommonUtils.getDiscoveryUri;

import java.util.Map;

import jakarta.inject.Inject;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;

@Readiness
public class OidcClientHealthCheck implements HealthCheck {
    private static final String HEALTH_CHECK_NAME = "OIDC Client Health Check";

    private static final String OK_STATUS = "OK";
    private static final String ERROR_STATUS = "Error";
    private static final String DISABLED_STATUS = "Disabled";
    private static final String UNKNOWN_STATUS = "Unknown";

    @Inject
    OidcClientsImpl oidcClients;

    @Inject
    OidcClientsConfig oidcClientsConfig;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder builder = HealthCheckResponse.builder()
                .name(HEALTH_CHECK_NAME)
                .up();

        String status = checkClient(builder, DEFAULT_CLIENT_KEY, oidcClients.getClient());
        boolean atLeastOneClientIsReady = OK_STATUS.equals(status);

        for (Map.Entry<String, OidcClient> entry : oidcClients.getStaticOidcClients().entrySet()) {
            status = checkClient(builder, entry.getKey(), entry.getValue());
            if (!atLeastOneClientIsReady) {
                atLeastOneClientIsReady = OK_STATUS.equals(status);
            }
        }

        if (!atLeastOneClientIsReady) {
            builder.down();
        }
        return builder.build();
    }

    private String checkClient(HealthCheckResponseBuilder builder, String clientId,
            OidcClient oidcClient) {
        String name = clientId;
        String status = null;
        if (oidcClient instanceof OidcClientRecorder.DisabledOidcClient) {
            status = DISABLED_STATUS;
            var disabledClientConfig = oidcClientsConfig.namedClients().get(clientId);
            if (disabledClientConfig != null && disabledClientConfig.clientName().isPresent()) {
                name = disabledClientConfig.clientName().get();
            }
        } else if (oidcClient instanceof OidcClientImpl oidcClientImpl) {
            var oidcClientConfig = oidcClientImpl.getConfig();
            if (oidcClientConfig.clientName().isPresent()) {
                name = oidcClientConfig.clientName().get();
            }
            if (!oidcClientConfig.clientEnabled()) {
                status = DISABLED_STATUS;
            } else if (oidcClientConfig.discoveryEnabled().orElse(true) && oidcClientConfig.authServerUrl().isPresent()) {
                try {
                    String authServerUriString = OidcCommonUtils.getAuthServerUrl(oidcClientConfig);
                    String discoveryUri = getDiscoveryUri(authServerUriString);
                    status = checkHealth(oidcClientImpl, discoveryUri).await().indefinitely();
                } catch (Exception e) {
                    status = ERROR_STATUS + ": " + e.getMessage();
                }
            } else {
                // We may introduce a metadata health property
                status = UNKNOWN_STATUS;
            }
        }

        if (status != null) {
            builder.withData(name, status);
        }

        return status;
    }

    private static Uni<String> checkHealth(OidcClientImpl oidcClient, String healthUri) {
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
