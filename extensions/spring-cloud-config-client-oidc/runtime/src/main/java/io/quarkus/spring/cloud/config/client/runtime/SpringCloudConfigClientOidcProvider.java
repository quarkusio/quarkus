package io.quarkus.spring.cloud.config.client.runtime;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.vertx.mutiny.ext.web.client.HttpRequest;

public class SpringCloudConfigClientOidcProvider implements SpringCloudClientCredentialsProvider {

    private OidcClient oidcClient;

    @ConfigProperty(name = "quarkus.spring-cloud-config.oidc-client-name")
    Optional<String> oidcClientId;

    @PostConstruct
    public void init() {
        OidcClients oidcClients = Arc.container().instance(OidcClients.class).get();
        oidcClient = oidcClientId.isPresent()
                ? Objects.requireNonNull(oidcClients.getClient(oidcClientId.get()), "Unknown client")
                : oidcClients.getClient();
    }

    @Override
    public void addAuthenticationInfo(HttpRequest<?> request, SpringCloudConfigClientConfig springCloudConfigClientConfig) {
        request.bearerTokenAuthentication(oidcClient.getTokens().await().indefinitely().getAccessToken());
    }
}
