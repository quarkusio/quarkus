package io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hamcrest.Matchers;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.first.FirstEntity;
import io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.second.SecondEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MultiplePersistenceUnitConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FirstEntity.class, SecondEntity.class, PanacheTestResource.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Inject
    EntityManager defaultEntityManager;
    @Inject
    Session defaulSession;

    @Inject
    @PersistenceUnit("second")
    EntityManager secondEntityManager;
    @Inject
    @PersistenceUnit("second")
    Session secondSession;

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

    @Test
    void entityManagerShouldExist() {
        assertNotNull(FirstEntity.getEntityManager());
        assertEquals(FirstEntity.getEntityManager(), defaultEntityManager);

        assertNotNull(SecondEntity.getEntityManager());
        assertEquals(SecondEntity.getEntityManager(), secondEntityManager);
    }

    @Test
    void sessionShouldExist() {
        assertNotNull(FirstEntity.getSession());
        assertEquals(FirstEntity.getSession(), defaulSession);

        assertNotNull(SecondEntity.getSession());
        assertEquals(SecondEntity.getSession(), secondSession);
    }
}
