package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversDefaultSchemaCatalogTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MyAuditedEntity.class, EnversTestDefaultSchemaCatalogResource.class,
                    AbstractEnversResource.class)
            .addAsResource("application-with-default-schema-catalog.properties", "application.properties"));

    @Test
    public void testDefaultSchemaAndCatalog() {
        RestAssured.when().get("/envers-default-schema-catalog").then().body(is("OK"));
    }
}
