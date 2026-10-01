package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.QuarkusExtensionTest;

public class ClassLevelPersistenceUnitAnnotationTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(EntityInInventoryPU.class)
                    .addClass(EntityInUsersPU.class)
                    .addClass(EntityWithMultiplePUs.class)
                    .addAsResource("application-multiple-persistence-units-class-level.properties",
                            "application.properties"));

    @Inject
    @PersistenceUnit("users")
    EntityManager usersEntityManager;

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    @Test
    @Transactional
    public void testEntityInInventoryPU() {
        EntityInInventoryPU entity = new EntityInInventoryPU("test-inventory");
        inventoryEntityManager.persist(entity);

        EntityInInventoryPU savedEntity = inventoryEntityManager.find(EntityInInventoryPU.class, entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());

        // Entity should not be accessible from users PU
        assertThatThrownBy(() -> usersEntityManager.find(EntityInInventoryPU.class, entity.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type");
    }

    @Test
    @Transactional
    public void testEntityInUsersPU() {
        EntityInUsersPU entity = new EntityInUsersPU("test-user");
        usersEntityManager.persist(entity);

        EntityInUsersPU savedEntity = usersEntityManager.find(EntityInUsersPU.class, entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());

        // Entity should not be accessible from inventory PU
        assertThatThrownBy(() -> inventoryEntityManager.find(EntityInUsersPU.class, entity.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type");
    }

    @Test
    @Transactional
    public void testEntityWithMultiplePUsInInventory() {
        EntityWithMultiplePUs entity = new EntityWithMultiplePUs("shared-entity-inventory");
        inventoryEntityManager.persist(entity);

        EntityWithMultiplePUs savedEntity = inventoryEntityManager.find(EntityWithMultiplePUs.class, entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());
    }

    @Test
    @Transactional
    public void testEntityWithMultiplePUsInUsers() {
        EntityWithMultiplePUs entity = new EntityWithMultiplePUs("shared-entity-users");
        usersEntityManager.persist(entity);

        EntityWithMultiplePUs savedEntity = usersEntityManager.find(EntityWithMultiplePUs.class, entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());
    }

    @Entity
    @Table(name = "entity_inventory")
    @PersistenceUnit("inventory")
    public static class EntityInInventoryPU {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String name;

        public EntityInInventoryPU() {
        }

        public EntityInInventoryPU(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity
    @Table(name = "entity_users")
    @PersistenceUnit("users")
    public static class EntityInUsersPU {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String name;

        public EntityInUsersPU() {
        }

        public EntityInUsersPU(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity
    @Table(name = "entity_multiple")
    @PersistenceUnit.List({ @PersistenceUnit("inventory"), @PersistenceUnit("users") })
    public static class EntityWithMultiplePUs {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String name;

        public EntityWithMultiplePUs() {
        }

        public EntityWithMultiplePUs(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
