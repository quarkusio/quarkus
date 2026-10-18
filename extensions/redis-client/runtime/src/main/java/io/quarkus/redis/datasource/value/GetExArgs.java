package io.quarkus.redis.datasource.value;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Argument list for the Redis <a href="https://redis.io/commands/getex">GETEX</a> command.
 */
public class GetExArgs implements RedisCommandExtraArguments {

    private long ex = -1;
    private long exAt = -1;
    private long px = -1;
    private long pxAt = -1;
    private boolean persist;

    /**
     * Set the expiration timeout, in seconds.
     *
     * @param timeout expiration timeout in seconds
     * @return the current {@code GetExArgs}
     */
    public GetExArgs ex(long timeout) {
        this.ex = timeout;
        return this;
    }

    /**
     * Set the expiration timeout, in seconds.
     *
     * @param timeout expiration timeout in seconds
     * @return the current {@code GetExArgs}
     */
    public GetExArgs ex(Duration timeout) {
        if (timeout == null) {
            throw new IllegalArgumentException("`timeout` must not be `null`");
        }
        return ex(timeout.toSeconds());
    }

    /**
     * Set the expiration timestamp as a number of seconds since the Unix epoch.
     *
     * @param timestamp the timestamp
     * @return the current {@code GetExArgs}
     */
    public GetExArgs exAt(long timestamp) {
        this.exAt = timestamp;
        return this;
    }

    /**
     * Set the expiration timestamp as a number of seconds since the Unix epoch.
     *
     * @param timestamp the timestamp
     * @return the current {@code GetExArgs}
     */
    public GetExArgs exAt(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("`timestamp` must not be `null`");
        }
        exAt(timestamp.toEpochMilli() / 1000);
        return this;
    }

    /**
     * Set the expiration timeout, in milliseconds.
     *
     * @param timeout expiration timeout in milliseconds
     * @return the current {@code GetExArgs}
     */
    public GetExArgs px(long timeout) {
        this.px = timeout;
        return this;
    }

    /**
     * Set the expiration timeout, in milliseconds.
     *
     * @param timeout expiration timeout in milliseconds
     * @return the current {@code GetExArgs}
     */
    public GetExArgs px(Duration timeout) {
        if (timeout == null) {
            throw new IllegalArgumentException("`timeout` must not be `null`");
        }
        return px(timeout.toMillis());
    }

    /**
     * Set the expiration timestamp as a number of milliseconds since the Unix epoch.
     *
     * @param timestamp the timestamp
     * @return the current {@code GetExArgs}
     */
    public GetExArgs pxAt(long timestamp) {
        this.pxAt = timestamp;
        return this;
    }

    /**
     * Set the expiration timestamp as a number of milliseconds since the Unix epoch.
     *
     * @param timestamp the timestamp
     * @return the current {@code GetExArgs}
     */
    public GetExArgs pxAt(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("`timestamp` must not be `null`");
        }
        return pxAt(timestamp.toEpochMilli());
    }

    /**
     * Set {@code PERSIST}.
     *
     * @return the current {@code GetExArgs}
     */
    public GetExArgs persist() {
        this.persist = true;
        return this;
    }

    public List<Object> toArgs() {
        List<Object> args = new ArrayList<>();
        if (ex >= 0) {
            args.add("EX");
            args.add(Long.toString(ex));
        }

        if (exAt >= 0) {
            args.add("EXAT");
            args.add(Long.toString(exAt));
        }

        if (px >= 0) {
            args.add("PX");
            args.add(Long.toString(px));
        }

        if (pxAt >= 0) {
            args.add("PXAT");
            args.add(Long.toString(pxAt));
        }

        if (persist) {
            args.add("PERSIST");
        }
        return args;
    }

}
