package io.quarkus.it.keycloak;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.OidcClientConfig;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant.Type;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class OidcClientCreator {

    @Inject
    OidcClients oidcClients;
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String oidcProviderAddress;
    @ConfigProperty(name = "quarkus.oidc.client-id")
    String oidcClientId;
    @ConfigProperty(name = "quarkus.oidc.credentials.secret")
    String oidcClientSecret;

    private volatile OidcClient oidcClient;

    public void init(@Observes StartupEvent event) {
        createOidcClient().subscribe().with(client -> {
            oidcClient = client;
        });
    }

    public OidcClient getOidcClient() {
        return oidcClient;
    }

    private Uni<OidcClient> createOidcClient() {
        OidcClientConfig cfg = OidcClientConfig
                .authServerUrl(oidcProviderAddress)
                .id("myclient")
                .clientId(oidcClientId)
                .credentials(oidcClientSecret)
                .grant(Type.PASSWORD)
                .grantOptions("password", Map.of("username", "jdoe", "password", "jdoe"))
                .build();
        return oidcClients.newClient(cfg);
    }
}
