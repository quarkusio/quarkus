package io.quarkus.hibernate.orm.singlepersistenceunit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.persistence.TransactionRequiredException;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.runtime.RequestScopedSessionHolder;
import io.quarkus.test.QuarkusUnitTest;

public class SinglePersistenceUnitCdiSessionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DefaultEntity.class)
                    .addAsResource("application.properties"));

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
        // Reads are allowed
        assertThatCode(() -> session.createQuery("select count(*) from DefaultEntity"))
                .doesNotThrowAnyException();
        // Writes are not
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> session.persist(defaultEntity))
                .isInstanceOf(TransactionRequiredException.class)
                .hasMessageContaining(
                        "Transaction is not active, consider adding @Transactional to your method to automatically activate one");
    }

    @Test
    public void noRequestNoTransaction() {
        DefaultEntity defaultEntity = new DefaultEntity("default");
        assertThatThrownBy(() -> session.persist(defaultEntity))
                .isInstanceOf(ContextNotActiveException.class)
                .hasMessageContainingAll("RequestScoped context was not active",
                        RequestScopedSessionHolder.class.getName());
    }

}
