package io.quarkus.mongodb;

import java.util.concurrent.TimeUnit;

import org.bson.conversions.Bson;

import com.mongodb.CursorType;
import com.mongodb.client.model.Collation;
import com.mongodb.reactivestreams.client.FindPublisher;

/**
 * Stream options for {@code find}.
 */
public class FindOptions {

    private Bson filter;
    private int limit;
    private int skip;
    private long maxTime;
    private TimeUnit maxTimeUnit;
    private Bson projection;
    private Bson sort;
    private boolean noCursorTimeout;
    private boolean oplogReplay;
    private boolean partial;
    private CursorType cursorType;
    private Collation collation;
    private String comment;
    private Bson hint;
    private Bson max;
    private Bson min;
    private boolean returnKey;
    private boolean showRecordId;
    private long maxAwaitTime;
    private TimeUnit maxAwaitTimeUnit;
    private int batchSize;

    /**
     * Sets the query filter to apply to the query.
     *
     * @param filter the filter, which may be null.
     * @return this
     */
    public FindOptions filter(Bson filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets the limit to apply.
     *
     * @param limit the limit, which may be null
     * @return this
     */
    public FindOptions limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets the number of documents to skip.
     *
     * @param skip the number of documents to skip
     * @return this
     */
    public FindOptions skip(int skip) {
        this.skip = skip;
        return this;
    }

    /**
     * Sets the maximum execution time on the server for this operation.
     *
     * @param maxTime the max time
     * @param timeUnit the time unit, which may not be null
     * @return this
     */
    public FindOptions maxTime(long maxTime, TimeUnit timeUnit) {
        this.maxTime = maxTime;
        this.maxTimeUnit = timeUnit;
        return this;
    }

    /**
     * The maximum amount of time for the server to wait on new documents to satisfy a tailable cursor
     * query. This only applies to a TAILABLE_AWAIT cursor. When the cursor is not a TAILABLE_AWAIT cursor,
     * this option is ignored.
     * <p>
     * On servers &gt;= 3.2, this option will be specified on the getMore command as "maxTimeMS". The default
     * is no value: no "maxTimeMS" is sent to the server with the getMore command.
     * <p>
     * On servers &lt; 3.2, this option is ignored, and indicates that the driver should respect the server's default value
     * <p>
     * A zero value will be ignored.
     *
     * @param maxAwaitTime the max await time
     * @param timeUnit the time unit to return the result in
     * @return the maximum await execution time in the given time unit
     */
    public FindOptions maxAwaitTime(long maxAwaitTime, TimeUnit timeUnit) {
        this.maxAwaitTime = maxTime;
        this.maxAwaitTimeUnit = timeUnit;
        return this;
    }

    /**
     * Sets a document describing the fields to return for all matching documents.
     *
     * @param projection the project document, which may be null.
     * @return this
     */
    public FindOptions projection(Bson projection) {
        this.projection = projection;
        return this;
    }

    /**
     * Sets the sort criteria to apply to the query.
     *
     * @param sort the sort criteria, which may be null.
     * @return this
     */
    public FindOptions sort(Bson sort) {
        this.sort = sort;
        return this;
    }

    /**
     * The server normally times out idle cursors after an inactivity period (10 minutes)
     * to prevent excess memory use. Set this option to prevent that.
     *
     * @param noCursorTimeout true if cursor timeout is disabled
     * @return this
     */
    public FindOptions noCursorTimeout(boolean noCursorTimeout) {
        this.noCursorTimeout = noCursorTimeout;
        return this;
    }

    /**
     * Users should not set this under normal circumstances.
     *
     * @param oplogReplay if oplog replay is enabled
     * @return this
     */
    public FindOptions oplogReplay(boolean oplogReplay) {
        this.oplogReplay = oplogReplay;
        return this;
    }

    /**
     * Get partial results from a sharded cluster if one or more shards are unreachable (instead of throwing an error).
     *
     * @param partial if partial results for sharded clusters is enabled
     * @return this
     */
    public FindOptions partial(boolean partial) {
        this.partial = partial;
        return this;
    }

    /**
     * Sets the cursor type.
     *
     * @param cursorType the cursor type
     * @return this
     */
    public FindOptions cursorType(CursorType cursorType) {
        this.cursorType = cursorType;
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
    public FindOptions collation(Collation collation) {
        this.collation = collation;
        return this;
    }

    /**
     * Sets the comment to the query. A null value means no comment is set.
     *
     * @param comment the comment
     * @return this
     */
    public FindOptions comment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Sets the hint for which index to use. A null value means no hint is set.
     *
     * @param hint the hint
     * @return this
     */
    public FindOptions hint(Bson hint) {
        this.hint = hint;
        return this;
    }

    /**
     * Sets the exclusive upper bound for a specific index. A null value means no max is set.
     *
     * @param max the max
     * @return this
     */
    public FindOptions max(Bson max) {
        this.max = max;
        return this;
    }

    /**
     * Sets the minimum inclusive lower bound for a specific index. A null value means no max is set.
     *
     * @param min the min
     * @return this
     */
    public FindOptions min(Bson min) {
        this.min = min;
        return this;
    }

    /**
     * Sets the returnKey. If true the find operation will return only the index keys in the resulting documents.
     *
     * @param returnKey the returnKey
     * @return this
     */
    public FindOptions returnKey(boolean returnKey) {
        this.returnKey = returnKey;
        return this;
    }

    /**
     * Sets the showRecordId. Set to true to add a field {@code $recordId} to the returned documents.
     *
     * @param showRecordId the showRecordId
     * @return this
     */
    public FindOptions showRecordId(boolean showRecordId) {
        this.showRecordId = showRecordId;
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
    public FindOptions batchSize(int size) {
        this.batchSize = size;
        return this;
    }

    public <T> FindPublisher<T> apply(FindPublisher<T> stream) {
        FindPublisher<T> publisher = stream;
        if (filter != null) {
            publisher = publisher.filter(filter);
        }
        if (limit > 0) {
            publisher = publisher.limit(limit);
        }
        if (skip > 0) {
            publisher = publisher.skip(skip);
        }
        if (maxTime > 0) {
            publisher = publisher.maxTime(maxTime, maxTimeUnit);
        }
        if (maxAwaitTime > 0) {
            publisher = publisher.maxAwaitTime(maxAwaitTime, maxAwaitTimeUnit);
        }
        if (projection != null) {
            publisher = publisher.projection(projection);
        }
        if (sort != null) {
            publisher = publisher.sort(sort);
        }
        if (noCursorTimeout) {
            publisher = publisher.noCursorTimeout(true);
        }
        if (oplogReplay) {
            publisher = publisher.oplogReplay(true);
        }
        if (partial) {
            publisher = publisher.partial(true);
        }
        if (cursorType != null) {
            publisher = publisher.cursorType(cursorType);
        }
        if (collation != null) {
            publisher = publisher.collation(collation);
        }
        if (comment != null) {
            publisher = publisher.comment(comment);
        }
        if (hint != null) {
            publisher = publisher.hint(hint);
        }
        if (max != null) {
            publisher = publisher.max(max);
        }
        if (min != null) {
            publisher = publisher.min(min);
        }
        if (returnKey) {
            publisher = publisher.returnKey(true);
        }
        if (showRecordId) {
            publisher = publisher.showRecordId(true);
        }
        if (batchSize > 0) {
            publisher.batchSize(batchSize);
        }
        return publisher;

    }

}
