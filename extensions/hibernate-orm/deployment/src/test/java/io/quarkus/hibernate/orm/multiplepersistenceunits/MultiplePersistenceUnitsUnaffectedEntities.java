package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

/**
 * This is just a visual test for the warning.
 */
public class MultiplePersistenceUnitsUnaffectedEntities {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units-unaffected-entities.properties",
                            "application.properties"));

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    @Test
    @Transactional
    public void testPlane() {
        Plane plane = new Plane("Airbus A380");
        inventoryEntityManager.persist(plane);

        Plane savedPlane = inventoryEntityManager.find(Plane.class, plane.getId());
        assertEquals(plane.getName(), savedPlane.getName());
    }
}
