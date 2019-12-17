package io.quarkus.mongodb.panache.transaction;

public class MongoTransactionException extends RuntimeException {
    public MongoTransactionException(Exception cause) {
        super(cause);
    }
}
