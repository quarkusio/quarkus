package io.quarkus.it.rest.client.selfsigned;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = SelfSignedServiceTestResource.class, restrictToAnnotatedClass = true)
public class ExternalSelfSignedTestCase {

    @Test
    public void should_accept_self_signed_certs() {
        when()
                .get("/self-signed/ExternalSelfSignedClient")
                .then()
                .statusCode(200)
                .body(is("Hello self-signed!"));
    }

    @Test
    public void javaxNetSsl() {
        given()
                .get("/self-signed/HttpClient/javax.net.ssl")
                .then()
                .statusCode(200)
                .body(is("Hello self-signed!"));
    }

    @Test
    public void fakeHost() {
        given()
                .get("/self-signed/HttpClient/fake-host")
                .then()
                .statusCode(500)
                .body(containsStringIgnoringCase("unable to find valid certification path"));
    }

}
