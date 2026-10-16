package io.quarkus.it.keycloak;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.client.KeycloakTestClient;

public class SpiffeKeycloakTestResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private static final String REALM = "quarkus-spiffe";
    private static final String CLIENT_ID = "quarkus-app-spiffe";
    private static final String IDP_ALIAS = "spiffe-idp";
    private static final String TRUST_DOMAIN = "spiffe://test.quarkus.io";
    private static final String SPIFFE_ID = TRUST_DOMAIN + "/test-workload";

    private final KeycloakTestClient client = new KeycloakTestClient();

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        client.setIntegrationTestContext(context);
    }

    @Override
    public Map<String, String> start() {
        String bundleEndpoint = "http://host.testcontainers.internal:18443/bundle";

        RealmRepresentation realm = createRealm();
        realm.addIdentityProvider(createSpiffeIdp(bundleEndpoint));
        realm.getClients().add(createSpiffeClient());
        realm.getUsers().add(createUser("alice"));
        client.createRealm(realm);

        return Map.of();
    }

    @Override
    public void stop() {
    }

    private static RealmRepresentation createRealm() {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setRequiredActions(List.of());

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

    private static UserRepresentation createUser(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEnabled(true);
        user.setRequiredActions(List.of());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        return user;
    }
}
