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
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that {@code quarkus.hibernate-orm."<pu>".jdbc.enabled=false} lets a specific persistence unit opt out of
 * blocking (JDBC) bootstrap, even though an H2 JDBC datasource is available for it, without affecting sibling
 * persistence units. Uses only in-memory H2 datasources, so it doesn't require any external service/container.
 */
public class MultiplePersistenceUnitsJdbcEnabledFalseTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units-jdbc-enabled-false.properties",
                            "application.properties"));

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    @Test
    @Transactional
    public void usersPersistenceUnitNotCreatedWhenJdbcDisabled() {
        // "inventory" has no override: its blocking PU is created and usable as normal.
        assertNotNull(inventoryEntityManager);

        // "users" explicitly disables jdbc bootstrap, so no bean should be exposed for it.
        EntityManager usersEntityManager = Arc.container()
                .instance(EntityManager.class, new PersistenceUnit.PersistenceUnitLiteral("users"))
                .get();
        assertThat(usersEntityManager).isNull();
    }
}
