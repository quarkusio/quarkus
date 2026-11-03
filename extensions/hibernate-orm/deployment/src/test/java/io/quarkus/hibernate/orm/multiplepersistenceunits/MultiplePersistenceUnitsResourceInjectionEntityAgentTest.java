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
                    .addClasses(User.class, Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @PersistenceAgent(unitName = "users")
    EntityAgent usersAgent;

    @PersistenceAgent(unitName = "inventory")
    EntityAgent inventoryAgent;

    @Test
    @Transactional
    public void testUser() {
        assertThat(usersAgent).isNotNull();
        User user = new User("gsmet");
        usersAgent.insert(user);

        assertThat(usersAgent.find(User.class, user.getId()).getName()).isEqualTo(user.getName());
    }

    @Test
    @Transactional
    public void testPlane() {
        assertThat(inventoryAgent).isNotNull();
        Plane plane = new Plane("Airbus A380");
        inventoryAgent.insert(plane);

        assertThat(inventoryAgent.find(Plane.class, plane.getId()).getName()).isEqualTo(plane.getName());
    }

    @Test
    @Transactional
    public void testUserInInventoryAgent() {
        assertThat(inventoryAgent).isNotNull();
        assertThatThrownBy(() -> inventoryAgent.insert(new User("gsmet")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type");
    }
}
