package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.persistence.SchemaManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsSchemaManagerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Inject
    SchemaManager defaultSchemaManager;

    @Inject
    @PersistenceUnit("users")
    SchemaManager usersSchemaManager;

    @Inject
    @PersistenceUnit("inventory")
    SchemaManager inventorySchemaManager;

    @Inject
    @PersistenceUnit("users")
    org.hibernate.relational.SchemaManager usersHibernateCriteriaBuilder;

    @Test
    public void testDefaultSchemaManager() {
        assertNotNull(defaultSchemaManager);
    }

    @Test
    public void testUsersSchemaManager() {
        assertNotNull(usersSchemaManager);
    }

    @Test
    public void testInventorySchemaManager() {
        assertNotNull(inventorySchemaManager);
    }

    @Test
    public void testUsersHibernateSchemaManager() {
        assertNotNull(usersHibernateCriteriaBuilder);
    }
}
