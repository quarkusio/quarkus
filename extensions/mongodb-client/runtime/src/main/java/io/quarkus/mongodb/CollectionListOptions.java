package io.quarkus.mongodb;

import java.util.concurrent.TimeUnit;

import org.bson.conversions.Bson;

import com.mongodb.reactivestreams.client.ListCollectionsPublisher;

/**
 * Options to configure the stream of database.
 * 
 * @see ReactiveMongoDatabase#listCollectionNames()
 */
public class CollectionListOptions {

    private long maxTime;
    private TimeUnit maxTimeUnit;
    private Bson filter;

    /**
     * Sets the query filter to apply to the query.
     *
     * @param filter the filter, which may be null.
     * @return this
     */
    public CollectionListOptions filter(Bson filter) {
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
    public CollectionListOptions maxTime(long maxTime, TimeUnit timeUnit) {
        this.maxTime = maxTime;
        this.maxTimeUnit = timeUnit;
        return this;
    }

    public <T> ListCollectionsPublisher<T> apply(ListCollectionsPublisher<T> stream) {
        ListCollectionsPublisher<T> publisher = stream;

        if (maxTime > 0) {
            publisher = publisher.maxTime(maxTime, maxTimeUnit);
        }
        if (filter != null) {
            publisher = publisher.filter(filter);
        }

        return publisher;
    }
}
