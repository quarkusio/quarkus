package io.quarkus.hibernate.orm.validation;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class JPATestValidationOfNonEntitiesTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, JPATestValidationOfNonEntitiesResource.class)
                    .addAsResource("application.properties")
                    .addAsResource(new StringAsset(""), "import.sql")); // define an empty import.sql file

    @Test
    public void testValueValidation() {
        RestAssured.given().when().get("validation/nonentity/value").then()
                .body(is("ok"));
    }

    @Test
    public void testBeanValidation() {
        RestAssured.given().when().get("validation/nonentity/bean").then()
                .body(is("ok"));
    }
}
