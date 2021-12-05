package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsCdiSessionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Inject
    Session defaultSession;

    @Inject
    @PersistenceUnit("users")
    Session usersSession;

    @Inject
    @PersistenceUnit("inventory")
    Session inventorySession;

    @Test
    @Transactional
    public void testDefault() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        defaultSession.persist(defaultEntity);

        DefaultEntity savedDefaultEntity = defaultSession.get(DefaultEntity.class, defaultEntity.getId());
        assertEquals(defaultEntity.getName(), savedDefaultEntity.getName());
    }

    @Test
    @Transactional
    public void testUser() {
        User user = new User("gsmet");
        usersSession.persist(user);

        User savedUser = usersSession.get(User.class, user.getId());
        assertEquals(user.getName(), savedUser.getName());
    }

    @Test
    @Transactional
    public void testPlane() {
        Plane plane = new Plane("Airbus A380");
        inventorySession.persist(plane);

        Plane savedPlane = inventorySession.get(Plane.class, plane.getId());
        assertEquals(plane.getName(), savedPlane.getName());
    }

    @Test
    @Transactional
    public void testUserInInventorySession() {
        User user = new User("gsmet");
        assertThatThrownBy(() -> inventorySession.persist(user)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity");
    }
}
