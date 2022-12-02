package io.quarkus.mongodb;

import java.util.concurrent.TimeUnit;

import org.bson.conversions.Bson;

import com.mongodb.reactivestreams.client.ListDatabasesPublisher;

/**
 * Options to configure the stream of database.
 *
 * @see ReactiveMongoClient#listDatabases(DatabaseListOptions)
 */
public class DatabaseListOptions {

    private long maxTime;
    private TimeUnit maxTimeUnit;
    private Bson filter;
    private boolean nameOnly;

    /**
     * Sets the maximum execution time on the server for this operation.
     *
     * @param maxTime the max time
     * @param timeUnit the time unit, which may not be null
     * @return this
     */
    public DatabaseListOptions maxTime(long maxTime, TimeUnit timeUnit) {
        this.maxTime = maxTime;
        this.maxTimeUnit = timeUnit;
        return this;
    }

    /**
     * Sets the query filter to apply to the returned database names.
     *
     * @param filter the filter, which may be null.
     * @return this
     */
    public DatabaseListOptions filter(Bson filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets the nameOnly flag that indicates whether the command should return just the database names or return the database
     * names and
     * size information.
     *
     * @param nameOnly the nameOnly flag, which may be null
     * @return this
     */
    public DatabaseListOptions nameOnly(boolean nameOnly) {
        this.nameOnly = nameOnly;
        return this;
    }

    public <T> ListDatabasesPublisher<T> apply(ListDatabasesPublisher<T> publisher) {
        ListDatabasesPublisher<T> result = publisher;
        if (nameOnly) {
            result = publisher.nameOnly(nameOnly);
        }
        if (filter != null) {
            result = publisher.filter(filter);
        }
        if (maxTime > 0) {
            result.maxTime(maxTime, maxTimeUnit);
        }
        return result;
    }
}
