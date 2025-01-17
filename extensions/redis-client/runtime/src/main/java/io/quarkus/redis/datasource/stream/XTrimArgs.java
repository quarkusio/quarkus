package io.quarkus.redis.datasource.stream;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * The argument of the <a href="http://redis.io/commands/xtrim">XTRIM</a> command.
 */
public class XTrimArgs implements RedisCommandExtraArguments {

    private long maxlen = -1;

    private boolean approximateTrimming;

    private String minid;

    private long limit = -1;

    /**
     * Sets the max length of the stream.
     *
     * @param maxlen the max length of the stream, must be positive
     * @return the current {@code XAddArgs}
     */
    public XTrimArgs maxlen(long maxlen) {
        this.maxlen = maxlen;
        return this;
    }

    /**
     * When set, prefix the {@link #maxlen} with {@code ~} to enable the <em>almost exact trimming</em>.
     * This is recommended when using {@link #maxlen(long)}.
     *
     * @return the current {@code XAddArgs}
     */
    public XTrimArgs nearlyExactTrimming() {
        this.approximateTrimming = true;
        return this;
    }

    /**
     * Evicts entries from the stream having IDs lower to the specified one.
     *
     * @param minid the min id, must not be {@code null}, must be a valid stream id
     * @return the current {@code XAddArgs}
     */
    public XTrimArgs minid(String minid) {
        this.minid = minid;
        return this;
    }

    /**
     * Sets the maximum entries that can get evicted.
     *
     * @param limit the limit, must be positive
     * @return the current {@code XAddArgs}
     */
    public XTrimArgs limit(long limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> args = new ArrayList<>();

        if (maxlen >= 0) {
            if (minid != null) {
                throw new IllegalArgumentException("Cannot use `MAXLEN` and `MINID` together");
            }

            args.add("MAXLEN");
            if (approximateTrimming) {
                args.add("~");
            } else {
                args.add("=");
            }
            args.add(Long.toString(maxlen));
        }

        if (minid != null) {
            args.add("MINID");
            if (approximateTrimming) {
                args.add("~");
            } else {
                args.add("=");
            }
            args.add(minid);
        }

        if (limit > 0) {
            if (!approximateTrimming) {
                throw new IllegalArgumentException("Cannot set the eviction limit when using exact trimming");
            }
            args.add("LIMIT");
            args.add(Long.toString(limit));
        }

        return args;
    }
}
