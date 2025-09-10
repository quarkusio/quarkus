package io.quarkus.hibernate.reactive.panache.test.multiple_pu;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.hibernate.reactive.panache.test.multiple_pu.first.FirstEntity;
import io.quarkus.hibernate.reactive.panache.test.multiple_pu.second.SecondEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;

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

    @RunOnVertxContext
    @Test
    public void testCreationUpdate(UniAsserter asserter) {
        asserter.execute(() -> createEntity()
                .flatMap(fe -> {
                    return findEntity(fe.id)
                            .flatMap(foundEntity -> {
                                Assertions.assertEquals("firstEntityName", foundEntity.name);
                                return updateFirstEntityName(fe.id);
                            })
                            .flatMap(v -> findEntity(fe.id))
                            .map(person2 -> {
                                Assertions.assertEquals("updatedFirstEntityName", person2.name);
                                return null;
                            });
                }).flatMap(v -> deleteAll()));
    }

    @WithTransaction("second")
    Uni<SecondEntity> createEntity() {
        SecondEntity personPanache = new SecondEntity();
        personPanache.name = "firstEntityName";
        return personPanache.persistAndFlush().map(v -> personPanache);
    }

    @WithTransaction("second")
    Uni<Void> updateFirstEntityName(Long id) {
        return SecondEntity.<SecondEntity> findById(id)
                .map(person -> {
                    person.name = "updatedFirstEntityName";
                    return null;
                });
    }

    @WithSession("second")
    Uni<SecondEntity> findEntity(Long id) {
        return SecondEntity.<SecondEntity> findById(id)
                .onItem().transform(entity -> {
                    System.out.println("Entity: " + entity.name);
                    return entity;
                });
    }

    @WithSession("second")
    Uni<Long> deleteAll() {
        return SecondEntity.deleteAll();
    }
}
