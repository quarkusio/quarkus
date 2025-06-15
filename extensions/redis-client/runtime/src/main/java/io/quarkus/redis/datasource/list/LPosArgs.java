package io.quarkus.redis.datasource.list;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;
import io.quarkus.redis.runtime.datasource.Validation;

/**
 * Represents the extra parameter of the <a href="https://redis.io/commands/lpos/">LPOS</a> command
 */
public class LPosArgs implements RedisCommandExtraArguments {

    private long rank;
    private long maxlen;

    /**
     * The RANK option specifies the "rank" of the first element to return, in case there are multiple matches.
     *
     * @param rank
     *        the rank value, can be negative
     *
     * @return the current {@code LPosArgs}
     **/
    public LPosArgs rank(long rank) {
        this.rank = rank;
        return this;
    }

    /**
     * the MAXLEN option tells the command to compare the provided element only with a given maximum number of list
     * items. So for instance specifying MAXLEN 1000 will make sure that the command performs only 1000 comparisons,
     * effectively running the algorithm on a subset of the list.
     *
     * @param max
     *        the max value, must be positive
     *
     * @return the current {@code LPosArgs}
     **/
    public LPosArgs maxlen(long max) {
        Validation.positive(max, "max");
        this.maxlen = max;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        if (rank != 0) {
            list.add("RANK");
            list.add(Long.toString(rank));
        }
        if (maxlen != 0) {
            list.add("MAXLEN");
            list.add(Long.toString(maxlen));
        }
        return list;
    }
}
