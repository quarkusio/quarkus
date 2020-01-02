package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
public class BearerTokenAuthorizationTest {

    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180/auth");
    private static final String KEYCLOAK_REALM = "quarkus-";

    @BeforeAll
    public static void configureKeycloakRealm() throws IOException {
        for (String realmId : Arrays.asList("a", "b", "c", "d")) {
            RealmRepresentation realm = createRealm(KEYCLOAK_REALM + realmId);

            realm.getClients().add(createClient("quarkus-app-" + realmId));
            realm.getUsers().add(createUser("alice", "user"));
            realm.getUsers().add(createUser("admin", "user", "admin"));
            realm.getUsers().add(createUser("jdoe", "user", "confidential"));

            RestAssured
                    .given()
                    .auth().oauth2(getAdminAccessToken())
                    .contentType("application/json")
                    .body(JsonSerialization.writeValueAsBytes(realm))
                    .when()
                    .post(KEYCLOAK_SERVER_URL + "/admin/realms").then()
                    .statusCode(201);
        }
    }

    @AfterAll
    public static void removeKeycloakRealm() {
        for (String realmId : Arrays.asList("a", "b", "c", "d")) {
            RestAssured
                    .given()
                    .auth().oauth2(getAdminAccessToken())
                    .when()
                    .delete(KEYCLOAK_SERVER_URL + "/admin/realms/" + KEYCLOAK_REALM + realmId).then().statusCode(204);
        }
    }

    private static String getAdminAccessToken() {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", "admin")
                .param("password", "admin")
                .param("client_id", "admin-cli")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/master/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }

    private static RealmRepresentation createRealm(String name) {
        RealmRepresentation realm = new RealmRepresentation();

        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("confidential", null, false));

        return realm;
    }

    private static ClientRepresentation createClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setDirectAccessGrantsEnabled(true);
        client.setEnabled(true);
        client.setDefaultRoles(new String[] { "role-" + clientId });

        return client;
    }

    private static UserRepresentation createUser(String username, String... realmRoles) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEnabled(true);
        user.setCredentials(new ArrayList<>());
        user.setRealmRoles(Arrays.asList(realmRoles));

        CredentialRepresentation credential = new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        credential.setTemporary(false);

        user.getCredentials().add(credential);

        return user;
    }

    @Test
    public void testResolveTenantIdentifier() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-b/api/user")
                .then()
                .statusCode(200)
                .body("preferred_username", equalTo("alice"));

        // should give a 403 given that access token from issuer b can not access tenant c
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-c/api/user")
                .then()
                .statusCode(403);
    }

    @Test
    public void testResolveTenantConfig() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", "d"))
                .when().get("/tenant/tenant-d/api/user")
                .then()
                .statusCode(200)
                .body("preferred_username", equalTo("alice"));

        // should give a 403 given that access token from issuer b can not access tenant c
        RestAssured.given().auth().oauth2(getAccessToken("alice", "b"))
                .when().get("/tenant/tenant-d/api/user")
                .then()
                .statusCode(403);
    }

    @Test
    public void testDefaultTenant() {
        // any non-extent tenant should accept tokens from tenant a
        RestAssured.given().auth().oauth2(getAccessToken("alice", "a"))
                .when().get("/tenant/tenant-any/api/user")
                .then()
                .statusCode(200)
                .body("preferred_username", equalTo("alice"));
    }

    private String getAccessToken(String userName, String clientId) {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", "quarkus-app-" + clientId)
                .param("client_secret", "secret")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM + clientId + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }
}
