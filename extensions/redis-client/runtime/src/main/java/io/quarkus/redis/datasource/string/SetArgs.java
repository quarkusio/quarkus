package io.quarkus.redis.datasource.string;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Argument list for the Redis <a href="https://redis.io/commands/SET">SET</a> command.
 *
 * @deprecated Use {@link io.quarkus.redis.datasource.value.SetArgs} instead.
 */
@Deprecated
public class SetArgs extends io.quarkus.redis.datasource.value.SetArgs implements RedisCommandExtraArguments {

    private long ex = -1;
    private long exAt = -1;
    private long px = -1;
    private long pxAt = -1;
    private boolean nx;
    private boolean keepttl;
    private boolean xx;
    private boolean get;

    /**
     * Set the specified expire time, in seconds.
     *
     * @param timeout expire time in seconds.
     * @return the current {@code GetExArgs}
     */
    @Override
    public SetArgs ex(long timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("`timeout` must be positive");
        }
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
    public SetArgs ex(Duration timeout) {
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
    public SetArgs exAt(long timestamp) {
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
    public SetArgs exAt(Instant timestamp) {
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
    public SetArgs px(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("`timeout` must be positive");
        }
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
    public SetArgs px(Duration timeout) {
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
    public SetArgs pxAt(long timestamp) {
        this.pxAt = timestamp;
        return this;
    }

    /**
     * Set the specified Unix time at which the key will expire, in milliseconds.
     *
     * @param timestamp the timestamp
     * @return the current {@code SetArgs}
     */
    @Override
    public SetArgs pxAt(Instant timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("`timestamp` must not be `null`");
        }
        return pxAt(timestamp.toEpochMilli());
    }

    /**
     * Only set the key if it does not already exist.
     *
     * @return the current {@code SetArgs}
     */
    @Override
    public SetArgs nx() {
        this.nx = true;
        return this;
    }

    /**
     * Set the value and retain the existing TTL.
     *
     * @return the current {@code SetArgs}
     */
    @Override
    public SetArgs keepttl() {
        this.keepttl = true;
        return this;
    }

    /**
     * Only set the key if it already exists.
     *
     * @return the current {@code SetArgs}
     */
    @Override
    public SetArgs xx() {
        this.xx = true;
        return this;
    }

    /**
     * Return the old string stored at key, or nil if key did not exist. An error is returned and SET aborted if the
     * value stored at key is not a string.
     *
     * @return the current {@code SetArgs}
     */
    @Override
    public SetArgs get() {
        this.get = true;
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

        if (nx) {
            args.add("NX");
        }

        if (xx) {
            args.add("XX");
        }

        if (keepttl) {
            args.add("KEEPTTL");
        }

        if (get) {
            args.add("GET");
        }
        return args;
    }

}
