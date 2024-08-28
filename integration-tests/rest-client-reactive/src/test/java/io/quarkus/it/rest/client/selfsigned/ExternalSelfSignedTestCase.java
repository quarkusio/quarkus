package io.quarkus.it.rest.client.selfsigned;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.client.wronghost.BadHostServiceTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = SelfSignedServiceTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = BadHostServiceTestResource.class, restrictToAnnotatedClass = true)
public class ExternalSelfSignedTestCase {

    @Test
    public void should_accept_self_signed_certs() {
        when()
                .get("/self-signed")
                .then()
                .statusCode(200)
                .body(is("200"));
    }
}
