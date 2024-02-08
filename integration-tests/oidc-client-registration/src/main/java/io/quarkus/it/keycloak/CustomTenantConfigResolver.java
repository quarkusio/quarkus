package io.quarkus.it.keycloak;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.Json;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.client.registration.ClientMetadata;
import io.quarkus.oidc.client.registration.OidcClientRegistration;
import io.quarkus.oidc.client.registration.OidcClientRegistrationConfig;
import io.quarkus.oidc.client.registration.OidcClientRegistrations;
import io.quarkus.oidc.client.registration.RegisteredClient;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    OidcClientRegistration clientReg;

    @Inject
    OidcClientRegistrations clientRegs;

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    volatile RegisteredClient defaultRegClientOnStartup;
    volatile RegisteredClient tenantRegClientOnStartup;
    volatile RegisteredClient regClientDynamically;
    volatile RegisteredClient regClientDynamicTenant;

    volatile Map<String, RegisteredClient> regClientsMulti;

    void onStartup(@Observes StartupEvent event) {

        // Default OIDC client registration, client is registered at startup
        defaultRegClientOnStartup = clientReg.registeredClient();

        //regClientOnStartup = regClientOnStartup.update(createMetadata("http://localhost:8081/protected/postlogout")).await()
        //        .indefinitely();
        // Confirm that access to the client-specific registration endpoint works.
        // Sending an update with Keycloak works but ID tokens end up without a preferred_username impacting tests
        //so we just re-read the configuration
        defaultRegClientOnStartup = defaultRegClientOnStartup.read().await().indefinitely();

        // Custom 'tenant-client' OIDC client registration, client is registered at startup
        tenantRegClientOnStartup = clientRegs.getClientRegistration("tenant-client").registeredClient();

        // Two custom OIDC client registrations registered right now using the same registration endpoint
        // as one which was used to register defaultRegClientOnStartup and defaultRegClientOnStartup clients
        // at startup
        ClientMetadata clientMetadataMulti1 = createMetadata("http://localhost:8081/protected/multi1", "Multi1 Client");
        ClientMetadata clientMetadataMulti2 = createMetadataWithBuilder("http://localhost:8081/protected/multi2",
                "Multi2 Client");

        Uni<Map<String, RegisteredClient>> clients = clientReg
                .registerClients(List.of(clientMetadataMulti1, clientMetadataMulti2))
                .collect().asMap(r -> URI.create(r.metadata().getRedirectUris().get(0)).getPath(), r -> r);
        regClientsMulti = clients.await().indefinitely();
    }

    void onShutdown(@Observes ShutdownEvent event) {

        if (defaultRegClientOnStartup != null) {
            defaultRegClientOnStartup.delete().await().indefinitely();
        }
        if (tenantRegClientOnStartup != null) {
            tenantRegClientOnStartup.delete().await().indefinitely();
        }
        if (regClientDynamically != null) {
            regClientDynamically.delete().await().indefinitely();
        }
        if (regClientDynamicTenant != null) {
            regClientDynamicTenant.delete().await().indefinitely();
        }
        if (regClientsMulti != null) {
            for (RegisteredClient client : regClientsMulti.values()) {
                client.delete().await().indefinitely();
            }
        }
    }

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {
        if (routingContext.request().path().endsWith("/protected")) {
            return Uni.createFrom().item(createTenantConfig("registered-client", defaultRegClientOnStartup.metadata()));
        } else if (routingContext.request().path().endsWith("/protected/tenant")) {
            return Uni.createFrom().item(createTenantConfig("registered-client-tenant", tenantRegClientOnStartup.metadata()));
        } else if (routingContext.request().path().endsWith("/protected/dynamic")) {
            // New client registration done dynamically at the request time,
            // using the same registration endpoint used to register a default client at startup
            OidcClientRegistrationConfig clientRegConfig = new OidcClientRegistrationConfig();
            clientRegConfig.registrationPath = Optional.of(
                    authServerUrl + "/clients-registrations/openid-connect");
            clientRegConfig.metadata.redirectUri = Optional.of("http://localhost:8081/protected/dynamic");
            clientRegConfig.metadata.clientName = Optional.of("Dynamic Client");

            return clientRegs.newClientRegistration(clientRegConfig)
                    .onItem().transform(cfg -> registeredClientDynamically(cfg.registeredClient()));
        } else if (routingContext.request().path().endsWith("/protected/dynamic-tenant")) {
            // New client registration done dynamically at the request time, using a new configured
            // an OIDC tenant specific registration endpoint
            OidcClientRegistration tenantClientReg = clientRegs.getClientRegistration("dynamic-tenant");
            ClientMetadata metadata = createMetadata("http://localhost:8081/protected/dynamic-tenant",
                    "Dynamic Tenant Client");

            return tenantClientReg.registerClient(metadata)
                    .onItem().transform(r -> registeredClientDynamicTenant(r));
        } else if (routingContext.request().path().endsWith("/protected/multi1")) {
            return Uni.createFrom().item(createTenantConfig("registered-client-multi1",
                    regClientsMulti.get("/protected/multi1").metadata()));
        } else if (routingContext.request().path().endsWith("/protected/multi2")) {
            return Uni.createFrom().item(createTenantConfig("registered-client-multi2",
                    regClientsMulti.get("/protected/multi2").metadata()));
        }

        return null;
    }

    private OidcTenantConfig registeredClientDynamically(RegisteredClient newClient) {

        regClientDynamically = newClient;

        return createTenantConfig("registered-client-dynamically", regClientDynamically.metadata());
    }

    private OidcTenantConfig registeredClientDynamicTenant(RegisteredClient newClient) {

        regClientDynamicTenant = newClient;

        return createTenantConfig("registered-client-dynamic-tenant", regClientDynamicTenant.metadata());
    }

    private OidcTenantConfig createTenantConfig(String tenantId, ClientMetadata metadata) {
        OidcTenantConfig oidcConfig = new OidcTenantConfig();
        oidcConfig.setTenantId(tenantId);
        oidcConfig.setAuthServerUrl(authServerUrl);
        oidcConfig.setApplicationType(ApplicationType.WEB_APP);
        oidcConfig.setClientName(metadata.getClientName());
        oidcConfig.setClientId(metadata.getClientId());
        oidcConfig.getCredentials().setSecret(metadata.getClientSecret());
        String redirectUri = metadata.getRedirectUris().get(0);
        oidcConfig.getAuthentication().setRedirectPath(URI.create(redirectUri).getPath());
        return oidcConfig;
    }

    protected static ClientMetadata createMetadata(String redirectUri, String clientName) {
        return new ClientMetadata(Json.createObjectBuilder()
                .add(OidcConstants.CLIENT_METADATA_REDIRECT_URIS, Json.createArrayBuilder().add(redirectUri))
                .add(OidcConstants.CLIENT_METADATA_CLIENT_NAME, clientName)
                .build());
    }

    protected static ClientMetadata createMetadataWithBuilder(String redirectUri, String clientName) {
        return ClientMetadata.builder().redirectUri(redirectUri).clientName(clientName).build();
    }
}
