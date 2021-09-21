package io.quarkus.neo4j.runtime;

import java.util.Map;

import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.TransactionWork;
import org.neo4j.driver.Value;

final class JTAAwareSession implements Session {

    private final Session delegate;
    private final Transaction currentTransaction;

    JTAAwareSession(Session delegate) {

        this.delegate = delegate;
        this.currentTransaction = this.delegate.beginTransaction();
    }

    @Override
    public Transaction beginTransaction() {

        return this.beginTransaction(TransactionConfig.empty());
    }

    @Override
    public Transaction beginTransaction(TransactionConfig config) {

        throw new UnsupportedOperationException(
                "Unmanaged transactions are not supported in a managed (JTA) environment.");
    }

    @Override
    public <T> T readTransaction(TransactionWork<T> work) {

        return readTransaction(work, TransactionConfig.empty());
    }

    @Override
    public <T> T readTransaction(TransactionWork<T> work, TransactionConfig config) {

        throw new UnsupportedOperationException(
                "Transaction functions are not supported in a managed (JTA) environment.");
    }

    @Override
    public <T> T writeTransaction(TransactionWork<T> work) {

        return writeTransaction(work, TransactionConfig.empty());
    }

    @Override
    public <T> T writeTransaction(TransactionWork<T> work, TransactionConfig config) {

        throw new UnsupportedOperationException(
                "Transaction functions are not supported in a managed (JTA) environment.");
    }

    @Override
    public Result run(String query, TransactionConfig config) {

        throw new UnsupportedOperationException(
                "Cannot use a different transaction configuration in a managed (JTA) environment.");
    }

    @Override
    public Result run(String query, Map<String, Object> parameters, TransactionConfig config) {

        throw new UnsupportedOperationException(
                "Cannot use a different transaction configuration in a managed (JTA) environment.");
    }

    @Override
    public Result run(Query query, TransactionConfig config) {

        throw new UnsupportedOperationException(
                "Cannot use a different transaction configuration in a managed (JTA) environment.");
    }

    @Override
    public Bookmark lastBookmark() {

        return this.delegate.lastBookmark();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("A reset is not supported in a managed (JTA) environment.");
    }

    @Override
    public void close() {
        // NO-OP in a managed environment
    }

    @Override
    public Result run(String query, Value parameters) {
        return currentTransaction.run(query, parameters);
    }

    @Override
    public Result run(String query, Map<String, Object> parameters) {
        return currentTransaction.run(query, parameters);
    }

    @Override
    public Result run(String query, Record parameters) {
        return currentTransaction.run(query, parameters);
    }

    @Override
    public Result run(String query) {
        return currentTransaction.run(query);
    }

    @Override
    public Result run(Query query) {
        return currentTransaction.run(query);
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    void commitAndClose() {
        try {
            this.currentTransaction.commit();
        } finally {
            this.delegate.close();
        }
    }

    void rollbackAndClose() {
        try {
            this.currentTransaction.rollback();
        } finally {
            this.delegate.close();
        }
    }
}
