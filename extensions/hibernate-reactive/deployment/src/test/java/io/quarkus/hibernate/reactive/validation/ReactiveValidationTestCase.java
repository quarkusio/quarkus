package io.quarkus.hibernate.reactive.validation;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ReactiveValidationTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, ReactiveTestValidationResource.class)
                    .addAsResource("application.properties")
                    .addAsResource(new StringAsset(""), "import.sql")); // define an empty import.sql file

    @Test
    public void testValidEntity() {
        String entityName = "valid";
        RestAssured.given().body(entityName).when().post("/validation").then()
                .body(is("OK"));
    }

    @Test
    public void testInvalidEntityWithLongName() {
        RestAssured.given().body(
                "Post method should not persist an entity having a Size constraint of 50 on the name column.")
                .when().post("/validation").then()
                .body(containsString(MyEntity.ENTITY_NAME_TOO_LONG));
    }

    @Test
    public void testInvalidEntityWithEmptyName() {
        RestAssured.given().body("").when().post("/validation").then()
                .body(containsString(MyEntity.ENTITY_NAME_CANNOT_BE_EMPTY));
    }
}
