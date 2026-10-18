package io.quarkus.hibernate.orm.transaction;

import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

/**
 * Check transaction lifecycle, including session flushes and the closing of the session.
 */
public class TransactionAnnotationLifecycleTest extends AbstractTransactionLifecycleTest {

    @Inject
    TransactionAnnotationCRUD transactionAnnotationCRUD;

    @Override
    protected TestCRUD crud() {
        return transactionAnnotationCRUD;
    }

    @Override
    protected boolean expectDoubleFlush() {
        return false;
    }

    @ApplicationScoped
    public static class TransactionAnnotationCRUD extends TestCRUD {
        @Inject
        EntityManager entityManager;

        @Override
        @Transactional
        public ValueAndExecutionMetadata<Void> create(long id, String name) {
            return super.create(id, name);
        }

        @Override
        @Transactional
        public ValueAndExecutionMetadata<String> retrieve(long id) {
            return super.retrieve(id);
        }

        @Override
        @Transactional
        public ValueAndExecutionMetadata<String> callStoredProcedure(long id) {
            return super.callStoredProcedure(id);
        }

        @Override
        @Transactional
        public ValueAndExecutionMetadata<Void> update(long id, String name) {
            return super.update(id, name);
        }

        @Override
        @Transactional
        public ValueAndExecutionMetadata<Void> delete(long id) {
            return super.delete(id);
        }

        @Override
        public <T> ValueAndExecutionMetadata<T> inTransaction(Function<EntityManager, T> action) {
            // We should already be in a transaction
            return ValueAndExecutionMetadata.run(entityManager, action);
        }
    }

}
