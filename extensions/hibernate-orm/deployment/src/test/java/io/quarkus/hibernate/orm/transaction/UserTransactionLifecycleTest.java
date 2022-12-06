package io.quarkus.hibernate.orm.transaction;

import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

/**
 * Check transaction lifecycle, including session flushes and the closing of the session.
 */
public class UserTransactionLifecycleTest extends AbstractTransactionLifecycleTest {

    @Inject
    UserTransactionCRUD userTransactionCRUD;

    @Override
    protected TestCRUD crud() {
        return userTransactionCRUD;
    }

    @Override
    protected boolean expectDoubleFlush() {
        return false;
    }

    @ApplicationScoped
    public static class UserTransactionCRUD extends TestCRUD {
        @Inject
        EntityManager entityManager;
        @Inject
        UserTransaction userTransaction;

        @Override
        public <T> ValueAndExecutionMetadata<T> inTransaction(Function<EntityManager, T> action) {
            try {
                userTransaction.begin();
                ValueAndExecutionMetadata<T> result;
                try {
                    result = ValueAndExecutionMetadata.run(entityManager, action);
                } catch (Exception e) {
                    try {
                        userTransaction.rollback();
                    } catch (Exception e2) {
                        e.addSuppressed(e2);
                    }
                    throw e;
                }
                userTransaction.commit();
                return result;
            } catch (NotSupportedException | SystemException | RollbackException | HeuristicMixedException
                    | HeuristicRollbackException e) {
                throw new IllegalStateException("Unexpected exception: " + e.getMessage(), e);
            }
        }
    }

}
