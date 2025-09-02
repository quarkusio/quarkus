package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsCdiMetamodelTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Inject
    Metamodel defaultMetamodel;

    @Inject
    @PersistenceUnit("users")
    Metamodel usersMetamodel;

    @Inject
    @PersistenceUnit("inventory")
    Metamodel inventoryMetamodel;

    @Test
    public void defaultMetamodel() {
        assertNotNull(defaultMetamodel);

        EntityType<DefaultEntity> entityType = defaultMetamodel.entity(DefaultEntity.class);
        assertNotNull(entityType);

        assertNotNull(entityType.getName());
        assertEquals(DefaultEntity.class.getSimpleName(), entityType.getName());
    }

    @Test
    public void usersMetamodel() {
        assertNotNull(usersMetamodel);

        EntityType<User> entityType = usersMetamodel.entity(User.class);
        assertNotNull(entityType);

        assertEquals(User.class.getSimpleName(), entityType.getName());
    }

    @Test
    public void inventoryMetamodel() {
        assertNotNull(inventoryMetamodel);

        EntityType<Plane> entityType = inventoryMetamodel.entity(Plane.class);
        assertNotNull(entityType);

        assertEquals(Plane.class.getSimpleName(), entityType.getName());
    }

    @Test
    public void testUserInInventoryMetamodel() {
        assertThatThrownBy(() -> {
            inventoryMetamodel.entity(User.class);
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not an entity");
    }

}
