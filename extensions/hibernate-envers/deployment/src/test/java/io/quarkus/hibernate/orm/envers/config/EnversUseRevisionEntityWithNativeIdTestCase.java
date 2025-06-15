package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversUseRevisionEntityWithNativeIdTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MyAuditedEntity.class, EnversTestUseRevisionEntityWithNativeIdResource.class,
                    AbstractEnversResource.class)
            .addAsResource("application-with-use-revision-entity-with-native-id.properties", "application.properties"));

    @Test
    public void testUseRevisionEntityWithNativeId() {
        RestAssured.when().get("/envers-use-revision-entity-with-native-id").then().body(is("OK"));
    }
}
