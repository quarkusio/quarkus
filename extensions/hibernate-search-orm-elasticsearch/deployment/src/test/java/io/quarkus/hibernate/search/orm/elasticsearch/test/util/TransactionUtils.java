package io.quarkus.hibernate.search.orm.elasticsearch.test.util;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.UserTransaction;

public class TransactionUtils {
    private TransactionUtils() {
    }

    public static void inTransaction(UserTransaction transaction, Runnable runnable) {
        try {
            transaction.begin();
            try {
                runnable.run();
                transaction.commit();
            } catch (Throwable t) {
                try {
                    transaction.rollback();
                } catch (Throwable t2) {
                    t.addSuppressed(t2);
                }
                throw t;
            }
        } catch (SystemException | NotSupportedException | RollbackException | HeuristicMixedException
                | HeuristicRollbackException e) {
            throw new IllegalStateException("Transaction exception", e);
        }
    }
}
