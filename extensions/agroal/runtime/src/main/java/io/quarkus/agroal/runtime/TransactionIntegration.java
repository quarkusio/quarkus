package io.quarkus.agroal.runtime;

public enum TransactionIntegration {

    /**
     * Integrate the JDBC Datasource with the JTA TransactionManager of Quarkus.
     * This is the default.
     */
    ENABLED,

    /**
     * Similarly to {@link #ENABLED}, also enables integration with the JTA
     * TransactionManager of Quarkus, but enabling XA transactions as well.
     * Requires a JDBC driver implementing {@link javax.sql.XADataSource}
     */
    XA,

    /**
     * Disables the Agroal integration with the Narayana TransactionManager.
     * This is typically a bad idea, and is only useful in special cases:
     * make sure to not use this without having a deep understanding of the
     * implications.
     */
    DISABLED
}
