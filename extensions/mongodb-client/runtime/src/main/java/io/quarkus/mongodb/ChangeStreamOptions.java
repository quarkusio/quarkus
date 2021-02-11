package io.quarkus.mongodb;

import java.util.concurrent.TimeUnit;

import org.bson.BsonDocument;
import org.bson.BsonTimestamp;

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.reactivestreams.client.ChangeStreamPublisher;

/**
 * Configures the Change Stream
 */
public class ChangeStreamOptions {

    private FullDocument fullDocument;
    private BsonDocument resumeToken;
    private BsonTimestamp startAtOperationTime;
    private long maxAwaitTime;
    private Collation collation;
    private TimeUnit maxAwaitTimeUnit;

    /**
     * Sets the fullDocument value.
     *
     * @param fullDocument the fullDocument
     * @return this
     */
    public ChangeStreamOptions fullDocument(FullDocument fullDocument) {
        this.fullDocument = fullDocument;
        return this;
    }

    /**
     * Sets the logical starting point for the new change stream.
     *
     * @param resumeToken the resume token
     * @return this
     */
    public ChangeStreamOptions resumeAfter(BsonDocument resumeToken) {
        this.resumeToken = resumeToken;
        return this;
    }

    /**
     * The change stream will only provide changes that occurred after the specified timestamp.
     *
     * <p>
     * Any command run against the server will return an operation time that can be used here.
     * </p>
     * <p>
     * The default value is an operation time obtained from the server before the change stream was created.
     * </p>
     *
     * @param startAtOperationTime the start at operation time.
     */
    public ChangeStreamOptions startAtOperationTime(BsonTimestamp startAtOperationTime) {
        this.startAtOperationTime = startAtOperationTime;
        return this;
    }

    /**
     * Sets the maximum await execution time on the server for this operation.
     *
     * @param maxAwaitTime the max await time. A zero value will be ignored, and indicates that the driver should respect the
     *        server's
     *        default value
     * @param timeUnit the time unit, which may not be null
     * @return this
     */
    public ChangeStreamOptions maxAwaitTime(long maxAwaitTime, TimeUnit timeUnit) {
        this.maxAwaitTime = maxAwaitTime;
        this.maxAwaitTimeUnit = timeUnit;
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
    public ChangeStreamOptions collation(Collation collation) {
        this.collation = collation;
        return this;
    }

    public <T> ChangeStreamPublisher<T> apply(ChangeStreamPublisher<T> stream) {
        ChangeStreamPublisher<T> publisher = stream;
        if (collation != null) {
            publisher = publisher.collation(collation);
        }
        if (maxAwaitTime > 0) {
            publisher = publisher.maxAwaitTime(maxAwaitTime, maxAwaitTimeUnit);
        }
        if (fullDocument != null) {
            publisher = publisher.fullDocument(fullDocument);
        }
        if (resumeToken != null) {
            publisher = publisher.resumeAfter(resumeToken);
        }
        if (startAtOperationTime != null) {
            publisher = publisher.startAtOperationTime(startAtOperationTime);
        }
        return publisher;
    }
}
