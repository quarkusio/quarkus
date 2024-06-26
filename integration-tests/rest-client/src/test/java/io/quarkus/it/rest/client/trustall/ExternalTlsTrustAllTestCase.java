package io.quarkus.it.rest.client.trustall;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.it.rest.client.wronghost.ExternalWrongHostTestResourceUsingHostnameVerifier;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@WithTestResource(value = ExternalTlsTrustAllTestResource.class, restrictToAnnotatedClass = false)
@WithTestResource(ExternalWrongHostTestResourceUsingHostnameVerifier.class)
public class ExternalTlsTrustAllTestCase {

    @Test
    public void restClient() {
        when()
                .get("/wrong-host")
                .then()
                .statusCode(200)
                .body(is("200"));
    }
}
