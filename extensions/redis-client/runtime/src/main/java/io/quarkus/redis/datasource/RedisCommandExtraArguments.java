package io.quarkus.redis.datasource;

import java.util.Collections;
import java.util.List;

import io.quarkus.redis.datasource.codecs.Codec;

public interface RedisCommandExtraArguments {

    /**
     * @return the list of arguments, encoded as a list of String.
     */
    default List<String> toArgs() {
        return toArgs(null);
    }

    /**
     * @param encoder an optional encoder to encode some of the values
     * @return the list of arguments, encoded as a list of String.
     */
    default List<String> toArgs(Codec encoder) {
        return Collections.emptyList();
    }

}
