package io.quarkus.mongodb.panache.common.transaction;

public class MongoTransactionException extends RuntimeException {
    public MongoTransactionException(Exception cause) {
        super(cause);
    }
}
