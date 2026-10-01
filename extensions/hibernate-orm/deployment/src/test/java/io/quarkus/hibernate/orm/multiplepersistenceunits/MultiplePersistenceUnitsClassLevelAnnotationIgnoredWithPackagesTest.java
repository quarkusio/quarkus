package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.mixed.MixedConfigAndClassLevelEntity;
import io.quarkus.test.QuarkusExtensionTest;

// Reproducer for the case where .packages Quarkus configuration is used together with
// a class-level @PersistenceUnit annotation on an entity whose package is assigned,
// through configuration, to a persistence unit different from the one targeted by the
// annotation. The .packages configuration must win and the annotation must be ignored.
public class MultiplePersistenceUnitsClassLevelAnnotationIgnoredWithPackagesTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MixedConfigAndClassLevelEntity.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units-class-level-ignored-with-packages.properties",
                            "application.properties"));

    @Inject
    @PersistenceUnit("users")
    EntityManager usersEntityManager;

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    @Test
    @Transactional
    public void classLevelAnnotationIsIgnoredInFavorOfPackagesConfiguration() {
        // MixedConfigAndClassLevelEntity is in a package assigned, via .packages configuration,
        // to the "users" persistence unit, even though it also carries a class-level
        // @PersistenceUnit("inventory") annotation.
        MixedConfigAndClassLevelEntity entity = new MixedConfigAndClassLevelEntity("test");
        usersEntityManager.persist(entity);

        MixedConfigAndClassLevelEntity savedEntity = usersEntityManager.find(MixedConfigAndClassLevelEntity.class,
                entity.getId());
        assertEquals(entity.getName(), savedEntity.getName());

        // The class-level annotation targeting "inventory" must be ignored:
        // the entity must not be known to the inventory persistence unit.
        assertThatThrownBy(() -> inventoryEntityManager.find(MixedConfigAndClassLevelEntity.class, entity.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type");
    }
}
