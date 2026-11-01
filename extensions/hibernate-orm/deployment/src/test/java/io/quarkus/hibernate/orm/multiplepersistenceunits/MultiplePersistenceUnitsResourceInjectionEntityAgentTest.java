package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityAgent;
import jakarta.persistence.PersistenceAgent;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusExtensionTest;

public class MultiplePersistenceUnitsResourceInjectionEntityAgentTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @PersistenceAgent(unitName = "users")
    EntityAgent usersEntityAgent;

    @PersistenceAgent(unitName = "inventory")
    EntityAgent inventoryEntityAgent;

    @Test
    @Transactional
    public void testUser() {
        User user = new User("gsmet");
        usersEntityAgent.insert(user);

        User savedUser = usersEntityAgent.find(User.class, user.getId());
        assertThat(savedUser.getName()).isEqualTo(user.getName());
    }

    @Test
    @Transactional
    public void testPlane() {
        Plane plane = new Plane("Airbus A380");
        inventoryEntityAgent.insert(plane);

        Plane savedPlane = inventoryEntityAgent.find(Plane.class, plane.getId());
        assertThat(savedPlane.getName()).isEqualTo(plane.getName());
    }

    @Test
    @Transactional
    public void testUserInInventoryEntityAgent() {
        User user = new User("gsmet");
        assertThatThrownBy(() -> inventoryEntityAgent.insert(user)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type");
    }

    @Test
    public void testAccessBothPersistenceUnits() {
        testUser();
        testPlane();
    }

}
