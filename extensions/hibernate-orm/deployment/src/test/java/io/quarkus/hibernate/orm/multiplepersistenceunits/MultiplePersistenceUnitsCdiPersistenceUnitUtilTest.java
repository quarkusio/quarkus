package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsCdiPersistenceUnitUtilTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Inject
    PersistenceUnitUtil defaultPersistenceUnitUtil;

    @Inject
    EntityManager defaultEntityManager;

    @Inject
    @PersistenceUnit("users")
    PersistenceUnitUtil usersPersistenceUnitUtil;

    @Inject
    @PersistenceUnit("users")
    EntityManager usersEntityManager;

    @Inject
    @PersistenceUnit("inventory")
    PersistenceUnitUtil inventoryPersistenceUnitUtil;

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    @Test
    @Transactional
    public void testDefaultPersistenceUnitUtil() {
        assertNotNull(defaultPersistenceUnitUtil);

        DefaultEntity entity = new DefaultEntity("test");
        defaultEntityManager.persist(entity);

        assertTrue(defaultPersistenceUnitUtil.isLoaded(entity, "name"));
    }

    @Test
    @Transactional
    public void testUsersPersistenceUnitUtil() {
        assertNotNull(usersPersistenceUnitUtil);

        User entity = new User("test");
        usersEntityManager.persist(entity);

        assertTrue(usersPersistenceUnitUtil.isLoaded(entity, "name"));
    }

    @Test
    @Transactional
    public void testInventoryPersistenceUnitUtil() {
        assertNotNull(inventoryPersistenceUnitUtil);

        Plane entity = new Plane("test");
        inventoryEntityManager.persist(entity);

        assertTrue(inventoryPersistenceUnitUtil.isLoaded(entity, "name"));
    }
}