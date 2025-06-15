package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversRevisionOnCollectionChangeTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyAuditedEntity.class, EnversTestRevisionOnCollectionChangeResource.class,
                            AbstractEnversResource.class)
                    .addAsResource("application-with-revision-on-collection-change.properties",
                            "application.properties"));

    @Test
    public void testRevisionOnCollectionChange() {
        RestAssured.when().get("/envers-revision-on-collection-change").then().body(is("OK"));
    }
}
