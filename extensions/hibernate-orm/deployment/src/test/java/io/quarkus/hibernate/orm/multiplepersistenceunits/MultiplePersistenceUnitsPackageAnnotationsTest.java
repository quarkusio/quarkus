package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.shared.SharedEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.user.User;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.annotation.user.subpackage.OtherUserInSubPackage;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsPackageAnnotationsTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(Plane.class.getPackage().getName())
                    .addPackage(SharedEntity.class.getPackage().getName())
                    .addPackage(User.class.getPackage().getName())
                    .addPackage(OtherUserInSubPackage.class.getPackage().getName())
                    .addAsResource("application-multiple-persistence-units-annotations.properties", "application.properties"));

    @Inject
    EntityManager defaultEntityManager;

    @Inject
    @PersistenceUnit("users")
    EntityManager usersEntityManager;

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    @Test
    @Transactional
    public void testDefault() {
        SharedEntity defaultEntity = new SharedEntity("default");
        defaultEntityManager.persist(defaultEntity);

        SharedEntity savedDefaultEntity = defaultEntityManager.find(SharedEntity.class, defaultEntity.getId());
        assertEquals(defaultEntity.getName(), savedDefaultEntity.getName());

        SharedEntity defaultEntity2 = new SharedEntity("default2");
        usersEntityManager.persist(defaultEntity2);

        SharedEntity savedDefaultEntity2 = usersEntityManager.find(SharedEntity.class, defaultEntity2.getId());
        assertEquals(defaultEntity2.getName(), savedDefaultEntity2.getName());

        SharedEntity defaultEntity3 = new SharedEntity("default3");
        inventoryEntityManager.persist(defaultEntity3);

        SharedEntity savedDefaultEntity3 = inventoryEntityManager.find(SharedEntity.class, defaultEntity3.getId());
        assertEquals(defaultEntity3.getName(), savedDefaultEntity3.getName());
    }

    @Test
    @Transactional
    public void testUser() {
        User user = new User("gsmet");
        usersEntityManager.persist(user);

        User savedUser = usersEntityManager.find(User.class, user.getId());
        assertEquals(user.getName(), savedUser.getName());

        OtherUserInSubPackage otherUserInSubPackage = new OtherUserInSubPackage("other-user");
        usersEntityManager.persist(otherUserInSubPackage);

        OtherUserInSubPackage savedOtherUserInSubPackage = usersEntityManager.find(OtherUserInSubPackage.class,
                otherUserInSubPackage.getId());
        assertEquals(savedOtherUserInSubPackage.getName(), savedOtherUserInSubPackage.getName());
    }

    @Test
    @Transactional
    public void testPlane() {
        Plane plane = new Plane("Airbus A380");
        inventoryEntityManager.persist(plane);

        Plane savedPlane = inventoryEntityManager.find(Plane.class, plane.getId());
        assertEquals(plane.getName(), savedPlane.getName());
    }

    @Test
    @Transactional
    public void testUserInInventoryEntityManager() {
        User user = new User("gsmet");
        assertThatThrownBy(() -> inventoryEntityManager.persist(user)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity");
    }
}
