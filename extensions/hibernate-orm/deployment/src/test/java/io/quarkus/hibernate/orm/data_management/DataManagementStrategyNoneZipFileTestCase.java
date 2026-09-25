package io.quarkus.hibernate.orm.data_management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.InitScriptTestResource;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * With the "none" data management strategy, a data init script packaged as a zip file
 * stays available to explicit {@link org.hibernate.relational.SchemaManager} calls after startup.
 */
public class DataManagementStrategyNoneZipFileTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, InitScriptTestResource.class,
                            DataManagementStrategyNoneTestCase.SchemaManagerTestResource.class)
                    .addAsResource("application-data-init-script-as-zip-file-test.properties", "application.properties")
                    .addAsResource("load-script-test.zip"))
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.data-management.strategy", "none");

    @Test
    public void zipDataInitScriptAvailableToSchemaManager() {
        RestAssured.when().get("/orm-init-script/3").then()
                .body(Matchers.is(InitScriptTestResource.NO_ENTITY_MESSAGE));

        RestAssured.when().post("/orm-schema-manager/populate").then().statusCode(204);
        RestAssured.when().get("/orm-init-script/3").then()
                .body(Matchers.is("other-load-script sql load script entity"));
    }
}
