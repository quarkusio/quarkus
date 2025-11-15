package io.quarkus.hibernate.reactive.panache.test.multiple_pu;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.test.multiple_pu.first.FirstEntity;
import io.quarkus.hibernate.reactive.panache.test.multiple_pu.second.SecondEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MultiplePersistenceUnitDefaultDisabledConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FirstEntity.class, SecondEntity.class, PanacheTestResource.class)
                    .addAsResource("application-multiple-persistence-units-default-disabled.properties",
                            "application.properties"));

    @Test
    public void panacheOperations() {
        /**
         * First entity operations
         */
        RestAssured.when().get("/persistence-unit/first-explicit/name-1-default-disabled").then().body(Matchers.is("1"));

        /**
         * second entity operations
         */
        RestAssured.when().get("/persistence-unit/second/name-2-default-disabled").then().body(Matchers.is("1"));
    }

    @Test
    public void testWithSessionOnDemandMultiplePersistenceUnits() {
        RestAssured.when().get("/persistence-unit/session-on-demand/test-default-disabled").then().body(Matchers.is("1,1"));
    }
}
