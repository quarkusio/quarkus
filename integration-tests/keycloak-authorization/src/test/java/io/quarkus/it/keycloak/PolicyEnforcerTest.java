package io.quarkus.it.keycloak;

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
public class PolicyEnforcerTest {

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

        configurePermissionResourcePermission(authorizationSettings);
        configureClaimBasedPermission(authorizationSettings);
        configureHttpResponseClaimBasedPermission(authorizationSettings);

        client.setAuthorizationSettings(authorizationSettings);

        return client;
    }

    private static void configurePermissionResourcePermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policy = createJSPolicy("Confidential Policy", "var identity = $evaluation.context.identity;\n" +
                "\n" +
                "if (identity.hasRealmRole(\"confidential\")) {\n" +
                "$evaluation.grant();\n" +
                "}", settings);
        createPermission(settings, createResource(settings, "Permission Resource", "/api/permission"), policy);
    }

    private static void configureClaimBasedPermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policy = createJSPolicy("Claim-Based Policy", "var context = $evaluation.getContext();\n"
                + "var attributes = context.getAttributes();\n"
                + "\n"
                + "if (attributes.containsValue('grant', 'true')) {\n"
                + "    $evaluation.grant();\n"
                + "}", settings);
        createPermission(settings, createResource(settings, "Claim Protected Resource", "/api/permission/claim-protected"),
                policy);
    }

    private static void configureHttpResponseClaimBasedPermission(ResourceServerRepresentation settings) {
        PolicyRepresentation policy = createJSPolicy("Http Response Claim-Based Policy",
                "var context = $evaluation.getContext();\n"
                        + "var attributes = context.getAttributes();\n"
                        + "\n"
                        + "if (attributes.containsValue('user-name', 'alice')) {\n"
                        + "    $evaluation.grant();\n"
                        + "}",
                settings);
        createPermission(settings, createResource(settings, "Http Response Claim Protected Resource",
                "/api/permission/http-response-claim-protected"), policy);
    }

    private static void createPermission(ResourceServerRepresentation settings, ResourceRepresentation resource,
            PolicyRepresentation policy) {
        PolicyRepresentation permission = new PolicyRepresentation();

        permission.setName(resource.getName() + " Permission");
        permission.setType("resource");
        permission.setResources(new HashSet<>());
        permission.getResources().add(resource.getName());
        permission.setPolicies(new HashSet<>());
        permission.getPolicies().add(policy.getName());

        settings.getPolicies().add(permission);
    }

    private static ResourceRepresentation createResource(ResourceServerRepresentation authorizationSettings, String name,
            String uri) {
        ResourceRepresentation resource = new ResourceRepresentation(name);

        resource.setUris(Collections.singleton(uri));

        authorizationSettings.getResources().add(resource);
        return resource;
    }

    private static PolicyRepresentation createJSPolicy(String name, String code, ResourceServerRepresentation settings) {
        PolicyRepresentation policy = new PolicyRepresentation();

        policy.setName(name);
        policy.setType("js");
        policy.setConfig(new HashMap<>());
        policy.getConfig().put("code", code);

        settings.getPolicies().add(policy);

        return policy;
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
    public void testUserHasRoleConfidential() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/permission")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Permission Resource"));
        ;
        RestAssured.given().auth().oauth2(getAccessToken("admin"))
                .when().get("/api/permission")
                .then()
                .statusCode(403);
    }

    @Test
    public void testRequestParameterAsClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/claim-protected?grant=true")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Claim Protected Resource"));
        ;
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/claim-protected?grant=false")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/claim-protected")
                .then()
                .statusCode(403);
    }

    @Test
    public void testHttpResponseFromExternalServiceAsClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
                .when().get("/api/permission/http-response-claim-protected")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Http Response Claim Protected Resource"));
        RestAssured.given().auth().oauth2(getAccessToken("jdoe"))
                .when().get("/api/permission/http-response-claim-protected")
                .then()
                .statusCode(403);
    }

    @Test
    public void testPublicResource() {
        RestAssured.given()
                .when().get("/api/public")
                .then()
                .statusCode(204);
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
