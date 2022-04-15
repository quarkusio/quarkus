package io.quarkus.hibernate.search.elasticsearch.test.util;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

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
