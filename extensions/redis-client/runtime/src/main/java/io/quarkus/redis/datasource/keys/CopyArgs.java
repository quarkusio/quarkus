package io.quarkus.redis.datasource.keys;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Arguments for the Redis <a href="https://redis.io/commands/copy">COPY</a> command.
 */
public class CopyArgs implements RedisCommandExtraArguments {

    private long destinationDb = -1;

    private boolean replace;

    /**
     * Specify an alternative logical database index for the destination key.
     *
     * @param destinationDb
     *        logical database index to apply for {@literal DB}.
     *
     * @return the current {@code CopyArgs}.
     */
    public CopyArgs destinationDb(long destinationDb) {
        this.destinationDb = destinationDb;
        return this;
    }

    /**
     * Hint redis to remove the destination key before copying the value to it.
     *
     * @param replace
     *        remove destination key before copying the value {@literal REPLACE}.
     *
     * @return {@code this}.
     */
    public CopyArgs replace(boolean replace) {
        this.replace = replace;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> args = new ArrayList<>();
        if (destinationDb != -1) {
            args.add("DB");
            args.add(Long.toString(destinationDb));
        }

        if (replace) {
            args.add("REPLACE");
        }
        return args;
    }

}
