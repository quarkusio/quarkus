package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.QuarkusExtensionTest;

public class MultiplePersistenceUnitsClassLevelAnnotationTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(EntityWithClassLevelPersistenceUnit.class)
                    .addAsResource("application-multiple-persistence-units-class-level.properties",
                            "application.properties"));

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    @Inject
    @PersistenceUnit("users")
    EntityManager usersEntityManager;

    @Test
    @Transactional
    public void testClassLevelAnnotation() {
        EntityWithClassLevelPersistenceUnit entity = new EntityWithClassLevelPersistenceUnit("test-entity");
        inventoryEntityManager.persist(entity);

        EntityWithClassLevelPersistenceUnit savedEntity = inventoryEntityManager.find(
                EntityWithClassLevelPersistenceUnit.class, entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());

        // Entity should not be accessible from users PU
        assertThatThrownBy(() -> usersEntityManager.find(EntityWithClassLevelPersistenceUnit.class, entity.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type");
    }

    @Entity
    @PersistenceUnit("inventory")
    public static class EntityWithClassLevelPersistenceUnit {

        private long id;

        private String name;

        public EntityWithClassLevelPersistenceUnit() {
        }

        public EntityWithClassLevelPersistenceUnit(String name) {
            this.name = name;
        }

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "planeSeq")
        public long getId() {
            return id;
        }

        public void setId(long id) {
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
