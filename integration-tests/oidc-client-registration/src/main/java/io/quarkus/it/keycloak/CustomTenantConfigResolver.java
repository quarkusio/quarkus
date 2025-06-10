package io.quarkus.it.keycloak;

import static io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Jwt.Source.BEARER;
import static io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType.WEB_APP;

import java.net.URI;
import java.util.List;
import java.util.Map;

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
    ClientAuthWithSignedJwtCreator clientAuthWithSignedJwtCreator;

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
        defaultRegClientOnStartup = clientReg.registeredClient().await().indefinitely();
        if (!"Default Client".equals(defaultRegClientOnStartup.metadata().getClientName())) {
            throw new RuntimeException("Unexpected cient name");
        }

        // Confirm that access to the client-specific registration endpoint works.
        defaultRegClientOnStartup = defaultRegClientOnStartup.update(
                ClientMetadata.builder().clientName("Default Client Updated").build()).await()
                .indefinitely();

        // Read using RegisteredClient.read
        RegisteredClient defaultRegClientOnStartup2 = defaultRegClientOnStartup.read().await().indefinitely();

        // Read using OidcClientRegistration.readClient(regUri, regToken)
        defaultRegClientOnStartup = clientReg
                .readClient(defaultRegClientOnStartup.registrationUri(),
                        defaultRegClientOnStartup.registrationToken())
                .await().indefinitely();

        if (!defaultRegClientOnStartup2.metadata().getClientId().equals(
                defaultRegClientOnStartup2.metadata().getClientId())) {
            throw new RuntimeException("Inconsistent read results");
        }

        // Custom 'tenant-client' OIDC client registration, client is registered at startup
        tenantRegClientOnStartup = clientRegs.getClientRegistration("tenant-client").registeredClient()
                .await().indefinitely();

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

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {
        if (routingContext.request().path().endsWith("/protected")) {
            return Uni.createFrom().item(createTenantConfig("registered-client", defaultRegClientOnStartup.metadata()));
        } else if (routingContext.request().path().endsWith("/protected/tenant")) {
            return Uni.createFrom().item(createTenantConfig("registered-client-tenant", tenantRegClientOnStartup.metadata()));
        } else if (routingContext.request().path().endsWith("/protected/dynamic")) {
            if (regClientDynamically == null) {
                // New client registration done dynamically at the request time,
                // using the same registration endpoint used to register a default client at startup
                OidcClientRegistrationConfig clientRegConfig = OidcClientRegistrationConfig.builder()
                        .registrationPath(authServerUrl + "/clients-registrations/openid-connect")
                        .metadata("Dynamic Client", "http://localhost:8081/protected/dynamic")
                        .build();

                return clientRegs.newClientRegistration(clientRegConfig)
                        .onItem().transformToUni(cfg -> cfg.registeredClient())
                        .onItem().transform(r -> registeredClientDynamically(r));
            } else {
                return Uni.createFrom()
                        .item(createTenantConfig("registered-client-dynamically", regClientDynamically.metadata()));
            }
        } else if (routingContext.request().path().endsWith("/protected/dynamic-tenant")) {
            if (regClientDynamicTenant == null) {
                // New client registration done dynamically at the request time, using a new configured
                // an OIDC tenant specific registration endpoint

                OidcClientRegistration tenantClientReg = clientRegs.getClientRegistration("dynamic-tenant");
                ClientMetadata metadata = createMetadata("http://localhost:8081/protected/dynamic-tenant",
                        "Dynamic Tenant Client");

                return tenantClientReg.registerClient(metadata)
                        .onItem().transform(r -> registeredClientDynamicTenant(r));
            } else {
                return Uni.createFrom()
                        .item(createTenantConfig("registered-client-dynamic-tenant", regClientDynamicTenant.metadata()));
            }
        } else if (routingContext.request().path().endsWith("/protected/multi1")) {
            return Uni.createFrom().item(createTenantConfig("registered-client-multi1",
                    regClientsMulti.get("/protected/multi1").metadata()));
        } else if (routingContext.request().path().endsWith("/protected/multi2")) {
            return Uni.createFrom().item(createTenantConfig("registered-client-multi2",
                    regClientsMulti.get("/protected/multi2").metadata()));
        } else if (routingContext.normalizedPath().endsWith("/jwt-bearer-token-file")) {
            var clientMetadata = clientAuthWithSignedJwtCreator.getCreatedClientMetadata();
            var redirectPath = URI.create(clientMetadata.getRedirectUris().get(0)).getPath();
            var tenantConfig = OidcTenantConfig
                    .authServerUrl(authServerUrl)
                    .applicationType(WEB_APP)
                    .tenantId("registered-client-jwt-bearer-token-file")
                    .clientName(clientMetadata.getClientName())
                    .clientId(clientMetadata.getClientId())
                    .authentication().redirectPath(redirectPath).end()
                    .credentials()
                    .jwt()
                    .source(BEARER)
                    .tokenPath(clientAuthWithSignedJwtCreator.getSignedJwtTokenPath())
                    .endCredentials()
                    .build();
            return Uni.createFrom().item(tenantConfig);
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
