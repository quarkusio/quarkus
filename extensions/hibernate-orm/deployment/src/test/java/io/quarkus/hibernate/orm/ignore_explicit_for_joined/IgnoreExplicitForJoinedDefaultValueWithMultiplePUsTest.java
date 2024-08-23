package io.quarkus.hibernate.orm.ignore_explicit_for_joined;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class IgnoreExplicitForJoinedDefaultValueWithMultiplePUsTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-pu-discriminator-ignore-explicit-for-joined-default-value.properties",
                            "application.properties"));
    @PersistenceUnit("users")
    @Inject
    EntityManager emUsers;

    @PersistenceUnit("inventory")
    @Inject
    EntityManager emInventory;

    @ActivateRequestContext
    @Test
    public void testDefaultValue() {
        Map<String, Object> usersProperties = emUsers.getEntityManagerFactory().getProperties();
        assertEquals("false", usersProperties.get("hibernate.discriminator.ignore_explicit_for_joined"));

        Map<String, Object> inventoryProperties = emUsers.getEntityManagerFactory().getProperties();
        assertEquals("false", inventoryProperties.get("hibernate.discriminator.ignore_explicit_for_joined"));
    }
}
