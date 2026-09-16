package io.quarkus.narayana.observers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionScoped;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * A transaction scoped bean that is only instantiated by its own {@code @BeforeDestroyed(TransactionScoped.class)}
 * observer must be notified when the transaction is rolled back, without an error being logged.
 */
public class TransactionScopedObserverOnRollbackTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TransactionalBean.class, RollbackObserver.class))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertTrue(
                    records.stream().noneMatch(record -> record.getMessage().contains("@BeforeDestroyed")),
                    () -> "Unexpected log records: " + records.stream().map(LogRecord::getMessage).toList()));

    @Inject
    TransactionalBean bean;

    @Test
    public void observerNotifiedOnRollback() {
        RollbackObserver.NOTIFIED = 0;

        assertThrows(IllegalStateException.class, bean::fail);

        assertEquals(1, RollbackObserver.NOTIFIED);
    }

    @ApplicationScoped
    static class TransactionalBean {

        @Transactional
        void fail() {
            throw new IllegalStateException("Rollback here");
        }
    }

    @TransactionScoped
    static class RollbackObserver {

        static volatile int NOTIFIED = 0;

        void beforeDestroyed(@Observes @BeforeDestroyed(TransactionScoped.class) Object event) {
            NOTIFIED++;
        }
    }
}
