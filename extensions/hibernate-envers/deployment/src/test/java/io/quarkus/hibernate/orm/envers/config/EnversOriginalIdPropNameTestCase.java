package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversOriginalIdPropNameTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MyAuditedEntity.class, EnversTestOriginalIdPropNameResource.class, AbstractEnversResource.class)
            .addAsResource("application-with-original-id-prop-name.properties", "application.properties"));

    @Test
    public void testOriginalIdPropNameOverride() {
        RestAssured.when().get("/envers-original-id-prop-name").then().body(is("OK"));
    }
}
