package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.TransactionRequiredException;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsCdiSessionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Inject
    Session defaultSession;

    @Inject
    @PersistenceUnit("users")
    Session usersSession;

    @Inject
    @PersistenceUnit("inventory")
    Session inventorySession;

    @Test
    @Transactional
    public void defaultEntityManagerInTransaction() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        defaultSession.persist(defaultEntity);

        DefaultEntity savedDefaultEntity = defaultSession.get(DefaultEntity.class, defaultEntity.getId());
        assertEquals(defaultEntity.getName(), savedDefaultEntity.getName());
    }

    @Test
    @ActivateRequestContext
    public void defaultEntityManagerInRequestNoTransaction() {
        // Reads are allowed
        assertThatCode(() -> defaultSession.createQuery("select count(*) from DefaultEntity"))
                .doesNotThrowAnyException();
        // Writes are not
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> defaultSession.persist(defaultEntity))
                .isInstanceOf(TransactionRequiredException.class)
                .hasMessageContaining(
                        "Transaction is not active, consider adding @Transactional to your method to automatically activate one");
    }

    @Test
    public void defaultEntityManagerNoRequestNoTransaction() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> defaultSession.persist(defaultEntity))
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
        usersSession.persist(user);

        User savedUser = usersSession.get(User.class, user.getId());
        assertEquals(user.getName(), savedUser.getName());
    }

    @Test
    @ActivateRequestContext
    public void usersEntityManagerInRequestNoTransaction() {
        // Reads are allowed
        assertThatCode(() -> usersSession.createQuery("select count(*) from User"))
                .doesNotThrowAnyException();
        // Writes are not
        User user = new User("gsmet");
        assertThatThrownBy(() -> usersSession.persist(user))
                .isInstanceOf(TransactionRequiredException.class)
                .hasMessageContaining(
                        "Transaction is not active, consider adding @Transactional to your method to automatically activate one");
    }

    @Test
    public void usersEntityManagerNoRequestNoTransaction() {
        User user = new User("gsmet");
        assertThatThrownBy(() -> usersSession.persist(user))
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
        inventorySession.persist(plane);

        Plane savedPlane = inventorySession.get(Plane.class, plane.getId());
        assertEquals(plane.getName(), savedPlane.getName());
    }

    @Test
    @ActivateRequestContext
    public void inventoryEntityManagerInRequestNoTransaction() {
        // Reads are allowed
        assertThatCode(() -> inventorySession.createQuery("select count(*) from Plane"))
                .doesNotThrowAnyException();
        // Writes are not
        Plane plane = new Plane("Airbus A380");
        assertThatThrownBy(() -> inventorySession.persist(plane))
                .isInstanceOf(TransactionRequiredException.class)
                .hasMessageContaining(
                        "Transaction is not active, consider adding @Transactional to your method to automatically activate one");
    }

    @Test
    public void inventoryEntityManagerNoRequestNoTransaction() {
        Plane plane = new Plane("Airbus A380");
        assertThatThrownBy(() -> inventorySession.persist(plane))
                .isInstanceOf(ContextNotActiveException.class)
                .hasMessageContainingAll(
                        "Cannot use the EntityManager/Session because neither a transaction nor a CDI request context is active",
                        "Consider adding @Transactional to your method to automatically activate a transaction",
                        "@ActivateRequestContext if you have valid reasons not to use transactions");
    }

    @Test
    @Transactional
    public void testUserInInventorySession() {
        User user = new User("gsmet");
        assertThatThrownBy(() -> inventorySession.persist(user)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown entity");
    }
}
