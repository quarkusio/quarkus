package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.PersistenceAgent;
import jakarta.transaction.Transactional;

import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusExtensionTest;

public class MultiplePersistenceUnitsResourceInjectionStatelessSessionTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @PersistenceAgent(unitName = "users")
    StatelessSession usersStatelessSession;

    @PersistenceAgent(unitName = "inventory")
    StatelessSession inventoryStatelessSession;

    @Test
    @Transactional
    public void testUser() {
        User user = new User("gsmet");
        usersStatelessSession.insert(user);

        User savedUser = usersStatelessSession.find(User.class, user.getId());
        assertThat(savedUser.getName()).isEqualTo(user.getName());
    }

    @Test
    @Transactional
    public void testPlane() {
        Plane plane = new Plane("Airbus A380");
        inventoryStatelessSession.insert(plane);

        Plane savedPlane = inventoryStatelessSession.find(Plane.class, plane.getId());
        assertThat(savedPlane.getName()).isEqualTo(plane.getName());
    }

    @Test
    @Transactional
    public void testUserInInventoryStatelessSession() {
        User user = new User("gsmet");
        assertThatThrownBy(() -> inventoryStatelessSession.insert(user)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type");
    }

    @Test
    public void testAccessBothPersistenceUnits() {
        testUser();
        testPlane();
    }

}
