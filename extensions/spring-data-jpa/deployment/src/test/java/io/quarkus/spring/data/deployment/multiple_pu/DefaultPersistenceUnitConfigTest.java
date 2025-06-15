package io.quarkus.spring.data.deployment.multiple_pu;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.spring.data.deployment.multiple_pu.first.FirstEntity;
import io.quarkus.spring.data.deployment.multiple_pu.first.FirstEntityRepository;
import io.quarkus.spring.data.deployment.multiple_pu.second.SecondEntity;
import io.quarkus.spring.data.deployment.multiple_pu.second.SecondEntityRepository;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class DefaultPersistenceUnitConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(FirstEntity.class, SecondEntity.class, FirstEntityRepository.class,
                    SecondEntityRepository.class, PanacheTestResource.class)
            .addAsResource("application-test.properties", "application.properties"));

    @Test
    public void panacheOperations() {
        /**
         * First entity operations
         */
        RestAssured.when().get("/persistence-unit/first/name-1").then().body(Matchers.is("1"));
        RestAssured.when().get("/persistence-unit/first/name-2").then().body(Matchers.is("2"));

        /**
         * second entity operations
         */
        RestAssured.when().get("/persistence-unit/second/name-1").then().body(Matchers.is("1"));
        RestAssured.when().get("/persistence-unit/second/name-2").then().body(Matchers.is("2"));
    }
}
