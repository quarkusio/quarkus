package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.override.EntityWithoutClassLevelAnnotation;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.override.OverriddenEntity;
import io.quarkus.test.QuarkusExtensionTest;

// An entity's class-level @PersistenceUnit annotation must take precedence over a
// package-level @PersistenceUnit annotation applied to the package the entity belongs to.
public class PackageLevelAnnotationOverriddenByClassLevelAnnotationTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(OverriddenEntity.class.getPackage().getName()))
            .withConfiguration("""
                    quarkus.hibernate-orm.datasource=<default>
                    quarkus.hibernate-orm."users".datasource=users
                    quarkus.hibernate-orm."inventory".datasource=inventory
                    """);

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

    @Test
    @Transactional
    public void otherEntityInSamePackageStillFollowsPackageLevelAnnotation() {
        EntityWithoutClassLevelAnnotation entity = new EntityWithoutClassLevelAnnotation("test");
        inventoryEntityManager.persist(entity);

        EntityWithoutClassLevelAnnotation savedEntity = inventoryEntityManager.find(
                EntityWithoutClassLevelAnnotation.class, entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());

        // This entity has no class-level annotation, so the package-level annotation ("inventory")
        // still applies: the entity should not be known to the "users" persistence unit.
        assertThatThrownBy(() -> usersEntityManager.find(EntityWithoutClassLevelAnnotation.class, entity.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type");
    }
}
