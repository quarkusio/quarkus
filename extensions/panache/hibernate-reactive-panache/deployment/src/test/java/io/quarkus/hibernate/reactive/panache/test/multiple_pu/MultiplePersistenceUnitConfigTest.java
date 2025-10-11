package io.quarkus.hibernate.reactive.panache.test.multiple_pu;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.test.multiple_pu.first.FirstEntity;
import io.quarkus.hibernate.reactive.panache.test.multiple_pu.second.SecondEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MultiplePersistenceUnitConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FirstEntity.class, SecondEntity.class, PanacheTestResource.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Test
    public void panacheOperations() {
        /**
         * First entity operations
         */
        RestAssured.when().get("/persistence-unit/first-default/name-1").then().body(Matchers.is("1"));

        /**
         * second entity operations
         */
        RestAssured.when().get("/persistence-unit/second/name-2").then().body(Matchers.is("1"));
    }

    @Test
    public void testWithSessionOnDemandMultiplePersistenceUnits() {
        RestAssured.when().get("/persistence-unit/session-on-demand/test").then().body(Matchers.is("1,1"));
    }
}
