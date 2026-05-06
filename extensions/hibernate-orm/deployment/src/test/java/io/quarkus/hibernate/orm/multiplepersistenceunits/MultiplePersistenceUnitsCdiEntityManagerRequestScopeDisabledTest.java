package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusExtensionTest;

public class MultiplePersistenceUnitsCdiEntityManagerRequestScopeDisabledTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Inject
    EntityManager defaultEntityManager;

    @Inject
    @PersistenceUnit("users")
    EntityManager usersEntityManager;

    @Inject
    @PersistenceUnit("inventory")
    EntityManager inventoryEntityManager;

    // The following sessionFactories are not used in tests but needs to be correctly created, if not the test will fail
    @Inject
    SessionFactory defaultSessionFactory;

    @Inject
    @Named("users")
    SessionFactory usersFactory;

    @Inject
    @Named("inventory")
    SessionFactory inventoryFactory;

    @Inject
    @PersistenceUnit("<default>")
    EntityManager defaultEntityManagerWithQualifier;

    @Inject
    @PersistenceUnit("<default>")
    SessionFactory defaultSessionFactoryWithQualifier;

    @Inject
    @Named("<default>")
    @PersistenceUnit("<default>")
    EntityManager defaultEntityManagerWithBothQualifiers;

    @Inject
    @Named("<default>")
    @PersistenceUnit("<default>")
    SessionFactory defaultSessionFactoryWithBothQualifiers;

    @Test
    @Transactional
    public void defaultEntityManagerInTransaction() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        defaultEntityManager.persist(defaultEntity);

        DefaultEntity savedDefaultEntity = defaultEntityManager.find(DefaultEntity.class, defaultEntity.getId());
        assertEquals(defaultEntity.getName(), savedDefaultEntity.getName());
    }

    @Test
    @ActivateRequestContext
    public void defaultEntityManagerInRequestNoTransaction() {
        // With request-scoped disabled, both reads and writes fail
        assertThatThrownBy(() -> defaultEntityManager.createQuery("select count(*) from DefaultEntity"))
                .hasMessageContaining("Cannot use the EntityManager/Session because no transaction is active");
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> defaultEntityManager.persist(defaultEntity))
                .hasMessageContaining("Cannot use the EntityManager/Session because no transaction is active");
    }

    @Test
    public void defaultEntityManagerNoRequestNoTransaction() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> defaultEntityManager.persist(defaultEntity))
                .isInstanceOf(ContextNotActiveException.class)
                .hasMessageContainingAll(
                        "Cannot use the EntityManager/Session because no transaction is active",
                        "Consider adding @Transactional to your method to automatically activate a transaction",
                        "set 'quarkus.hibernate-orm.request-scoped.enabled' to 'true' if you have valid reasons not to use transactions");
    }

    @Test
    @Transactional
    public void usersEntityManagerInTransaction() {
        User user = new User("gsmet");
        usersEntityManager.persist(user);

        User savedUser = usersEntityManager.find(User.class, user.getId());
        assertEquals(user.getName(), savedUser.getName());
    }

    @Test
    @ActivateRequestContext
    public void usersEntityManagerInRequestNoTransaction() {
        // With request-scoped disabled, both reads and writes fail
        assertThatThrownBy(() -> usersEntityManager.createQuery("select count(*) from User"))
                .hasMessageContaining("Cannot use the EntityManager/Session because no transaction is active");
        User user = new User("gsmet");
        assertThatThrownBy(() -> usersEntityManager.persist(user))
                .hasMessageContaining("Cannot use the EntityManager/Session because no transaction is active");
    }

    @Test
    public void usersEntityManagerNoRequestNoTransaction() {
        User user = new User("gsmet");
        assertThatThrownBy(() -> usersEntityManager.persist(user))
                .isInstanceOf(ContextNotActiveException.class)
                .hasMessageContainingAll(
                        "Cannot use the EntityManager/Session because no transaction is active",
                        "Consider adding @Transactional to your method to automatically activate a transaction",
                        "set 'quarkus.hibernate-orm.request-scoped.enabled' to 'true' if you have valid reasons not to use transactions");
    }

    @Test
    @Transactional
    public void inventoryEntityManagerInTransaction() {
        Plane plane = new Plane("Airbus A380");
        inventoryEntityManager.persist(plane);

        Plane savedPlane = inventoryEntityManager.find(Plane.class, plane.getId());
        assertEquals(plane.getName(), savedPlane.getName());
    }

    @Test
    @ActivateRequestContext
    public void inventoryEntityManagerInRequestNoTransaction() {
        // With request-scoped disabled, both reads and writes fail
        assertThatThrownBy(() -> inventoryEntityManager.createQuery("select count(*) from Plane"))
                .hasMessageContaining("Cannot use the EntityManager/Session because no transaction is active");
        Plane plane = new Plane("Airbus A380");
        assertThatThrownBy(() -> inventoryEntityManager.persist(plane))
                .hasMessageContaining("Cannot use the EntityManager/Session because no transaction is active");
    }

    @Test
    public void inventoryEntityManagerNoRequestNoTransaction() {
        Plane plane = new Plane("Airbus A380");
        assertThatThrownBy(() -> inventoryEntityManager.persist(plane))
                .isInstanceOf(ContextNotActiveException.class)
                .hasMessageContainingAll(
                        "Cannot use the EntityManager/Session because no transaction is active",
                        "Consider adding @Transactional to your method to automatically activate a transaction",
                        "set 'quarkus.hibernate-orm.request-scoped.enabled' to 'true' if you have valid reasons not to use transactions");
    }

    @Test
    @Transactional
    public void testUserInInventoryEntityManager() {
        User user = new User("gsmet");
        assertThatThrownBy(() -> inventoryEntityManager.persist(user)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity type");
    }

    @Test
    public void defaultQualifiedEntityManagerAndSessionFactoryAreInjected() {
        Assertions.assertNotNull(defaultEntityManagerWithQualifier,
                "@PersistenceUnit(\"<default>\") EntityManager should be injected and non-null");
        Assertions.assertNotNull(defaultSessionFactoryWithQualifier,
                "@PersistenceUnit(\"<default>\") SessionFactory should be injected and non-null");
    }

    @Test
    public void defaultEntityManagerAndSessionFactoryWithBothQualifiersAreInjected() {
        Assertions.assertNotNull(defaultEntityManagerWithBothQualifiers,
                "EntityManager should be injectable with both @Named and @PersistenceUnit qualifiers for <default>");
        Assertions.assertNotNull(defaultSessionFactoryWithBothQualifiers,
                "SessionFactory should be injectable with both @Named and @PersistenceUnit qualifiers for <default>");
    }
}
