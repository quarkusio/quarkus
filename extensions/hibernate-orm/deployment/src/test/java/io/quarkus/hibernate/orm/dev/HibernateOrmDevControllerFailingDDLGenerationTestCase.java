package io.quarkus.hibernate.orm.dev;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.TestTags;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

@Tag(TestTags.DEVMODE)
public class HibernateOrmDevControllerFailingDDLGenerationTestCase {
    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntityWithFailingDDLGeneration.class,
                            TypeWithUnsupportedSqlCode.class,
                            H2CustomDialect.class,
                            HibernateOrmDevInfoServiceTestResource.class)
                    .addAsResource("application-generation-none-customh2.properties", "application.properties")
                    .addAsResource("import-custom-table-name.sql", "import.sql"));

    @Test
    public void infoAvailableButWithException() {
        RestAssured.given()
                .param("expectedCreateDDLContent", "EXCEPTION")
                .param("expectedDropDDLContent", "EXCEPTION")
                .when().get("/dev-info/check-pu-info-with-failing-ddl-generation")
                .then().body(is("OK"));
    }

}
