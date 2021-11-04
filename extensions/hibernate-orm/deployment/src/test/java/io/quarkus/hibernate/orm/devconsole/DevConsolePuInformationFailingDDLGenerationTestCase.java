package io.quarkus.hibernate.orm.devconsole;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DevConsolePuInformationFailingDDLGenerationTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntityWithFailingDDLGeneration.class,
                            TypeWithUnsupportedSqlCode.class,
                            DevConsoleInfoSupplierTestResource.class)
                    .addAsResource("application-generation-none.properties", "application.properties")
                    .addAsResource("import-custom-table-name.sql", "import.sql"));

    @Test
    public void infoAvailableButWithException() {
        RestAssured.given()
                .param("expectedCreateDDLContent", "EXCEPTION")
                .param("expectedDropDDLContent", "EXCEPTION")
                .when().get("/dev-console-info-supplier/check-pu-info-with-failing-ddl-generation")
                .then().body(is("OK"));
    }

}
