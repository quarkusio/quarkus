package io.quarkus.redis.datasource.autosuggest;

import static io.quarkus.redis.runtime.datasource.Validation.positive;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class GetArgs implements RedisCommandExtraArguments {

    private boolean fuzzy;
    private int max;
    private boolean withScores;

    /**
     * Performs a fuzzy prefix search, including prefixes at Levenshtein distance of 1 from the prefix sent.
     *
     * @return the current {@code GetArgs}.
     */
    public GetArgs fuzzy() {
        this.fuzzy = true;
        return this;
    }

    /**
     * Limits the results to a maximum of num (default: 5).
     *
     * @param max the max number of results, must be strictly positive
     * @return the current {@code GetArgs}.
     */
    public GetArgs max(int max) {
        positive(max, "max");
        this.max = max;
        return this;
    }

    /**
     * Also to attach the score of each suggestion.
     * This can be used to merge results from multiple instances.
     *
     * @return the current {@code GetArgs}.
     */
    public GetArgs withScores() {
        this.withScores = true;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        if (fuzzy) {
            list.add("FUZZY");
        }
        if (max > 0) {
            list.add("MAX");
            list.add(Integer.toString(max));
        }
        if (withScores) {
            list.add("WITHSCORES");
        }
        return list;
    }

    public boolean hasScores() {
        return withScores;
    }
}
