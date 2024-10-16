package io.quarkus.redis.datasource.stream;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represents the extra parameter of the <a href="https://redis.io/commands/xreadgroup/>XREADGROUP</a> command.
 */
public class XReadGroupArgs implements RedisCommandExtraArguments {

    private int count = -1;

    private Duration block;

    private boolean noack;

    /**
     * Sets the max number of entries per stream to return
     *
     * @param count the count, must be positive
     * @return the current {@code XReadGroupArgs}
     */
    public XReadGroupArgs count(int count) {
        this.count = count;
        return this;
    }

    /**
     * Sets the max duration to wait for messages
     *
     * @param block the duration, must not {@code null}
     * @return the current {@code XReadGroupArgs}
     */
    public XReadGroupArgs block(Duration block) {
        this.block = block;
        return this;
    }

    /**
     * Avoids adding the message to the PEL in cases where reliability is not a requirement and the occasional message
     * loss is acceptable. This is equivalent to acknowledging the message when it is read.
     *
     * @return the current {@code XReadGroupArgs}
     */
    public XReadGroupArgs noack() {
        this.noack = true;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> args = new ArrayList<>();
        if (count > 0) {
            args.add("COUNT");
            args.add(Integer.toString(count));
        }

        if (block != null) {
            args.add("BLOCK");
            args.add(Long.toString(block.toMillis()));
        }
        if (noack) {
            args.add("NOACK");
        }

        return args;
    }
}
