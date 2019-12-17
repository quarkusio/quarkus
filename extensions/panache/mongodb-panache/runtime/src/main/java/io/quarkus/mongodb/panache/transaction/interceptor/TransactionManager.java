package io.quarkus.mongodb.panache.transaction.interceptor;

import java.util.Objects;
import java.util.Optional;

public class TransactionManager {
    private final static ThreadLocal<Transaction> HOLDER = new ThreadLocal<>();

    static void setTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction);
        HOLDER.set(transaction);
    }

    public static Optional<Transaction> getTransaction() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static boolean activeTransaction() {
        return HOLDER.get() != null;
    }

    static void clear() {
        HOLDER.remove();
    }
}
