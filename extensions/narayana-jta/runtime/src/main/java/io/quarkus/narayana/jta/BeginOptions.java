package io.quarkus.narayana.jta;

/**
 * Builder interface to allow a transaction to be customized, including things like timeout and semantics when an
 * existing transaction is present.
 */
public class BeginOptions {

    boolean commitOnRequestScopeEnd;
    int timeout = 0;

    /**
     * If this method is called the transaction will be automatically committed when the request scope is destroyed,
     * instead of being rolled back.
     * <p>
     *
     * @return These options
     */
    public BeginOptions commitOnRequestScopeEnd() {
        commitOnRequestScopeEnd = true;
        return this;
    }

    /**
     * Sets the transaction timeout for transactions created by this builder. A value of zero refers to the system
     * default.
     *
     * @param seconds
     *        The timeout in seconds
     *
     * @return This builder
     *
     * @throws IllegalArgumentException
     *         If seconds is negative
     */
    public BeginOptions timeout(int seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds cannot be negative");
        }
        this.timeout = seconds;
        return this;
    }
}
