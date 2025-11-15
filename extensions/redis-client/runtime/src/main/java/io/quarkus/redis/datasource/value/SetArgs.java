package io.quarkus.redis.datasource.value;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Argument list for the Redis <a href="https://redis.io/commands/SET">SET</a> command.
 */
public class SetArgs implements RedisCommandExtraArguments {

    private long ex = -1;
    private long exAt = -1;
    private long px = -1;
    private long pxAt = -1;
    private boolean nx;
    private boolean keepttl;
    private boolean xx;
    private boolean get;

    /**
     * Set the expiration timeout, in seconds.
     *
     * @param timeout expiration timeout in seconds
     * @return the current {@code SetArgs}
     */
    public SetArgs ex(long timeout) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("`timeout` must be positive");
        }
        this.ex = timeout;
        return this;
    }

    /**
     * Set the expiration timeout, in seconds.
     *
     * @param timeout expiration timeout in seconds
     * @return the current {@code SetArgs}
     */
    public SetArgs ex(Duration timeout) {
        if (timeout == null) {
            throw new IllegalArgumentException("`timeout` must not be `null`");
        }
        return ex(timeout.toSeconds());
    }

    /**
     * Set the expiration timestamp as a number of seconds since the Unix epoch.
     *
     * @param timestamp the timestamp
     * @return the current {@code SetArgs}
     */
    public SetArgs exAt(long timestamp) {
        this.exAt = timestamp;
        return this;
    }

    /**
     * Set the expiration timestamp as a number of seconds since the Unix epoch.
     *
     * @param timestamp the timestamp
     * @return the current {@code SetArgs}
     */
    public SetArgs exAt(Instant timestamp) {
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
     * @return the current {@code SetArgs}
     */
    public SetArgs px(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("`timeout` must be positive");
        }
        this.px = timeout;
        return this;
    }

    /**
     * Set the expiration timeout, in milliseconds.
     *
     * @param timeout expiration timeout in milliseconds
     * @return the current {@code SetArgs}
     */
    public SetArgs px(Duration timeout) {
        if (timeout == null) {
            throw new IllegalArgumentException("`timeout` must not be `null`");
        }
        return px(timeout.toMillis());
    }

    /**
     * Set the expiration timestamp as a number of milliseconds since the Unix epoch.
     *
     * @param timestamp the timestamp
     * @return the current {@code SetArgs}
     */
    public SetArgs pxAt(long timestamp) {
        this.pxAt = timestamp;
        return this;
    }

    /**
     * Set the expiration timestamp as a number of milliseconds since the Unix epoch.
     *
     * @param timestamp the timestamp
     * @return the current {@code SetArgs}
     */
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
    public SetArgs nx() {
        this.nx = true;
        return this;
    }

    /**
     * Set the value and retain the existing TTL.
     *
     * @return the current {@code SetArgs}
     */
    public SetArgs keepttl() {
        this.keepttl = true;
        return this;
    }

    /**
     * Only set the key if it already exists.
     *
     * @return the current {@code SetArgs}
     */
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
    public SetArgs get() {
        this.get = true;
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
