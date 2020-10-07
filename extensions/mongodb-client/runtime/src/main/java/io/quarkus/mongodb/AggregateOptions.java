package io.quarkus.mongodb;

import java.util.concurrent.TimeUnit;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Collation;
import com.mongodb.reactivestreams.client.AggregatePublisher;

/**
 * Configures the aggregate streams.
 */
public class AggregateOptions {

    private Bson hint;
    private boolean allowDiskUse;
    private long maxTime;
    private TimeUnit maxTimeUnit;
    private long maxAwaitTime;
    private TimeUnit maxAwaitTimeUnit;
    private boolean bypassDocumentValidation;
    private Collation collation;
    private String comment;
    private int batchSize;

    /**
     * Enables writing to temporary files. A null value indicates that it's unspecified.
     *
     * @param allowDiskUse true if writing to temporary files is enabled
     * @return this
     */
    public AggregateOptions allowDiskUse(boolean allowDiskUse) {
        this.allowDiskUse = allowDiskUse;
        return this;
    }

    /**
     * Sets the maximum execution time on the server for this operation.
     *
     * @param maxTime the max time
     * @param timeUnit the time unit, which may not be null
     * @return this
     */
    public AggregateOptions maxTime(long maxTime, TimeUnit timeUnit) {
        this.maxTime = maxTime;
        this.maxTimeUnit = timeUnit;
        return this;
    }

    /**
     * The maximum amount of time for the server to wait on new documents to satisfy a {@code $changeStream} aggregation.
     * <p>
     * A zero value will be ignored.
     *
     * @param maxAwaitTime the max await time
     * @param timeUnit the time unit to return the result in
     * @return the maximum await execution time in the given time unit
     */
    public AggregateOptions maxAwaitTime(long maxAwaitTime, TimeUnit timeUnit) {
        this.maxAwaitTime = maxAwaitTime;
        this.maxAwaitTimeUnit = timeUnit;
        return this;
    }

    /**
     * Sets the bypass document level validation flag.
     *
     * <p>
     * Note: This only applies when an $out stage is specified
     * </p>
     * .
     *
     * @param bypassDocumentValidation If true, allows the write to opt-out of document level validation.
     * @return this
     */
    public AggregateOptions bypassDocumentValidation(boolean bypassDocumentValidation) {
        this.bypassDocumentValidation = true;
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
    public AggregateOptions collation(Collation collation) {
        this.collation = collation;
        return this;
    }

    /**
     * Sets the comment to the aggregation. A null value means no comment is set.
     *
     * @param comment the comment
     * @return this
     */
    public AggregateOptions comment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Sets the hint for which index to use. A null value means no hint is set.
     *
     * @param hint the hint
     * @return this
     */
    public AggregateOptions hint(Bson hint) {
        this.hint = hint;
        return this;
    }

    /**
     * Sets the number of documents to return per batch.
     *
     * <p>
     * Overrides the {@link org.reactivestreams.Subscription#request(long)} value for setting the batch size, allowing for fine
     * grained
     * control over the underlying cursor.
     * </p>
     *
     * @param size the batch size
     * @return this
     */
    public AggregateOptions batchSize(int size) {
        this.batchSize = size;
        return this;
    }

    public <T> AggregatePublisher<T> apply(AggregatePublisher<T> stream) {
        AggregatePublisher<T> publisher = stream;

        if (hint != null) {
            publisher = publisher.hint(hint);
        }
        if (comment != null) {
            publisher = publisher.comment(comment);
        }
        if (collation != null) {
            publisher = publisher.collation(collation);
        }
        publisher.bypassDocumentValidation(bypassDocumentValidation);
        publisher.allowDiskUse(allowDiskUse);
        if (maxAwaitTime > 0) {
            publisher.maxAwaitTime(maxAwaitTime, maxAwaitTimeUnit);
        }
        if (maxTime > 0) {
            publisher.maxAwaitTime(maxTime, maxTimeUnit);
        }
        if (batchSize > 0) {
            publisher.batchSize(batchSize);
        }
        return publisher;
    }
}
