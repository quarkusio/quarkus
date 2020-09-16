package io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.first.FirstEntity;
import io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.second.SecondEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MultiplePersistenceUnitConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FirstEntity.class, SecondEntity.class, PanacheTestResource.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Test
    public void panacheOperations() {
        /**
         * First entity operations
         */
        RestAssured.when().get("/persistence-unit/first/name-1").then().body(Matchers.is("name-1"));
        RestAssured.when().get("/persistence-unit/first/name-2").then().body(Matchers.is("name-2"));

        /**
         * second entity operations
         */
        RestAssured.when().get("/persistence-unit/second/name-1").then().body(Matchers.is("name-1"));
        RestAssured.when().get("/persistence-unit/second/name-2").then().body(Matchers.is("name-2"));
    }
}
