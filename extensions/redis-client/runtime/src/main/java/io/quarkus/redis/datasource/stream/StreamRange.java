package io.quarkus.redis.datasource.stream;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;
import io.quarkus.redis.runtime.datasource.Validation;

/**
 * Represents a stream range.
 */
public class StreamRange implements RedisCommandExtraArguments {

    private final String lowerBound;

    private final String higherBound;

    public StreamRange(String lowerBound, String higherBound) {
        this.lowerBound = Validation.notNullOrBlank(lowerBound, "lowerBound");
        this.higherBound = Validation.notNullOrBlank(higherBound, "higherBound");
    }

    public static StreamRange of(String lowerBound, String higherBound) {
        return new StreamRange(lowerBound, higherBound);
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        list.add(lowerBound);
        list.add(higherBound);
        return list;
    }
}
