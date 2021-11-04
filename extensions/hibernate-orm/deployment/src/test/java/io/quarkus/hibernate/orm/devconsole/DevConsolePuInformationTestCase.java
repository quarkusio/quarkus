package io.quarkus.hibernate.orm.devconsole;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DevConsolePuInformationTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntityWithSuccessfulDDLGeneration.class,
                            DevConsoleInfoSupplierTestResource.class)
                    .addAsResource("application.properties")
                    .addAsResource("import-custom-table-name.sql", "import.sql"));

    @Test
    public void infoAvailable() {
        RestAssured.given()
                .param("expectedCreateDDLContent", "EXCEPTION")
                .param("expectedDropDDLContent", "EXCEPTION")
                .when().get("/dev-console-info-supplier/check-pu-info-with-successful-ddl-generation")
                .then().body(is("OK"));
    }

}
