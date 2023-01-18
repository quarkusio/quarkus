package io.quarkus.hibernate.orm.lazyloading;

import static io.quarkus.hibernate.orm.TransactionTestUtils.inTransaction;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

import org.junit.jupiter.api.Test;

public abstract class AbstractLazyBasicTest {

    @Inject
    EntityManager em;

    @Inject
    UserTransaction transaction;

    private final AccessDelegate delegate;
    private Long entityId;

    public AbstractLazyBasicTest(AccessDelegate delegate) {
        this.delegate = delegate;
    }

    @Test
    public void update_all_nullToNonNull() {
        initNull();
        inTransaction(transaction, () -> {
            delegate.updateAllProperties(em, entityId, "updated1", "updated2", "updated3");
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "updated1", "updated2", "updated3");
        });
    }

    @Test
    public void update_allLazy_nullToNonNull() {
        initNull();
        inTransaction(transaction, () -> {
            delegate.updateAllLazyProperties(em, entityId, "updated1", "updated2");
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, "updated1", "updated2");
        });
    }

    @Test
    public void update_oneEager_nullToNonNull() {
        initNull();
        inTransaction(transaction, () -> {
            delegate.updateOneEagerProperty(em, entityId, "updated1");
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "updated1", null, null);
        });
    }

    @Test
    public void update_oneLazy_nullToNonNull() {
        initNull();
        inTransaction(transaction, () -> {
            delegate.updateOneLazyProperty(em, entityId, "updated2");
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, "updated2", null);
        });
    }

    @Test
    public void update_all_nonNullToNonNull() {
        initNonNull();
        inTransaction(transaction, () -> {
            delegate.updateAllProperties(em, entityId, "updated1", "updated2", "updated3");
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "updated1", "updated2", "updated3");
        });
    }

    @Test
    public void update_allLazy_nonNullToNonNull() {
        initNonNull();
        inTransaction(transaction, () -> {
            delegate.updateAllLazyProperties(em, entityId, "updated1", "updated2");
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", "updated1", "updated2");
        });
    }

    @Test
    public void update_oneEager_nonNullToNonNull() {
        initNonNull();
        inTransaction(transaction, () -> {
            delegate.updateOneEagerProperty(em, entityId, "updated1");
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "updated1", "initial2", "initial3");
        });
    }

    @Test
    public void update_oneLazy_nonNullToNonNull() {
        initNonNull();
        inTransaction(transaction, () -> {
            delegate.updateOneLazyProperty(em, entityId, "updated2");
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", "updated2", "initial3");
        });
    }

    @Test
    public void update_all_nonNullToNull() {
        initNonNull();
        inTransaction(transaction, () -> {
            delegate.updateAllProperties(em, entityId, null, null, null);
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, null, null);
        });
    }

    @Test
    public void update_allLazy_nonNullToNull() {
        initNonNull();
        inTransaction(transaction, () -> {
            delegate.updateAllLazyProperties(em, entityId, null, null);
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", null, null);
        });
    }

    @Test
    public void update_oneEager_nonNullToNull() {
        initNonNull();
        inTransaction(transaction, () -> {
            delegate.updateOneEagerProperty(em, entityId, null);
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, "initial2", "initial3");
        });
    }

    @Test
    public void update_oneLazy_nonNullToNull() {
        initNonNull();
        inTransaction(transaction, () -> {
            delegate.updateOneLazyProperty(em, entityId, null);
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", null, "initial3");
        });
    }

    private void initNull() {
        inTransaction(transaction, () -> {
            entityId = delegate.create(em, null, null, null);
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, null, null, null);
        });
    }

    private void initNonNull() {
        inTransaction(transaction, () -> {
            entityId = delegate.create(em, "initial1", "initial2", "initial3");
        });
        inTransaction(transaction, () -> {
            delegate.testLazyLoadingAndPersistedValues(em, entityId, "initial1", "initial2", "initial3");
        });
    }

    /**
     * An interface for delegate classes,
     * classes whose bytecode is transformed by Quarkus to replace public field access with getter/setter access.
     * <p>
     * (Test bytecode was not transformed by Quarkus when using QuarkusUnitTest last time I checked).
     */
    interface AccessDelegate {

        long create(EntityManager entityManager, String eagerProperty1, String lazyProperty1, String lazyProperty2);

        void updateAllProperties(EntityManager entityManager, long entityId, String eagerProperty1, String lazyProperty1,
                String lazyProperty2);

        void updateAllLazyProperties(EntityManager entityManager, long entityId, String lazyProperty1, String lazyProperty2);

        void updateOneEagerProperty(EntityManager entityManager, long entityId, String eagerProperty1);

        void updateOneLazyProperty(EntityManager entityManager, long entityId, String lazyProperty1);

        void testLazyLoadingAndPersistedValues(EntityManager entityManager, long entityId,
                String expectedEagerProperty1,
                String expectedLazyProperty1,
                String expectedLazyProperty2);
    }
}
