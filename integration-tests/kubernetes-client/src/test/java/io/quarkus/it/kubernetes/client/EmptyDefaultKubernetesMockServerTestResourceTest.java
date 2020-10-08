package io.quarkus.it.kubernetes.client;

import static org.hamcrest.Matchers.is;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

@QuarkusTest
@TestProfile(EmptyDefaultKubernetesMockServerTestResourceTest.MyProfile.class)
@QuarkusTestResource(CustomKubernetesMockServerTestResource.class)
public class EmptyDefaultKubernetesMockServerTestResourceTest {

    //test that we get empty lists back from the mocked kubernetes server
    @Test
    public void testEmptyConfigmaps() {
        RestAssured.when().get("/empty/test/configmaps").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    public void testEmptyDeployments() {
        RestAssured.when().get("/empty/test/deployments").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    public void testEmptyEvents() {
        RestAssured.when().get("/empty/test/events").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    public void testEmptyIngresses() {
        RestAssured.when().get("/empty/test/ingresses").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    public void testEmptyPods() {
        RestAssured.when().get("/empty/test/pods").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    public void testEmptySecrets() {
        RestAssured.when().get("/empty/test/secrets").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    public void testEmptyServices() {
        RestAssured.when().get("/empty/test/services").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    @Test
    public void testEmptyServiceAccounts() {
        RestAssured.when().get("/empty/test/serviceaccounts").then()
                .statusCode(200)
                .body("size()", is(0));
    }

    public static class MyProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> conf = new HashMap<>();
            conf.put("quarkus.kubernetes-client.test.default-types", "true");
            return conf;
        }
    }

}
