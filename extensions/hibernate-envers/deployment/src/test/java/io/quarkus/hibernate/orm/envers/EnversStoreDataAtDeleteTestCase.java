package io.quarkus.hibernate.orm.envers;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class EnversStoreDataAtDeleteTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyAuditedEntity.class, MyRevisionEntity.class, MyRevisionListener.class,
                            EnversTestStoreDataAtDeleteResource.class)
                    .addAsResource("application-with-store-data-at-delete.properties", "application.properties"));

    @Test
    public void testStoreDataAtDelete() {
        RestAssured.when().delete("/envers-store-data-at-delete").then()
                .body(is("OK"));
    }

}
