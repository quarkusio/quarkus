package io.quarkus.it.rest.client.wronghost;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.client.selfsigned.SelfSignedServiceTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithTestResource(SelfSignedServiceTestResource.class)
@WithTestResource(BadHostServiceTestResource.class)
public class ExternalWrongHostTestCase {
    @Test
    public void restClient() {
        when()
                .get("/wrong-host")
                .then()
                .statusCode(200)
                .body(is("200"));
    }

    @Test
    public void restClientRejected() {
        when()
                .get("/wrong-host-rejected")
                .then()
                .statusCode(500)
                .body(containsString("SSLHandshakeException"));
    }
}
