package io.quarkus.hibernate.orm.transaction;

import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

public class GetTransactionLifecycleTest extends AbstractTransactionLifecycleTest {

    @Inject
    GetTransactionCRUD getTransactionCRUD;

    @Override
    protected TestCRUD crud() {
        return getTransactionCRUD;
    }

    @Override
    protected boolean expectDoubleFlush() {
        // We expect double flushes in this case because EntityTransaction.commit() triggers a flush,
        // and then the transaction synchronization will also trigger a flush before transaction completion.
        // This may be a bug in ORM, but in any case there's nothing we can do about it.
        return true;
    }

    @ApplicationScoped
    public static class GetTransactionCRUD extends TestCRUD {
        @Inject
        EntityManagerFactory entityManagerFactory;

        @Override
        public <T> ValueAndExecutionMetadata<T> inTransaction(Function<EntityManager, T> action) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            try (AutoCloseable closeable = entityManager::close) {
                EntityTransaction tx = entityManager.getTransaction();
                tx.begin();
                ValueAndExecutionMetadata<T> result;
                try {
                    result = ValueAndExecutionMetadata.run(entityManager, action);
                } catch (Exception e) {
                    try {
                        tx.rollback();
                    } catch (Exception e2) {
                        e.addSuppressed(e2);
                    }
                    throw e;
                }
                tx.commit();
                return result;
            } catch (Exception e) {
                throw new IllegalStateException("Unexpected exception: " + e.getMessage(), e);
            }
        }
    }

}
