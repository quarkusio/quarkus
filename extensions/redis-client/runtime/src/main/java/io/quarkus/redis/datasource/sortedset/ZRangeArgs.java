package io.quarkus.redis.datasource.sortedset;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class ZRangeArgs implements RedisCommandExtraArguments {

    private boolean rev;
    private long offset = -1;
    private int count;

    /**
     * The REV argument reverses the ordering, so elements are ordered from highest to lowest score, and score ties are
     * resolved by reverse lexicographical ordering.
     *
     * @return the current {@code ZRangeArgs}
     **/
    public ZRangeArgs rev() {
        this.rev = true;
        return this;
    }

    /**
     * The LIMIT argument can be used to obtain a sub-range from the matching elements.
     * A negative {@code count} returns all elements from the {@code offset}.
     *
     * @param offset the offset value
     * @param count the count value
     * @return the current {@code ZRangeArgs}
     **/
    public ZRangeArgs limit(long offset, int count) {
        this.offset = offset;
        this.count = count;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        if (rev) {
            list.add("REV");
        }
        if (count != 0 && offset != -1) {
            list.add("LIMIT");
            list.add(Long.toString(offset));
            list.add(Long.toString(count));
        }
        return list;
    }

    public boolean isReverse() {
        return rev;
    }
}
