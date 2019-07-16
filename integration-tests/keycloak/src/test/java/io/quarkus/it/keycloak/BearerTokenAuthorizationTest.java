package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hamcrest.Matchers;
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
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.util.JsonSerialization;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
public class BearerTokenAuthorizationTest {

    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180/auth");
    private static final String KEYCLOAK_REALM = "quarkus";

    @BeforeAll
    public static void configureKeycloakRealm() throws IOException {
        RealmRepresentation realm = createRealm(KEYCLOAK_REALM);

        realm.getClients().add(createClient("quarkus-app"));
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

    @AfterAll
    public static void removeKeycloakRealm() {
        RestAssured
                .given()
                .auth().oauth2(getAdminAccessToken())
                .when()
                .delete(KEYCLOAK_SERVER_URL + "/admin/realms/" + KEYCLOAK_REALM).then().statusCode(204);
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

        client.setAuthorizationServicesEnabled(true);

        ResourceServerRepresentation authorizationSettings = new ResourceServerRepresentation();

        authorizationSettings.setResources(new ArrayList<>());
        authorizationSettings.setPolicies(new ArrayList<>());

        configureConfidentialResourcePermission(authorizationSettings);
        configurePermissionResourcePermission(authorizationSettings);

        client.setAuthorizationSettings(authorizationSettings);

        return client;
    }

    private static void configureConfidentialResourcePermission(ResourceServerRepresentation authorizationSettings) {
        ResourceRepresentation resource = new ResourceRepresentation("Confidential Resource");

        resource.setUris(Collections.singleton("/api/confidential"));

        authorizationSettings.getResources().add(resource);

        PolicyRepresentation policy = new PolicyRepresentation();

        policy.setName("Confidential Policy");
        policy.setType("js");
        policy.setConfig(new HashMap<>());
        policy.getConfig().put("code",
                "var identity = $evaluation.context.identity;\n" +
                        "\n" +
                        "if (identity.hasRealmRole(\"confidential\")) {\n" +
                        "$evaluation.grant();\n" +
                        "}");

        authorizationSettings.getPolicies().add(policy);

        PolicyRepresentation permission = new PolicyRepresentation();

        permission.setName("Confidential Permission");
        permission.setType("resource");
        permission.setResources(new HashSet<>());
        permission.getResources().add(resource.getName());
        permission.setPolicies(new HashSet<>());
        permission.getPolicies().add(policy.getName());

        authorizationSettings.getPolicies().add(permission);
    }

    private static void configurePermissionResourcePermission(ResourceServerRepresentation authorizationSettings) {
        ResourceRepresentation resource = new ResourceRepresentation("Permission Resource");

        resource.setUris(Collections.singleton("/api/permission"));

        authorizationSettings.getResources().add(resource);

        PolicyRepresentation policy = new PolicyRepresentation();

        policy.setName("Permission Policy");
        policy.setType("js");
        policy.setConfig(new HashMap<>());
        policy.getConfig().put("code", "$evaluation.grant();");

        authorizationSettings.getPolicies().add(policy);

        PolicyRepresentation permission = new PolicyRepresentation();

        permission.setName("Permission Resource Permission");
        permission.setType("resource");
        permission.setResources(new HashSet<>());
        permission.getResources().add(resource.getName());
        permission.setPolicies(new HashSet<>());
        permission.getPolicies().add(policy.getName());

        authorizationSettings.getPolicies().add(permission);
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
    public void testSecureAccessSuccess() {
        for (String username : Arrays.asList("alice", "jdoe", "admin")) {
            RestAssured.given().auth().oauth2(getAccessToken(username))
                    .when().get("/api/users/me")
                    .then()
                    .statusCode(200)
                    .body("userName", equalTo(username));
        }
    }

    @Test
    public void testAccessAdminResource() {
        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api/admin")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("granted"));
    }

    @Test
    public void testPermissionClaimsInformationProvider() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/claims-cip")
                .then()
                .statusCode(200)
                .body("claims", everyItem(Matchers.hasKey("claim-a")));
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/claims-cip")
                .then()
                .statusCode(200)
                .body("claims", everyItem(Matchers.hasKey("global-claim")));
        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api/permission/claims-cip")
                .then()
                .statusCode(200)
                .body("claims", everyItem(Matchers.hasKey("global-claim")));
    }

    @Test
    public void testPermissionHttpInformationProvider() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/http-cip")
                .then()
                .statusCode(200)
                .body("claims", everyItem(Matchers.hasKey("user-name")));
    }

    @Test
    public void testDeniedAccessAdminResource() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/admin")
                .then()
                .statusCode(403);
    }

    @Test
    public void testDeniedAccessConfigEnforcingAccess() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/b")
                .then()
                .statusCode(403);
    }

    @Test
    public void testAllowAccessConfigDisablingEnforcer() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/a")
                .then()
                .statusCode(404);
    }

    @Test
    public void testAccessConfidentialResource() {
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/confidential")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("confidential"));
    }

    /**
     * This test make sure multi-tenancy is working so that applications can define their
     * {@link org.keycloak.adapters.KeycloakConfigResolver}
     * classes as regular CDI beans. See {@link TestConfigResolver}
     */
    @Test
    public void testAccessConfidentialResourceUsingTenantConfig() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().header("tenant", "tenant-1").get("/api/confidential")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("confidential"));
    }

    @Test
    public void testDeniedAccessConfidentialResource() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/confidential")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api/confidential")
                .then()
                .statusCode(403);
        testAccessConfidentialResource();
    }

    @Test
    public void testDeniedNoBearerToken() {
        RestAssured.given()
                .when().get("/api/users/me").then()
                .statusCode(403);
    }

    private String getAccessToken(String userName) {
        return RestAssured
                .given()
                .param("grant_type", "password")
                .param("username", userName)
                .param("password", userName)
                .param("client_id", "quarkus-app")
                .param("client_secret", "secret")
                .when()
                .post(KEYCLOAK_SERVER_URL + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class).getToken();
    }
}
