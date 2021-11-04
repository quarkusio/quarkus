package io.quarkus.hibernate.orm.envers;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversValidationTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyAuditedEntity.class, MyRevisionEntity.class, MyRevisionListener.class,
                            EnversTestValidationResource.class)
                    .addAsResource("application.properties")
                    .addAsResource(new StringAsset(""), "import.sql")); // define an empty import.sql file

    @Test
    public void testInsert() {
        String entityName = "audited";
        RestAssured.given().body(entityName).when().post("/envers").then()
                .body(is("OK"));
    }

}
