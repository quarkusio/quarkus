package io.quarkus.hibernate.reactive.validation;

import static org.hamcrest.Matchers.containsString;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ReactiveValidationTraversableResolverTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyLazyEntity.class, MyLazyChildEntity.class,
                            ReactiveTestValidationTraversableResolverResource.class)
                    .addAsResource("application.properties")
                    .addAsResource(new StringAsset(""), "import.sql")); // define an empty import.sql file

    @Test
    public void testPass() {
        Long id = Long.parseLong(RestAssured.post("/validation")
                .body()
                .asString());
        RestAssured.get("/validation/{id}", id)
                .then()
                .statusCode(200)
                .body(containsString("OK"));
    }

    @Test
    public void testFail() {
        Long id = Long.parseLong(RestAssured.post("/validation")
                .body()
                .asString());
        RestAssured.get("/validation/fail/{id}", id)
                .then()
                .statusCode(200)
                .body(containsString("OK"));
    }

}
