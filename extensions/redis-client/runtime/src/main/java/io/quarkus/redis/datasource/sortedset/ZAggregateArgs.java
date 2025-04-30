package io.quarkus.redis.datasource.sortedset;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class ZAggregateArgs implements RedisCommandExtraArguments {

    public enum Aggregate {
        SUM,
        MIN,
        MAX
    }

    private final List<Double> weights = new ArrayList<>();

    private Aggregate aggregate;

    /**
     * Using the WEIGHTS option, it is possible to specify a multiplication factor for each input sorted set.
     * This means that the score of every element in every input sorted set is multiplied by this factor before being
     * passed to the aggregation function. When WEIGHTS is not given, the multiplication factors default to 1.
     *
     * @param weights the weight values
     * @return the current {@code ZAggregateArgs}
     **/
    public ZAggregateArgs weights(double... weights) {
        if (weights == null) {
            throw new IllegalArgumentException("`weights` cannot be `null`");
        }
        for (double weight : weights) {
            this.weights.add(weight);
        }
        return this;
    }

    /**
     * With the AGGREGATE option, it is possible to specify how the results of the union are aggregated.
     * This option defaults to SUM, where the score of an element is summed across the inputs where it exists.
     * When this option is set to either MIN or MAX, the resulting set will contain the minimum or maximum score of
     * an element across the inputs where it exists.
     *
     * @param aggregate the aggregate value
     * @return the current {@code ZAggregateArgs}
     **/
    public ZAggregateArgs aggregate(Aggregate aggregate) {
        this.aggregate = aggregate;
        return this;
    }

    /**
     * Configure the {@code aggregate} function to be {@code SUM}.
     *
     * @return the current {@code ZAggregateArgs}
     */
    public ZAggregateArgs sum() {
        this.aggregate = Aggregate.SUM;
        return this;
    }

    /**
     * Configure the {@code aggregate} function to be {@code MIN}.
     *
     * @return the current {@code ZAggregateArgs}
     */
    public ZAggregateArgs min() {
        this.aggregate = Aggregate.MIN;
        return this;
    }

    /**
     * Configure the {@code aggregate} function to be {@code MAX}.
     *
     * @return the current {@code ZAggregateArgs}
     */
    public ZAggregateArgs max() {
        this.aggregate = Aggregate.MAX;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> args = new ArrayList<>();
        if (!weights.isEmpty()) {
            args.add("WEIGHTS");
            for (double w : weights) {
                args.add(Double.toString(w));
            }
        }
        if (aggregate != null) {
            args.add("AGGREGATE");
            switch (aggregate) {
                case SUM:
                    args.add("SUM");
                    break;
                case MIN:
                    args.add("MIN");
                    break;
                case MAX:
                    args.add("MAX");
                    break;
                default:
                    throw new IllegalArgumentException("Aggregation " + aggregate + " not supported");
            }
        }
        return args;
    }
}
