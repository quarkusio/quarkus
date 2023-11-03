package io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.repository;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.first.FirstEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MultiplePersistenceUnitConfigForRepositoryTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Issue11842Entity.class, Issue11842Repository.class, Issue11842Resource.class, FirstEntity.class)
                    .addAsResource("application-multiple-persistence-units-for-repository.properties",
                            "application.properties"));

    @Test
    public void panacheOperations() {
        // Using PanacheRepository
        RestAssured.when().get("/persistence-unit/repository/name-1").then().body(Matchers.is("1"));
        RestAssured.when().get("/persistence-unit/repository/name-2").then().body(Matchers.is("2"));

        // Using PanacheEntity
        RestAssured.when().get("/persistence-unit/panache-entity/name-1").then().body(Matchers.is("1"));
        RestAssured.when().get("/persistence-unit/panache-entity/name-2").then().body(Matchers.is("2"));
    }
}
