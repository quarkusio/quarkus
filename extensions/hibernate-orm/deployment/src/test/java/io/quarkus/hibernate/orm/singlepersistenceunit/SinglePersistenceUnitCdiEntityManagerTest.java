package io.quarkus.hibernate.orm.singlepersistenceunit;

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

import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitCdiEntityManagerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void inTransaction() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        entityManager.persist(defaultEntity);

        DefaultEntity savedDefaultEntity = entityManager.find(DefaultEntity.class, defaultEntity.getId());
        assertEquals(defaultEntity.getName(), savedDefaultEntity.getName());
    }

    @Test
    @ActivateRequestContext
    public void inRequestNoTransaction() {
        // Reads are allowed
        assertThatCode(() -> entityManager.createQuery("select count(*) from DefaultEntity"))
                .doesNotThrowAnyException();
        // Writes are not
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> entityManager.persist(defaultEntity))
                .isInstanceOf(TransactionRequiredException.class)
                .hasMessageContaining(
                        "Transaction is not active, consider adding @Transactional to your method to automatically activate one");
    }

    @Test
    public void noRequestNoTransaction() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> entityManager.persist(defaultEntity))
                .isInstanceOf(ContextNotActiveException.class)
                .hasMessageContainingAll(
                        "Cannot use the EntityManager/Session because neither a transaction nor a CDI request context is active",
                        "Consider adding @Transactional to your method to automatically activate a transaction",
                        "@ActivateRequestContext if you have valid reasons not to use transactions");
    }

}
