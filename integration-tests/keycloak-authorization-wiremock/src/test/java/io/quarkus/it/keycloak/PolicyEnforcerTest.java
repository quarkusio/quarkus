package io.quarkus.it.keycloak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.jwt.build.Jwt;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.Permission;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@QuarkusTest
public class PolicyEnforcerTest {

    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180/auth");
    private static final String KEYCLOAK_REALM = "quarkus";

    @BeforeAll
    public static void configureKeycloakRealm() throws IOException {
    }

    @AfterAll
    public static void removeKeycloakRealm() {
    }

    @Test
    public void testUserHasRoleConfidentialTenant() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", new HashSet<>(Arrays.asList("user")), "dummy", "Permission Resource Tenant"))
                .when().get("/api-permission-tenant")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe", new HashSet<>(Arrays.asList("user")), "dummy","Permission Resource Tenant"))
                .when().get("/api-permission-tenant")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("admin", new HashSet<>(Arrays.asList("admin", "user")), "KX3A-39WE", "Permission Resource Tenant"))
                .when().get("/api-permission-tenant")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Permission Resource Tenant"));
        ;
    }

    @Test
    public void testUserHasRoleConfidential() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", new HashSet<>(Arrays.asList("user")), "dummy", "Permission Resource"))
                .when().get("/api/permission")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe", new HashSet<>(Arrays.asList("user")), "KX3A-39WE","Permission Resource"))
                .when().get("/api/permission")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Permission Resource"));
        RestAssured.given().auth().oauth2(getAccessToken("jdoe", new HashSet<>(Arrays.asList("user")), "KX3A-39WE", "Scope Permission Resource"))
                .when().get("/api/permission/scope?scope=write")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("jdoe", new HashSet<>(Arrays.asList("user")), "KX3A-39WE","Scope Permission Resource", new HashSet<>(Arrays.asList("read"))))
                .when().get("/api/permission/scope?scope=read")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("read"));
        RestAssured.given().auth().oauth2(getAccessToken("admin", new HashSet<>(Arrays.asList("admin", "user")), "dummy", "Permission Resource"))
                .when().get("/api/permission")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("admin", new HashSet<>(Arrays.asList("admin", "user")), "KX3A-39WE","Scope Permission Resource"))
                .when().get("/api/permission/entitlements")
                .then()
                .statusCode(200);
    }

    @Test
    public void testRequestParameterAsClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", new HashSet<>(Arrays.asList("user")), "KX3A-39WE", "Claim Protected Resource"))
                .when().get("/api/permission/claim-protected?grant=true")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Claim Protected Resource"));
        RestAssured.given().auth().oauth2(getAccessToken("alice", new HashSet<>(Arrays.asList("user")), "KX3A-39WE", "Claim Protected Resource"))
                .when().get("/api/permission/claim-protected?grant=false")
                .then()
                .statusCode(403);
        RestAssured.given().auth().oauth2(getAccessToken("alice", new HashSet<>(Arrays.asList("user")), "KX3A-39WE", "Claim Protected Resource"))
                .when().get("/api/permission/claim-protected")
                .then()
                .statusCode(403);
    }

    @Test
    public void testHttpResponseFromExternalServiceAsClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", new HashSet<>(Arrays.asList("user")), "KX3A-39WE", "Http Response Claim Protected Resource"))
                .when().get("/api/permission/http-response-claim-protected")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Http Response Claim Protected Resource"));
        RestAssured.given().auth().oauth2(getAccessToken("alice", new HashSet<>(Arrays.asList("user")), "KX3A-39WE", "Http Response Claim Protected Resource"))
                .when().get("/api/permission/http-response-claim-protected")
                .then()
                .statusCode(403);
    }

    @Test
    public void testBodyClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", new HashSet<>(Arrays.asList("user")), "KX3A-39WE", "Body Claim Protected Resource"))
                .contentType(ContentType.JSON)
                .body("{\"from-body\": \"grant\"}")
                .when()
                .post("/api/permission/body-claim")
                .then()
                .statusCode(200)
                .and().body(Matchers.containsString("Body Claim Protected Resource"));
    }

    @Test
    public void testPublicResource() {
        RestAssured.given()
                .when().get("/api/public")
                .then()
                .statusCode(204);
    }

    @Test
    public void testPublicResourceWithEnforcingPolicy() {
        RestAssured.given()
                .when().get("/api/public-enforcing")
                .then()
                .statusCode(401);
    }

    @Test
    public void testPublicResourceWithEnforcingPolicyAndToken() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", new HashSet<>(Arrays.asList("user")), "dummy", "Claim Protected Resource"))
                .when().get("/api/public-enforcing")
                .then()
                .statusCode(403);
    }

    @Test
    public void testPublicResourceWithDisabledPolicyAndToken() {
        RestAssured.given().auth().oauth2(getAccessToken("alice", new HashSet<>(Arrays.asList("user")), "KX3A-39WE", "Claim Protected Resource"))
                .when().get("/api/public-token")
                .then()
                .statusCode(204);
    }

    @Test
    public void testPathConfigurationPrecedenceWhenPathCacheNotDefined() {
        RestAssured.given()
                .when().get("/api2/resource")
                .then()
                .statusCode(401);

        RestAssured.given()
                .when().get("/hello")
                .then()
                .statusCode(404);

        RestAssured.given()
                .when().get("/")
                .then()
                .statusCode(404);
    }

    private String getAccessToken(String userName, Set<String> groups, String rsid, String rsname, Set<String> scopes) {

        JsonObjectBuilder permission = Json.createObjectBuilder().add("rsid", rsid).add("rsname", rsname).add("scopes", Json.createArrayBuilder(scopes));
        JsonObject permissions = Json.createObjectBuilder().add("permissions", Json.createArrayBuilder().add(permission)).build();

        String jwt = Jwt.preferredUserName(userName)
                .claim("authorization", permissions)
                .groups(groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .sign();

        return jwt;
    }

    private String getAccessToken(String userName, Set<String> groups, String rsid, String rsname) {
        return getAccessToken(userName, groups, rsid, rsname, Collections.emptySet());
    }
}
