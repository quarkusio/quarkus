package io.quarkus.redis.datasource.bloom;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class BfInsertArgs implements RedisCommandExtraArguments {

    private long capacity;
    private double errorRate = -1.0;

    private boolean nocreate;

    private boolean nonScaling;

    private int expansion;

    /**
     * Specifies the desired capacity for the filter to be created. This parameter is ignored if the filter already
     * exists. If the filter is automatically created and this parameter is absent, then the module-level capacity is
     * used.
     *
     * @param capacity
     *        the capacity
     *
     * @return the current {@link BfInsertArgs}
     */
    public BfInsertArgs capacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    /**
     * Specifies the error ratio of the newly created filter if it does not yet exist. If the filter is automatically
     * created and error is not specified then the module-level error rate is used.
     *
     * @param errorRate
     *        the error rate, must be between 0 and 1.
     *
     * @return the current {@link BfInsertArgs}
     */
    public BfInsertArgs errorRate(double errorRate) {
        this.errorRate = errorRate;
        return this;
    }

    /**
     * Indicates that the filter should not be created if it does not already exist.
     *
     * @return the current {@link BfInsertArgs}
     */
    public BfInsertArgs nocreate() {
        this.nocreate = true;
        return this;
    }

    /**
     * Prevents the filter from creating additional sub-filters if initial capacity is reached. Non-scaling filters
     * requires slightly less memory than their scaling counterparts. The filter returns an error when capacity is
     * reached.
     *
     * @return the current {@link BfInsertArgs}
     */
    public BfInsertArgs nonScaling() {
        this.nonScaling = true;
        return this;
    }

    /**
     * Set the expansion factory. When capacity is reached, an additional sub-filter is created. The size of the new
     * sub-filter is the size of the last sub-filter multiplied by expansion. If the number of elements to be stored in
     * the filter is unknown, we recommend that you use an expansion of 2 or more to reduce the number of sub-filters.
     * Otherwise, we recommend that you use an expansion of 1 to reduce memory consumption. The default expansion value
     * is 2.
     *
     * @param expansion
     *        the expansion factor, must be positive
     *
     * @return the current {@link BfInsertArgs}
     */
    public BfInsertArgs expansion(int expansion) {
        if (expansion <= 0) {
            throw new IllegalArgumentException("the expansion factory must be positive");
        }
        this.expansion = expansion;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();

        if (capacity > 0) {
            list.add("CAPACITY");
            list.add(Long.toString(capacity));
        }

        if (errorRate != -1.0) {
            list.add("ERROR");
            list.add(new BigDecimal(errorRate).toPlainString()); // Prevent E notation
        }

        if (expansion > 0) {
            list.add("EXPANSION");
            list.add(Integer.toString(expansion));
        }

        if (nocreate) {
            list.add("NOCREATE");
        }

        if (nonScaling) {
            list.add("NONSCALING");
        }
        return list;
    }
}
