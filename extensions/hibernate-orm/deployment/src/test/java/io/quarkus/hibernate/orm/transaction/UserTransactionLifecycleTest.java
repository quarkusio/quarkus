package io.quarkus.hibernate.orm.transaction;

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

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
        // FIXME: We expect double flushes in this case, but that's a bug
        return true;
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
