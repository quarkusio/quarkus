package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.override.OverriddenEntity;
import io.quarkus.test.QuarkusExtensionTest;

// An entity's class-level @PersistenceUnit annotation must take precedence over a
// package-level @PersistenceUnit annotation applied to the package the entity belongs to.
public class PackageLevelAnnotationOverriddenByClassLevelAnnotationTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(OverriddenEntity.class.getPackage().getName())
                    .addAsResource("application-multiple-persistence-units-package-override.properties",
                            "application.properties"));

    @Inject
    @PersistenceUnit("users")
    EntityManager usersEntityManager;

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    @Test
    @Transactional
    public void entityIsOnlyInClassLevelPersistenceUnit() {
        OverriddenEntity entity = new OverriddenEntity("test");
        usersEntityManager.persist(entity);

        OverriddenEntity savedEntity = usersEntityManager.find(OverriddenEntity.class, entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());

        // The package-level annotation says "inventory", but the class-level annotation ("users")
        // must win: the entity should not be known to the "inventory" persistence unit.
        assertThatThrownBy(() -> inventoryEntityManager.find(OverriddenEntity.class, entity.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type");
    }
}
