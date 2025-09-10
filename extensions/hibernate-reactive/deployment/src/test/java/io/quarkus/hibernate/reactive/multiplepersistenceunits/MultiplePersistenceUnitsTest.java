package io.quarkus.hibernate.reactive.multiplepersistenceunits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.reactive.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.reactive.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class MultiplePersistenceUnitsTest extends BaseMultiplePersistenceUnitTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units-with-default.properties",
                            "application.properties"));
    @Inject
    Mutiny.SessionFactory defaultEntityManager;

    @Inject
    @PersistenceUnit("inventory")
    Mutiny.SessionFactory inventoryEntityManager;

    @Test
    @Transactional
    @RunOnVertxContext
    public void createEntityAndRefetch(UniAsserter uniAsserter) {
        assertNotNull(defaultEntityManager);
        assertNotNull(inventoryEntityManager);

        User userEntity = new User("user");
        Plane inventoryEntity = new Plane("plane");

        Uni<String> resultUnit = this.defaultEntityManager.withTransaction(s -> s.persist(userEntity))
                .flatMap(a -> inventoryEntityManager.withTransaction(s -> s.persist(inventoryEntity)))
                .flatMap(a -> {
                    Uni<User> fetchedUser = fetchUser(defaultEntityManager);
                    Uni<Plane> fetchedPlane = fetchPlane(inventoryEntityManager);

                    return Uni.combine().all().unis(fetchedUser, fetchedPlane).asTuple()
                            .map(tuple -> tuple.getItem1().getName() + " " + tuple.getItem2().getName());

                });

        uniAsserter.assertThat(() -> resultUnit, t -> {
            assertEquals("user plane", t);
        });
    }
}
