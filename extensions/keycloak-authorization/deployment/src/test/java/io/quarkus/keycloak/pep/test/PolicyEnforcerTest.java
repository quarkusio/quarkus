package io.quarkus.keycloak.pep.test;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.keycloak.representations.AccessTokenResponse;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
//to start docker manually
//docker run -p 8180:8080  --rm -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin     -e JAVA_OPTS="-Xms64m -Xmx512m -XX:MetaspaceSize=96M -XX:MaxMetaspaceSize=256m   -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -Dkeycloak.profile.feature.upload_scripts=enabled"  quay.io/keycloak/keycloak:10.0.0
@QuarkusTestResource(KeycloakTestResource.class)
public class PolicyEnforcerTest {

    private static final String KEYCLOAK_SERVER_URL = System.getProperty("keycloak.url", "http://localhost:8180/auth");
    private static final String KEYCLOAK_REALM = "quarkus";

    @RegisterExtension
    static QuarkusDevModeTest devModeTest = new QuarkusDevModeTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addAsResource("application.properties")
                            .addClasses(ProtectedResource.class, ProtectedResource2.class, PublicResource.class,
                                    UsersResource.class);
                }
            });

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
    public void testBodyClaim() {
        RestAssured.given().auth().oauth2(getAccessToken("alice"))
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
    public void testHealthCheck() {
        RestAssured.given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200);
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
