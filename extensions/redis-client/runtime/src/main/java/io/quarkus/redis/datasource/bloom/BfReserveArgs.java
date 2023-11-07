package io.quarkus.redis.datasource.bloom;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class BfReserveArgs implements RedisCommandExtraArguments {

    private boolean nonScaling;

    private int expansion;

    /**
     * Prevents the filter from creating additional sub-filters if initial capacity is reached. Non-scaling filters
     * requires slightly less memory than their scaling counterparts. The filter returns an error when capacity is reached.
     *
     * @return the current {@link BfReserveArgs}
     */
    public BfReserveArgs nonScaling() {
        this.nonScaling = true;
        return this;
    }

    /**
     * Set the expansion factory.
     * When capacity is reached, an additional sub-filter is created. The size of the new sub-filter is the size of
     * the last sub-filter multiplied by expansion. If the number of elements to be stored in the filter is unknown,
     * we recommend that you use an expansion of 2 or more to reduce the number of sub-filters. Otherwise, we recommend
     * that you use an expansion of 1 to reduce memory consumption. The default expansion value is 2.
     *
     * @param expansion the expansion factor, must be positive
     * @return the current {@link BfReserveArgs}
     */
    public BfReserveArgs expansion(int expansion) {
        if (expansion <= 0) {
            throw new IllegalArgumentException("the expansion factory must be positive");
        }
        this.expansion = expansion;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        if (expansion > 0) {
            list.add("EXPANSION");
            list.add(Integer.toString(expansion));
        }
        if (nonScaling) {
            list.add("NONSCALING");
        }
        return list;
    }
}
