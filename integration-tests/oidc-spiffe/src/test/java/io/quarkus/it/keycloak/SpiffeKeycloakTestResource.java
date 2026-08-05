package io.quarkus.it.keycloak;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.testcontainers.Testcontainers;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.quarkus.test.keycloak.server.KeycloakContainer;

public class SpiffeKeycloakTestResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private static final String REALM = "quarkus-spiffe";
    private static final String CLIENT_ID = "quarkus-app-spiffe";
    private static final String IDP_ALIAS = "spiffe-idp";
    private static final String TRUST_DOMAIN = "spiffe://test.quarkus.io";
    private static final String SPIFFE_ID = "spiffe://test.quarkus.io/test-workload";

    private KeycloakContainer keycloak;
    private DevServicesContext devServicesContext;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.devServicesContext = context;
    }

    @Override
    public Map<String, String> start() {
        // Read SPIFFE dev service bundle endpoint URL
        String spiffeBaseUrl = devServicesContext.devServicesProperties()
                .get("dev-svc.quarkus.spiffe-client.devservices.base-url");
        int bundlePort = URI.create(spiffeBaseUrl).getPort();

        // Expose the bundle endpoint port to Docker containers BEFORE starting Keycloak
        Testcontainers.exposeHostPorts(bundlePort);
        String bundleEndpoint = "http://host.testcontainers.internal:" + bundlePort + "/bundle";

        // Start Keycloak with SPIFFE feature enabled
        keycloak = new KeycloakContainer();
        keycloak.withEnv("KC_FEATURES", "spiffe");
        keycloak.start();

        // Create realm with SPIFFE IdP and federated-jwt client
        KeycloakTestClient client = new KeycloakTestClient(keycloak.getServerUrl());
        RealmRepresentation realm = createRealm();
        realm.addIdentityProvider(createSpiffeIdp(bundleEndpoint));
        realm.getClients().add(createSpiffeClient());
        realm.getUsers().add(createUser("alice", "user"));
        realm.getUsers().add(createUser("admin", "user", "admin"));
        client.createRealm(realm);

        String authServerUrl = keycloak.getServerUrl() + "/realms/" + REALM;
        Map<String, String> config = new HashMap<>();
        config.put("quarkus.oidc.auth-server-url", authServerUrl);
        config.put("quarkus.oidc.client-id", CLIENT_ID);
        config.put("quarkus.oidc.bearer.auth-server-url", authServerUrl);
        config.put("quarkus.oidc.bearer.client-id", CLIENT_ID);
        config.put("quarkus.oidc-client.auth-server-url", authServerUrl);
        config.put("quarkus.oidc-client.client-id", CLIENT_ID);
        return config;
    }

    @Override
    public void stop() {
        if (keycloak != null) {
            keycloak.stop();
        }
    }

    private static RealmRepresentation createRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setRequiredActions(List.of());

        RolesRepresentation roles = new RolesRepresentation();
        roles.setRealm(new ArrayList<>());
        realm.setRoles(roles);
        realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));

        return realm;
    }

    private static IdentityProviderRepresentation createSpiffeIdp(String bundleEndpoint) {
        IdentityProviderRepresentation idp = new IdentityProviderRepresentation();
        idp.setProviderId("spiffe");
        idp.setAlias(IDP_ALIAS);
        idp.setEnabled(true);
        idp.setConfig(Map.of(
                "trustDomain", TRUST_DOMAIN,
                "bundleEndpoint", bundleEndpoint));
        return idp;
    }

    private static ClientRepresentation createSpiffeClient() {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setPublicClient(false);
        client.setServiceAccountsEnabled(true);
        client.setRedirectUris(List.of("*"));
        client.setStandardFlowEnabled(true);
        client.setClientAuthenticatorType("federated-jwt");
        client.setEnabled(true);
        client.setAttributes(Map.of(
                "jwt.credential.issuer", IDP_ALIAS,
                "jwt.credential.sub", SPIFFE_ID));
        return client;
    }

    private static UserRepresentation createUser(String username, String... realmRoles) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setRealmRoles(List.of(realmRoles));
        user.setRequiredActions(List.of());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        return user;
    }
}
