package io.quarkus.hibernate.orm.envers.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversTrackEntitiesChangedInRevisionTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(MyAuditedEntity.class, EnversTestTrackEntitiesChangedInRevisionResource.class,
                    AbstractEnversResource.class)
            .addAsResource("application-with-track-entities-changed-in-revision.properties", "application.properties"));

    @Test
    public void testTrackEntitiesChangedInRevision() {
        RestAssured.when().get("/envers-track-entities-changed-in-revision").then().body(is("OK"));
    }
}
