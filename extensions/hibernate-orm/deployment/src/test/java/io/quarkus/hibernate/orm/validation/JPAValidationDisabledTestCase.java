package io.quarkus.hibernate.orm.validation;

import static org.hamcrest.Matchers.is;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class JPAValidationDisabledTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MyEntity.class, JPATestValidationResource.class)
                    .addAsResource("application-validation-disabled.properties", "application.properties"));

    @Test
    @Transactional
    public void testValidEntity() {
        String entityName = "Post method should not persist an entity having a Size constraint of 50 on the name column if validation was enabled.";
        RestAssured.given().body(entityName).when().post("/validation").then().body(is("OK"));
    }

}
