package io.quarkus.hibernate.orm.dev;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.TestTags;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Tag(TestTags.DEVMODE)
public class HibernateOrmDevControllerTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntityWithSuccessfulDDLGeneration.class,
                            HibernateOrmDevInfoServiceTestResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import-custom-table-name.sql", "import.sql"));

    @Test
    public void infoAvailable() {
        RestAssured.given()
                .param("expectedCreateDDLContent", "EXCEPTION")
                .param("expectedDropDDLContent", "EXCEPTION")
                .when().get("/dev-info/check-pu-info-with-successful-ddl-generation")
                .then().body(is("OK"));
    }

}
