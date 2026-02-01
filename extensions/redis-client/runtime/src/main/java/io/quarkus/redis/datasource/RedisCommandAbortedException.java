package io.quarkus.redis.datasource;

/**
 * Indicates a Redis command was aborted due to unmet preconditions (for example, a conditional {@code SET}
 * using {@code NX}/{@code XX} returning a nil reply).
 */
public class RedisCommandAbortedException extends RuntimeException {

    public RedisCommandAbortedException(String message) {
        super(message);
    }
}
