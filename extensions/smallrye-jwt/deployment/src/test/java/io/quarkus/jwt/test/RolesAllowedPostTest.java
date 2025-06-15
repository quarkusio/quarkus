package io.quarkus.jwt.test;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

public class RolesAllowedPostTest {
    private static Class<?>[] testClasses = { RolesEndpoint.class, User.class };

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.addClasses(testClasses).addAsResource("publicKey.pem").addAsResource("privateKey.pem")
                    .addAsResource("applicationRolesAllowedPost.properties", "application.properties"));

    @Test
    public void postText() {
        RestAssured.given().auth().oauth2(Jwt.upn("alice").groups("Tester").sign()).when().body("principal:")
                .post("/endp/postInjectedPrincipal").then().statusCode(200).body(equalTo("principal:alice"));
    }

    @Test
    public void postTextInvalidToken() {
        RestAssured.given().auth().oauth2("invalid").when().body("principal:").post("/endp/postInjectedPrincipal")
                .then().statusCode(401);
    }

    @Test
    public void postTextWrongRole() {
        RestAssured.given().auth().oauth2(Jwt.upn("alice").groups("User").sign()).when().body("principal:")
                .post("/endp/postInjectedPrincipal").then().statusCode(403);
    }

    @Test
    public void postJson() {
        RestAssured.given().header("Content-Type", "application/json").auth()
                .oauth2(Jwt.upn("alice").groups("Tester").sign()).when().body("{\"name\":\"alice\"}")
                .post("/endp/postInjectedPrincipalJson").then().statusCode(200)
                .body(equalTo("name:alice,principal:alice"));
    }

    @Test
    public void postJsonInvalidToken() {
        RestAssured.given().header("Content-Type", "application/json").auth().oauth2("invalid").when()
                .body("{\"name\":\"alice\"}").post("/endp/postInjectedPrincipalJson").then().statusCode(401);
    }

    @Test
    public void postJsonNoToken() {
        RestAssured.given().header("Content-Type", "application/json").when().body("{\"name\":\"alice\"}")
                .post("/endp/postInjectedPrincipalJson").then().statusCode(401);
    }

    @Test
    public void postJsonWrongRole() {
        RestAssured.given().header("Content-Type", "application/json").auth()
                .oauth2(Jwt.upn("alice").groups("User").sign()).when().body("{\"name\":\"alice\"}")
                .post("/endp/postInjectedPrincipalJson").then().statusCode(403);
    }
}
