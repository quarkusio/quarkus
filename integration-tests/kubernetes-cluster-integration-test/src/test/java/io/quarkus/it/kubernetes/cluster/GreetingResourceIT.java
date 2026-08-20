package io.quarkus.it.kubernetes.cluster;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

@QuarkusIntegrationTest
public class GreetingResourceIT {

    @BeforeAll
    public static void relaxHttpsValidation() {
        // The OpenShift route (quarkus.openshift.route.tls.termination=edge) is served over https using
        // whatever certificate the router presents - CRC's is self-signed and untrusted by a plain JVM
        // truststore, unlike a managed cluster's (e.g. the Red Hat Developer Sandbox) properly CA-signed one.
        // Relaxed here, scoped to this one e2e verification test - not something the launcher does globally.
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("hello"));
    }
}
