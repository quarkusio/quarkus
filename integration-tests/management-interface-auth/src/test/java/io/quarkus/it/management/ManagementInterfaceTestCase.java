package io.quarkus.it.management;

import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

@QuarkusTest
public class ManagementInterfaceTestCase {

    protected String getPrefix() {
        return "http://localhost:9001";
    }

    @Test
    void verifyThatHealthChecksAreExposedOnManagementInterface() {
        RestAssured.given().auth().preemptive().basic("john", "john").get(getPrefix() + "/q/health")
                .then().statusCode(401);

        RestAssured.given().auth().preemptive().basic("bob", "bob").get(getPrefix() + "/q/health")
                .then().statusCode(403);

        RestAssured.given().auth().basic("alice", "alice").get(getPrefix() + "/q/health")
                .then().statusCode(200)
                .body(Matchers.containsString("UP"));

        RestAssured.given().auth().basic("alice", "alice").get(getPrefix() + "/q/health/ready")
                .then().statusCode(200)
                .body(Matchers.containsString("UP"));

        RestAssured.given().auth().oauth2(getAdminToken()).get(getPrefix() + "/q/health")
                .then().statusCode(401);
        RestAssured.given().auth().oauth2(getUserToken()).get(getPrefix() + "/q/health")
                .then().statusCode(401);

        RestAssured.get("/q/health")
                .then().statusCode(404);
    }

    @Test
    void verifyThatMetricsAreExposedOnManagementInterface() {
        RestAssured.given().auth().basic("alice", "alice").get(getPrefix() + "/q/metrics")
                .then().statusCode(200)
                .body(Matchers.containsString("http_server_bytes_read"));
        RestAssured.given().auth().basic("bob", "bob").get(getPrefix() + "/q/metrics")
                .then().statusCode(200)
                .body(Matchers.containsString("http_server_bytes_read"));
        RestAssured.given().auth().basic("john", "john").get(getPrefix() + "/q/metrics")
                .then().statusCode(401);

        RestAssured.given().auth().oauth2(getAdminToken()).get(getPrefix() + "/q/metrics")
                .then().statusCode(200);
        RestAssured.given().auth().oauth2(getUserToken()).get(getPrefix() + "/q/metrics")
                .then().statusCode(200);
        RestAssured.given().auth().oauth2("wrongtoken").get(getPrefix() + "/q/metrics")
                .then().statusCode(401);

        RestAssured.get("/q/metrics")
                .then().statusCode(404);
    }

    @Test
    void verifyMainEndpointBasicAuth() {
        RestAssured.get("/service/hello").then().statusCode(200)
                .body(Matchers.equalTo("hello"));

        RestAssured.given().auth().preemptive().basic("john", "john").get("/service/goodbye")
                .then().statusCode(401);

        RestAssured.given().auth().preemptive().basic("alice", "alice").get("/service/goodbye")
                .then().statusCode(403);

        RestAssured.given().auth().basic("bob", "bob").get("/service/goodbye")
                .then().statusCode(200)
                .body(Matchers.equalTo("goodbye"));

        RestAssured.given().auth().basic("bob", "bob").get("/service/goodafternoon")
                .then().statusCode(200)
                .body(Matchers.equalTo("goodafternoon"));

        RestAssured.given().auth().oauth2(getAdminToken()).get("/service/goodbye")
                .then().statusCode(401);
        RestAssured.given().auth().oauth2(getUserToken()).get("/service/goodbye")
                .then().statusCode(401);
        RestAssured.given().auth().oauth2(getUserToken()).get("/service/goodafternoon")
                .then().statusCode(401);

    }

    @Test
    void verifyMainEndpointJwtAuth() {
        RestAssured.get("/service/hello").then().statusCode(200)
                .body(Matchers.equalTo("hello"));

        RestAssured.given().auth().preemptive().basic("john", "john").get("/service/goodmorning")
                .then().statusCode(401);
        RestAssured.given().auth().preemptive().basic("john", "john").get("/service/goodevening")
                .then().statusCode(401);
        RestAssured.given().auth().preemptive().basic("john", "john").get("/service/goodforenoon")
                .then().statusCode(401);

        RestAssured.given().auth().preemptive().basic("alice", "alice").get("/service/goodmorning")
                .then().statusCode(401);
        RestAssured.given().auth().preemptive().basic("alice", "alice").get("/service/goodevening")
                .then().statusCode(401);

        RestAssured.given().auth().basic("bob", "bob").get("/service/goodmorning")
                .then().statusCode(401);
        RestAssured.given().auth().basic("bob", "bob").get("/service/goodevening")
                .then().statusCode(401);

        RestAssured.given().auth().oauth2(getAdminToken()).get("/service/goodmorning")
                .then().statusCode(200)
                .body(Matchers.equalTo("goodmorning"));
        RestAssured.given().auth().oauth2(getAdminToken()).get("/service/goodforenoon")
                .then().statusCode(200)
                .body(Matchers.equalTo("goodforenoon"));
        RestAssured.given().auth().oauth2(getUserToken()).get("/service/goodmorning")
                .then().statusCode(403);

        RestAssured.given().auth().oauth2(getAdminToken()).get("/service/goodevening")
                .then().statusCode(200)
                .body(Matchers.equalTo("goodevening"));
        RestAssured.given().auth().oauth2(getUserToken()).get("/service/goodevening")
                .then().statusCode(200)
                .body(Matchers.equalTo("goodevening"));

    }

    private String getAdminToken() {
        return Jwt.upn("alice").groups(Set.of("admin", "user")).sign();
    }

    private String getUserToken() {
        return Jwt.subject("bob").groups("user").sign();
    }

}
