package io.quarkus.it.elytron;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class X500PrincipalUtilTestCase {

    @Test
    public void testVerifyPrincipal() {
        RestAssured.when()
                .get("/x500/verifyInjectedPrincipal")
                .then()
                .statusCode(200)
                .body(is("O=Fake X500Principal"));
    }

}
