package io.quarkus.it.keycloak;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.quarkus.test.keycloak.client.KeycloakTestClient.Tls;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

public class KeycloakXTestResourceLifecycleManager
        implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private static final String KEYCLOAK_REALM = "quarkus";
    private static final String KEYCLOAK_SERVICE_CLIENT = "quarkus-app";

    final KeycloakTestClient client = new KeycloakTestClient(
            new Tls("target/certificates/oidc-client-keystore.p12",
                    "target/certificates/oidc-client-truststore.p12"));

    @Override
    public Map<String, String> start() {

        RealmRepresentation realm = createRealm(KEYCLOAK_REALM);
        client.createRealm(realm);
        configureCimdClientPolicy(KEYCLOAK_REALM);

        return Map.of();
    }

    private static RealmRepresentation createRealm(String name) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setAccessTokenLifespan(3);
        realm.setSsoSessionMaxLifespan(3);

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("confidential", null, false));

        realm.getClients().add(createServiceClient(KEYCLOAK_SERVICE_CLIENT));

        realm.getUsers().add(createUser("alice", List.of("user")));
        realm.getUsers().add(createUser("admin", List.of("user", "admin")));
        realm.getUsers().add(createUser("jdoe", List.of("user", "confidential")));

        return realm;
    }

    private static ClientRepresentation createServiceClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setEnabled(true);

        return client;
    }

    private static UserRepresentation createUser(String username, List<String> realmRoles) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(realmRoles);
        user.setEmail(username + "@gmail.com");

        CredentialRepresentation credential = new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        credential.setTemporary(false);

        user.getCredentials().add(credential);

        return user;
    }

    private void configureCimdClientPolicy(String realmName) {
        String adminToken = client.getAdminAccessToken();
        String baseUrl = client.getAuthServerBaseUrl();

        String profiles = """
                {
                  "profiles": [{
                    "name": "cimd-profile",
                    "executors": [{
                      "executor": "client-id-metadata-document",
                      "configuration": {
                        "cimd-allow-permitted-domains": ["host.testcontainers.internal"]
                      }
                    }]
                  }]
                }
                """;

        keycloakRequest()
                .auth().oauth2(adminToken)
                .contentType("application/json")
                .body(profiles)
                .when()
                .put(baseUrl + "/admin/realms/" + realmName + "/client-policies/profiles")
                .then()
                .statusCode(204);

        String policies = """
                {
                  "policies": [{
                    "name": "cimd-policy",
                    "enabled": true,
                    "conditions": [{
                      "condition": "client-id-uri",
                      "configuration": {
                        "client-id-uri-scheme": ["https"],
                        "client-id-uri-allow-permitted-domains": ["host.testcontainers.internal"]
                      }
                    }],
                    "profiles": ["cimd-profile"]
                  }]
                }
                """;

        keycloakRequest()
                .auth().oauth2(adminToken)
                .contentType("application/json")
                .body(policies)
                .when()
                .put(baseUrl + "/admin/realms/" + realmName + "/client-policies/policies")
                .then()
                .statusCode(204);
    }

    private static RequestSpecification keycloakRequest() {
        return RestAssured.given()
                .keyStore("target/certificates/oidc-client-keystore.p12", "password")
                .trustStore("target/certificates/oidc-client-truststore.p12", "password");
    }

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        client.setIntegrationTestContext(context);
    }

    @Override
    public void stop() {
    }

}
