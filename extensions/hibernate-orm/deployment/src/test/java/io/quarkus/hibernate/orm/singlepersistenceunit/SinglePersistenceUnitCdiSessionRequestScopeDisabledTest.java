package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class SinglePersistenceUnitCdiSessionRequestScopeDisabledTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"))
            .overrideConfigKey("quarkus.hibernate-orm.request-scoped.enabled", "false");

    @Inject
    Session session;

    @Test
    @Transactional
    public void inTransaction() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        session.persist(defaultEntity);

        DefaultEntity savedDefaultEntity = session.get(DefaultEntity.class, defaultEntity.getId());
        assertEquals(defaultEntity.getName(), savedDefaultEntity.getName());
    }

    @Test
    @ActivateRequestContext
    public void inRequestNoTransaction() {
        // With request-scoped disabled, both reads and writes fail
        assertThatThrownBy(() -> session.createQuery("select count(*) from DefaultEntity"))
                .hasMessageContaining("Cannot use the EntityManager/Session because no transaction is active");
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> session.persist(defaultEntity))
                .hasMessageContaining("Cannot use the EntityManager/Session because no transaction is active");
    }

    @Test
    public void noRequestNoTransaction() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> session.persist(defaultEntity))
                .isInstanceOf(ContextNotActiveException.class)
                .hasMessageContainingAll(
                        "Cannot use the EntityManager/Session because no transaction is active",
                        "Consider adding @Transactional to your method to automatically activate a transaction",
                        "set 'quarkus.hibernate-orm.request-scoped.enabled' to 'true' if you have valid reasons not to use transactions");
    }

}
