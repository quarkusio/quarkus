package io.quarkus.redis.datasource.stream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * The argument of the <a href="http://redis.io/commands/xclaim">XCLAIM</a> command.
 */
public class XClaimArgs implements RedisCommandExtraArguments {

    private Duration idle;

    private long time = -1;

    private int retryCount = -1;

    private boolean force;

    private boolean justId;

    private String lastId;

    /**
     * Set the idle time (last time it was delivered) of the message. If {@code IDLE} is not specified, an {@code IDLE}
     * of 0 is assumed, that is, the time count is reset because the message has now a new owner trying to process it.
     *
     * @param idle
     *        the idle duration, must not be {@code null}
     *
     * @return the current {@code XClaimArgs}
     */
    public XClaimArgs idle(Duration idle) {
        this.idle = idle;
        return this;
    }

    /**
     * This is the same as {@code IDLE} but instead of a relative amount of milliseconds, it sets the idle time to a
     * specific Unix time (in milliseconds). This is useful in order to rewrite the {@code AOF} file generating
     * {@code XCLAIM} commands.
     *
     * @param time
     *        the timestamp
     *
     * @return the current {@code XClaimArgs}
     */
    public XClaimArgs time(long time) {
        this.time = time;
        return this;
    }

    /**
     * Set the retry counter to the specified value. This counter is incremented every time a message is delivered
     * again. Normally {@code XCLAIM} does not alter this counter, which is just served to clients when the
     * {@code XPENDING} command is called: this way clients can detect anomalies, like messages that are never processed
     * for some reason after a big number of delivery attempts.
     *
     * @param retryCount
     *        the retry count, must be positive
     *
     * @return the current {@code XClaimArgs}
     */
    public XClaimArgs retryCount(int retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    /**
     * Creates the pending message entry in the PEL even if certain specified IDs are not already in the PEL assigned to
     * a different client. However, the message must exist in the stream, otherwise the IDs of non-existing messages are
     * ignored.
     *
     * @return the current {@code XClaimArgs}
     */
    public XClaimArgs force() {
        this.force = true;
        return this;
    }

    /**
     * In the returned structure, only set the IDs of messages successfully claimed, without returning the actual
     * message. Using this option means the retry counter is not incremented.
     *
     * @return the current {@code XClaimArgs}
     */
    public XClaimArgs justId() {
        this.justId = true;
        return this;
    }

    /**
     * Sets the last id of the message to claim.
     *
     * @param lastId
     *        the last id, must not be {@code null}
     *
     * @return the current {@code XClaimArgs}
     */
    public XClaimArgs lastId(String lastId) {
        this.lastId = lastId;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> args = new ArrayList<>();

        if (idle != null) {
            args.add("IDLE");
            args.add(Long.toString(idle.toMillis()));
            if (time > 0) {
                throw new IllegalStateException("Cannot combine `IDLE` and `TIME`");
            }
        }

        if (time > 0) {
            args.add("TIME");
            args.add(Long.toString(time));
        }

        if (retryCount > 0) {
            args.add("RETRYCOUNT");
            args.add(Integer.toString(retryCount));
        }

        if (force) {
            args.add("FORCE");
        }

        if (justId) {
            args.add("JUSTID");
        }

        if (lastId != null) {
            args.add("LASTID");
            args.add(lastId);
        }

        return args;
    }
}
