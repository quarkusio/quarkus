package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.StatelessSession;
import org.hibernate.UnknownEntityTypeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsCdiStatelessSessionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Inject
    StatelessSession defaultSession;

    @Inject
    @PersistenceUnit("users")
    StatelessSession usersSession;

    @Inject
    @PersistenceUnit("inventory")
    StatelessSession inventorySession;

    @Test
    @Transactional
    public void defaultEntityManagerInTransaction() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        defaultSession.insert(defaultEntity);

        DefaultEntity savedDefaultEntity = defaultSession.get(DefaultEntity.class, defaultEntity.getId());
        assertEquals(defaultEntity.getName(), savedDefaultEntity.getName());
    }

    @Transactional
    public void usersEntityManagerInTransaction() {
        User user = new User("gsmet");
        usersSession.insert(user);

        User savedUser = usersSession.get(User.class, user.getId());
        assertEquals(user.getName(), savedUser.getName());
    }

    @Test
    @Transactional
    public void inventoryEntityManagerInTransaction() {
        Plane plane = new Plane("Airbus A380");
        inventorySession.insert(plane);

        Plane savedPlane = inventorySession.get(Plane.class, plane.getId());
        assertEquals(plane.getName(), savedPlane.getName());
    }

    @Test
    @Transactional
    public void testUserInInventorySession() {
        User user = new User("gsmet");
        assertThatThrownBy(() -> inventorySession.insert(user)).isInstanceOf(UnknownEntityTypeException.class);
    }
}
