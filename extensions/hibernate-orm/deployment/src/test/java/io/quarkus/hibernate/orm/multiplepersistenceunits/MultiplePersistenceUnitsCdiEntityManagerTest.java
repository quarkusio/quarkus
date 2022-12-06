package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TransactionRequiredException;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsCdiEntityManagerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
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
        // Reads are allowed
        assertThatCode(() -> defaultEntityManager.createQuery("select count(*) from DefaultEntity"))
                .doesNotThrowAnyException();
        // Writes are not
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> defaultEntityManager.persist(defaultEntity))
                .isInstanceOf(TransactionRequiredException.class)
                .hasMessageContaining(
                        "Transaction is not active, consider adding @Transactional to your method to automatically activate one");
    }

    @Test
    public void defaultEntityManagerNoRequestNoTransaction() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> defaultEntityManager.persist(defaultEntity))
                .isInstanceOf(ContextNotActiveException.class)
                .hasMessageContainingAll(
                        "Cannot use the EntityManager/Session because neither a transaction nor a CDI request context is active",
                        "Consider adding @Transactional to your method to automatically activate a transaction",
                        "@ActivateRequestContext if you have valid reasons not to use transactions");
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
        // Reads are allowed
        assertThatCode(() -> usersEntityManager.createQuery("select count(*) from User"))
                .doesNotThrowAnyException();
        // Writes are not
        User user = new User("gsmet");
        assertThatThrownBy(() -> usersEntityManager.persist(user))
                .isInstanceOf(TransactionRequiredException.class)
                .hasMessageContaining(
                        "Transaction is not active, consider adding @Transactional to your method to automatically activate one");
    }

    @Test
    public void usersEntityManagerNoRequestNoTransaction() {
        User user = new User("gsmet");
        assertThatThrownBy(() -> usersEntityManager.persist(user))
                .isInstanceOf(ContextNotActiveException.class)
                .hasMessageContainingAll(
                        "Cannot use the EntityManager/Session because neither a transaction nor a CDI request context is active",
                        "Consider adding @Transactional to your method to automatically activate a transaction",
                        "@ActivateRequestContext if you have valid reasons not to use transactions");
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
        // Reads are allowed
        assertThatCode(() -> inventoryEntityManager.createQuery("select count(*) from Plane"))
                .doesNotThrowAnyException();
        // Writes are not
        Plane plane = new Plane("Airbus A380");
        assertThatThrownBy(() -> inventoryEntityManager.persist(plane))
                .isInstanceOf(TransactionRequiredException.class)
                .hasMessageContaining(
                        "Transaction is not active, consider adding @Transactional to your method to automatically activate one");
    }

    @Test
    public void inventoryEntityManagerNoRequestNoTransaction() {
        Plane plane = new Plane("Airbus A380");
        assertThatThrownBy(() -> inventoryEntityManager.persist(plane))
                .isInstanceOf(ContextNotActiveException.class)
                .hasMessageContainingAll(
                        "Cannot use the EntityManager/Session because neither a transaction nor a CDI request context is active",
                        "Consider adding @Transactional to your method to automatically activate a transaction",
                        "@ActivateRequestContext if you have valid reasons not to use transactions");
    }

    @Test
    @Transactional
    public void testUserInInventoryEntityManager() {
        User user = new User("gsmet");
        assertThatThrownBy(() -> inventoryEntityManager.persist(user)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity");
    }
}
