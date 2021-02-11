package io.quarkus.it.rest.client.selfsigned;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ExternalSelfSignedTestCase {

    @Test
    public void should_accept_self_signed_certs() {
        when()
                .get("/self-signed")
                .then()
                .statusCode(200)
                .body(is("200"));
    }

    @Test
    public void should_accept_self_signed_certs_java_url() {
        when()
                .get("/self-signed/java")
                .then()
                .statusCode(200)
                .body(is(not(empty())));
    }
}
