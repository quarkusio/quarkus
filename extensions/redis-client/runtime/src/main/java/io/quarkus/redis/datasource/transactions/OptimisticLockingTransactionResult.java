package io.quarkus.redis.datasource.transactions;

/**
 * A structure holding the result of the commands executed in a transaction. Note that the result are ordered, and the
 * (0-based) index of the command must be used to retrieve the result of a specific command. In addition, it provides
 * the rerult from the pre-transaction block.
 */
public interface OptimisticLockingTransactionResult<I> extends TransactionResult {

    /**
     * Retrieves the result from the pre-transaction block
     *
     * @return the value produces by the pre-transaction block
     */
    I getPreTransactionResult();

}
