package io.quarkus.reactive.mssql.client;

import static io.restassured.RestAssured.given;

import org.hamcrest.CoreMatchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CredentialsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(CustomCredentialsProvider.class)
                    .addClass(CredentialsTestResource.class)
                    .addAsResource("application-credentials.properties", "application.properties"));

    @Test
    public void testConnect() {
        given()
                .when().get("/test")
                .then()
                .statusCode(200)
                .body(CoreMatchers.equalTo("OK"));
    }

}
