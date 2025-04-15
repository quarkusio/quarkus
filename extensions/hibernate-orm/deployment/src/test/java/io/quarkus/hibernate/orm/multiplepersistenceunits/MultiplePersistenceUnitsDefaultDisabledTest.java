package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsDefaultDisabledTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units-default-disabled.properties",
                            "application.properties"));

    @Inject
    @PersistenceUnit("users")
    EntityManager usersEntityManager;

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    @Test
    @Transactional
    public void defaultEntityManagerNotCreated() {
        assertNotNull(usersEntityManager);
        assertNotNull(inventoryEntityManager);

        EntityManager defaultEntityManager = Arc.container().instance(EntityManager.class).get();
        assertThat(defaultEntityManager).isNull();
    }
}
