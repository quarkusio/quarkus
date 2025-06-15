package io.quarkus.redis.datasource.cuckoo;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class CfReserveArgs implements RedisCommandExtraArguments {

    private long bucketSize;

    private int maxIterations;

    private int expansion;

    /**
     * Set the number of items in each bucket. A higher bucket size value improves the fill rate but also causes a
     * higher error rate and slightly slower performance. The default value is 2.
     *
     * @param bucketSize
     *        the bucket size
     *
     * @return the current {@link CfReserveArgs}
     */
    public CfReserveArgs bucketSize(long bucketSize) {
        this.bucketSize = bucketSize;
        return this;
    }

    /**
     * Sets the number of attempts to swap items between buckets before declaring filter as full and creating an
     * additional filter. A low value is better for performance and a higher number is better for filter fill rate. The
     * default value is 20.
     *
     * @param maxIterations
     *        the iterations
     *
     * @return the current {@link CfReserveArgs}
     */
    public CfReserveArgs maxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    /**
     * When a new filter is created, its size is the size of the current filter multiplied by expansion. Expansion is
     * rounded to the next 2^n number. The default value is 1.
     *
     * @param expansion
     *        the expansion factor
     *
     * @return the current {@link CfReserveArgs}
     */
    public CfReserveArgs expansion(int expansion) {
        this.expansion = expansion;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        if (bucketSize > 0) {
            list.add("BUCKETSIZE");
            list.add(Long.toString(bucketSize));
        }

        if (maxIterations > 0) {
            list.add("MAXITERATIONS");
            list.add(Integer.toString(maxIterations));
        }

        if (expansion > 0) {
            list.add("EXPANSION");
            list.add(Integer.toString(expansion));
        }

        return list;
    }
}
