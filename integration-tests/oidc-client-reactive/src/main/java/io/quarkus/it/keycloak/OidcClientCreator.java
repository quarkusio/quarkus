package io.quarkus.it.keycloak;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientConfig;
import io.quarkus.oidc.client.OidcClientConfig.Grant.Type;
import io.quarkus.oidc.client.OidcClients;
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
        OidcClientConfig cfg = new OidcClientConfig();
        cfg.setId("myclient");
        cfg.setAuthServerUrl(oidcProviderAddress);
        cfg.setClientId(oidcClientId);
        cfg.getCredentials().setSecret(oidcClientSecret);
        cfg.getGrant().setType(Type.PASSWORD);
        cfg.setGrantOptions(Map.of("password",
                Map.of("username", "jdoe", "password", "jdoe")));
        return oidcClients.newClient(cfg);
    }
}
