package io.quarkus.agroal.runtime;

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation;

public enum TransactionIsolationLevel {
    UNDEFINED(TransactionIsolation.UNDEFINED),
    NONE(TransactionIsolation.NONE),
    READ_UNCOMMITTED(TransactionIsolation.READ_UNCOMMITTED),
    READ_COMMITTED(TransactionIsolation.READ_COMMITTED),
    REPEATABLE_READ(TransactionIsolation.REPEATABLE_READ),
    SERIALIZABLE(TransactionIsolation.SERIALIZABLE);

    TransactionIsolation jdbcTransactionIsolationLevel;

    TransactionIsolationLevel(TransactionIsolation jdbcTransactionIsolationLevel) {
        this.jdbcTransactionIsolationLevel = jdbcTransactionIsolationLevel;
    }

    public static TransactionIsolationLevel of(String value) {
        switch (value) {
            case "none":
                return NONE;
            case "read-committed":
                return READ_COMMITTED;
            case "read-uncommitted":
                return READ_UNCOMMITTED;
            case "repeatable-read":
                return REPEATABLE_READ;
            case "serializable":
                return SERIALIZABLE;
            default:
                return UNDEFINED;
        }
    }
}