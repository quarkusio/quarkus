package io.quarkus.redis.datasource.sortedset;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class ZMpopArgs implements RedisCommandExtraArguments {

    private boolean min;
    private boolean max;

    private int count;

    /**
     * When the {@code MIN} modifier is used, the elements popped are those with the lowest scores from the first
     * non-empty sorted set.
     *
     * @return the current {@code ZmpopArgs}
     **/
    public ZMpopArgs min() {
        this.min = true;
        return this;
    }

    /**
     * The {@code MAX} modifier causes elements with the highest scores to be popped.
     *
     * @return the current {@code ZmpopArgs}
     **/
    public ZMpopArgs max() {
        this.max = true;
        return this;
    }

    /**
     * The optional {@code COUNT} can be used to specify the number of elements to pop, and is set to 1 by default.
     *
     * @param count
     *        the count value
     *
     * @return the current {@code ZmpopArgs}
     **/
    public ZMpopArgs count(int count) {
        this.count = count;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        if (min && max) {
            throw new IllegalArgumentException("Cannot use MIN and MAX together");
        }

        List<Object> args = new ArrayList<>();
        if (min) {
            args.add("MIN");
        }
        if (max) {
            args.add("MAX");
        }

        if (count > 0) {
            args.add("COUNT");
            args.add(Integer.toString(count));
        }

        return args;
    }
}
