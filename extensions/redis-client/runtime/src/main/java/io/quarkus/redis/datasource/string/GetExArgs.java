package io.quarkus.redis.datasource.string;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Argument list for the Redis <a href="https://redis.io/commands/getex">GETEX</a> command.
 *
 * @deprecated use {@link io.quarkus.redis.datasource.value.GetExArgs} instead
 */
@Deprecated
public class GetExArgs extends io.quarkus.redis.datasource.value.GetExArgs implements RedisCommandExtraArguments {

    private long ex = -1;
    private long exAt = -1;
    private long px = -1;
    private long pxAt = -1;
    private boolean persist;

    /**
     * Set the specified expire time, in seconds.
     *
     * @param timeout expire time in seconds.
     * @return the current {@code GetExArgs}
     */
    @Override
    public GetExArgs ex(long timeout) {
        this.ex = timeout;
        return this;
    }

    /**
     * Sets the expiration.
     *
     * @param timeout expire time in seconds.
     * @return the current {@code GetExArgs}
     */
    @Override
    public GetExArgs ex(Duration timeout) {
        if (timeout == null) {
            throw new IllegalArgumentException("`timeout` must not be `null`");
        }
        return ex(timeout.toMillis() / 1000);
    }

    /**
     * Sets the expiration time
     *
     * @param timestamp the timestamp
     * @return the current {@code GetExArgs}
     */
    @Override
    public GetExArgs exAt(long timestamp) {
        this.exAt = timestamp;
        return this;
    }

    /**
     * Sets the expiration time
     *
     * @param timestamp the timestamp type: posix time in seconds.
     * @return the current {@code GetExArgs}
     */
    @Override
    public GetExArgs exAt(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("`timestamp` must not be `null`");
        }
        exAt(timestamp.toEpochMilli() / 1000);
        return this;
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param timeout expire time in milliseconds.
     * @return the current {@code GetExArgs}
     */
    @Override
    public GetExArgs px(long timeout) {
        this.px = timeout;
        return this;
    }

    /**
     * Set the specified expire time, in milliseconds.
     *
     * @param timeout expire time in milliseconds.
     * @return the current {@code GetExArgs}
     */
    @Override
    public GetExArgs px(Duration timeout) {
        if (timeout == null) {
            throw new IllegalArgumentException("`timeout` must not be `null`");
        }
        return px(timeout.toMillis());
    }

    /**
     * Set the specified Unix time at which the key will expire, in milliseconds.
     *
     * @param timestamp the timestamp
     * @return the current {@code GetExArgs}
     */
    @Override
    public GetExArgs pxAt(long timestamp) {
        this.pxAt = timestamp;
        return this;
    }

    /**
     * Set the specified Unix time at which the key will expire, in milliseconds.
     *
     * @param timestamp the timestamp
     * @return the current {@code GetExArgs}
     */
    @Override
    public GetExArgs pxAt(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("`timestamp` must not be `null`");
        }
        return pxAt(timestamp.toEpochMilli());
    }

    /**
     * Sets {@code PERSIST}
     *
     * @return the current {@code GetExArgs}
     */
    @Override
    public GetExArgs persist() {
        this.persist = true;
        return this;
    }

    @Override
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
