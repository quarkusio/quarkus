package io.quarkus.it.smallrye.graphql.keycloak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class KeycloakRealmResourceManager implements QuarkusTestResourceLifecycleManager {

    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180");
    //private static String KEYCLOAK_SERVER_URL;
    private static final String KEYCLOAK_REALM = "quarkus";
    //private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:22.0.5";

    private GenericContainer<?> keycloak;

    @Override
    public Map<String, String> start() {
        RealmRepresentation realm = createRealm(KEYCLOAK_REALM);
        realm.setRevokeRefreshToken(true);
        realm.setRefreshTokenMaxReuse(0);
        realm.setAccessTokenLifespan(3);
        realm.setRequiredActions(List.of());

        realm.getClients().add(createClient("quarkus-app"));
        realm.getUsers().add(createUser("alice", "user"));

        try {
            Response response = RestAssured
                    .given()
                    .auth().oauth2(getAdminAccessToken())
                    .contentType("application/json")
                    .body(JsonSerialization.writeValueAsBytes(realm))
                    .when()
                    .post(KEYCLOAK_SERVER_URL + "/admin/realms");
            response.then()
                    .statusCode(201);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Map<String, String> properties = new HashMap<>();

        properties.put("quarkus.oidc.auth-server-url", KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM);
        properties.put("keycloak.url", KEYCLOAK_SERVER_URL);

        return properties;
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
        realm.setRequiredActions(List.of());
        realm.setUsers(new ArrayList<>());
        realm.setClients(new ArrayList<>());
        realm.setAccessTokenLifespan(3);
        realm.setSsoSessionMaxLifespan(3);
        realm.setRequiredActions(List.of());

        RolesRepresentation roles = new RolesRepresentation();
        List<RoleRepresentation> realmRoles = new ArrayList<>();

        roles.setRealm(realmRoles);
        realm.setRoles(roles);

        realm.getRoles().getRealm().add(new RoleRepresentation("user", null, false));
        realm.getRoles().getRealm().add(new RoleRepresentation("admin", null, false));

        return realm;
    }

    private static ClientRepresentation createClient(String clientId) {
        ClientRepresentation client = new ClientRepresentation();

        client.setClientId(clientId);
        client.setPublicClient(false);
        client.setSecret("secret");
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setRedirectUris(Arrays.asList("*"));
        client.setEnabled(true);
        client.setDefaultClientScopes(List.of("microprofile-jwt"));

        return client;
    }

    private static UserRepresentation createUser(String username, String... realmRoles) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(username);
        user.setEnabled(true);
        user.setRealmRoles(Arrays.asList(realmRoles));
        user.setEmail(username + "@gmail.com");
        user.setRequiredActions(List.of());
        user.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();

        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(username);
        credential.setTemporary(false);

        user.setCredentials(List.of(credential));

        return user;
    }

    @Override
    public void stop() {
        RestAssured
                .given()
                .auth().oauth2(getAdminAccessToken())
                .when()
                .delete(KEYCLOAK_SERVER_URL + "/admin/realms/" + KEYCLOAK_REALM).then().statusCode(204);
    }

    public static String getAccessToken() {
        io.restassured.response.Response response = RestAssured.given()
                .contentType("application/x-www-form-urlencoded")
                .accept("application/json")
                .formParam("username", "alice")
                .formParam("password", "alice")
                .param("client_id", "quarkus-app")
                .param("client_secret", "secret")
                .formParam("grant_type", "password")
                .post(KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token");
        return response.getBody().jsonPath().getString("access_token");
    }
}
