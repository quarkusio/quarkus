package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityAgent;
import jakarta.persistence.PersistenceAgent;
import jakarta.transaction.Transactional;

import org.hibernate.StatelessSession;
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

    @PersistenceAgent(unitName = "users")
    StatelessSession usersStatelessSession;

    @PersistenceAgent(unitName = "inventory")
    StatelessSession inventoryStatelessSession;

    @Test
    @Transactional
    public void testUsersStatelessSession() {
        assertThat(usersStatelessSession).isNotNull();

        User user = new User("statelessSession");
        usersStatelessSession.insert(user);
        assertThat(usersStatelessSession.find(User.class, user.getId()).getName()).isEqualTo(user.getName());
    }

    @Test
    @Transactional
    public void testInventoryStatelessSession() {
        assertThat(inventoryStatelessSession).isNotNull();

        Plane plane = new Plane("Airbus A380");
        inventoryStatelessSession.insert(plane);
        assertThat(inventoryStatelessSession.find(Plane.class, plane.getId()).getName()).isEqualTo(plane.getName());

        assertThatThrownBy(() -> inventoryStatelessSession.insert(new User("wrongUnit")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type");
    }

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
