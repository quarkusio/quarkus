package io.quarkus.narayana.observers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionScoped;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class TransactionScopedObserverOnRollbackTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TransactionalBean.class, RollbackObserver.class, LateBean.class))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(records -> assertTrue(
                    records.stream().noneMatch(record -> record.getMessage().contains("@BeforeDestroyed")),
                    () -> "Unexpected log records: " + records.stream().map(LogRecord::getMessage).toList()));

    @Inject
    TransactionalBean bean;

    @Test
    public void observerNotifiedAndDestroyedOnRollback() {
        RollbackObserver.NOTIFIED = 0;
        RollbackObserver.DESTROYED = 0;

        assertThrows(IllegalStateException.class, bean::fail);

        assertEquals(1, RollbackObserver.NOTIFIED);
        assertEquals(1, RollbackObserver.DESTROYED);
    }

    @Test
    public void beanCreatedAfterSetRollbackOnlyIsDestroyed() throws SystemException {
        LateBean.DESTROYED = 0;

        bean.markRollbackOnlyThenUseLateBean();

        assertEquals(1, LateBean.DESTROYED);
    }

    @ApplicationScoped
    static class TransactionalBean {

        @Inject
        TransactionManager transactionManager;

        @Inject
        LateBean lateBean;

        @Transactional
        void fail() {
            throw new IllegalStateException("Rollback here");
        }

        @Transactional
        void markRollbackOnlyThenUseLateBean() throws SystemException {
            transactionManager.setRollbackOnly();
            lateBean.touch();
        }
    }

    @TransactionScoped
    static class RollbackObserver {

        static volatile int NOTIFIED = 0;
        static volatile int DESTROYED = 0;

        void beforeDestroyed(@Observes @BeforeDestroyed(TransactionScoped.class) Object event) {
            NOTIFIED++;
        }

        @PreDestroy
        void destroyed() {
            DESTROYED++;
        }
    }

    @TransactionScoped
    static class LateBean {

        static volatile int DESTROYED = 0;

        void touch() {
        }

        @PreDestroy
        void destroyed() {
            DESTROYED++;
        }
    }
}
