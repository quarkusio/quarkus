package io.quarkus.redis.datasource.transactions;

/**
 * A structure holding the result of the commands executed in a transaction. Note that the result are ordered, and the
 * (0-based) index of the command must be used to retrieve the result of a specific command.
 */
public interface TransactionResult extends Iterable<Object> {

    /**
     * Checks if the transaction was discarded (rolled back)
     *
     * @return {@code true} if the transaction batch was discarded.
     */
    boolean discarded();

    /**
     * Checks if at least one command in the transaction ended with an error.
     * Errors are represented as {@code Throwable}s in the list of results.
     *
     * @return {@code true} if at least one command in the transaction ended with an error
     */
    boolean hasErrors();

    /**
     * Returns the number of responses.
     *
     * @return the number of responses
     */
    int size();

    /**
     * Returns {@code true} if this {@link TransactionResult} contains no responses.
     *
     * @return {@code true} if this {@link TransactionResult} contains no responses
     */
    boolean isEmpty();

    /**
     * Returns the response at the specified position in this {@link TransactionResult}.
     * It contains the result of the commands executed at the same position in the transaction.
     * If that command ended with an error, the return value is a {@link Throwable}.
     *
     * @param index index of the element to return
     * @param <T> the expected type
     * @return the element at the specified position in this {@link TransactionResult}
     */
    <T> T get(int index);

}
