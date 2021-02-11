package io.quarkus.mongodb;

import java.util.concurrent.TimeUnit;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Collation;
import com.mongodb.reactivestreams.client.DistinctPublisher;

public class DistinctOptions {

    private Bson filter;
    private long maxTime;
    private TimeUnit maxTimeUnit;
    private Collation collation;

    /**
     * Sets the query filter to apply to the query.
     *
     * @param filter the filter, which may be null.
     * @return this
     */
    public DistinctOptions filter(Bson filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets the maximum execution time on the server for this operation.
     *
     * @param maxTime the max time
     * @param timeUnit the time unit, which may not be null
     * @return this
     */
    public DistinctOptions maxTime(long maxTime, TimeUnit timeUnit) {
        this.maxTime = maxTime;
        this.maxTimeUnit = timeUnit;
        return this;
    }

    /**
     * Sets the collation options
     *
     * <p>
     * A null value represents the server default.
     * </p>
     * 
     * @param collation the collation options to use
     * @return this
     */
    public DistinctOptions collation(Collation collation) {
        this.collation = collation;
        return this;
    }

    public <T> DistinctPublisher<T> apply(DistinctPublisher<T> stream) {
        DistinctPublisher<T> publisher = stream;
        if (collation != null) {
            publisher = publisher.collation(collation);
        }
        if (maxTime > 0) {
            publisher = publisher.maxTime(maxTime, maxTimeUnit);
        }
        if (filter != null) {
            publisher = publisher.filter(filter);
        }
        return publisher;
    }
}
