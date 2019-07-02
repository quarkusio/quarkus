package io.quarkus.mongodb;

import java.util.concurrent.TimeUnit;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Collation;
import com.mongodb.client.model.MapReduceAction;
import com.mongodb.reactivestreams.client.MapReducePublisher;

/**
 * Map Reduce options.
 */
public class MapReduceOptions {

    private String collectionName;
    private String finalizeFunction;
    private Bson scope;
    private Bson sort;
    private Bson filter;
    private int limit;
    private boolean jsMode;
    private boolean verbose;
    private TimeUnit maxTimeUnit;
    private long maxTime;
    private MapReduceAction action;
    private String databaseName;
    private boolean sharded;
    private boolean nonAtomic;
    private boolean bypassDocumentValidation;
    private Collation collation;

    /**
     * Sets the collectionName for the output of the MapReduce
     *
     * <p>
     * The default action is replace the collection if it exists, to change this use {@link #action}.
     * </p>
     *
     * @param collectionName the name of the collection that you want the map-reduce operation to write its output.
     * @return this
     */
    public MapReduceOptions collectionName(String collectionName) {
        this.collectionName = collectionName;
        return this;
    }

    /**
     * Sets the JavaScript function that follows the reduce method and modifies the output.
     *
     * @param finalizeFunction the JavaScript function that follows the reduce method and modifies the output.
     * @return this
     */
    public MapReduceOptions finalizeFunction(String finalizeFunction) {
        this.finalizeFunction = finalizeFunction;
        return this;
    }

    /**
     * Sets the global variables that are accessible in the map, reduce and finalize functions.
     *
     * @param scope the global variables that are accessible in the map, reduce and finalize functions.
     * @return this
     */
    public MapReduceOptions scope(Bson scope) {
        this.scope = scope;
        return this;
    }

    /**
     * Sets the sort criteria to apply to the query.
     *
     * @param sort the sort criteria, which may be null.
     * @return this
     */
    public MapReduceOptions sort(Bson sort) {
        this.sort = sort;
        return this;
    }

    /**
     * Sets the query filter to apply to the query.
     *
     * @param filter the filter to apply to the query.
     * @return this
     */
    public MapReduceOptions filter(Bson filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Sets the limit to apply.
     *
     * @param limit the limit, which may be null
     * @return this
     */
    public MapReduceOptions limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets the flag that specifies whether to convert intermediate data into BSON format between the execution of the
     * map and reduce functions. Defaults to false.
     *
     * @param jsMode the flag that specifies whether to convert intermediate data into BSON format between the
     *        execution of the map and reduce functions
     * @return jsMode
     */
    public MapReduceOptions jsMode(boolean jsMode) {
        this.jsMode = jsMode;
        return this;
    }

    /**
     * Sets whether to include the timing information in the result information.
     *
     * @param verbose whether to include the timing information in the result information.
     * @return this
     */
    public MapReduceOptions verbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    /**
     * Sets the maximum execution time on the server for this operation.
     *
     * @param maxTime the max time
     * @param timeUnit the time unit, which may not be null
     * @return this
     */
    public MapReduceOptions maxTime(long maxTime, TimeUnit timeUnit) {
        this.maxTime = maxTime;
        this.maxTimeUnit = timeUnit;
        return this;
    }

    /**
     * Specify the {@code MapReduceAction} to be used when writing to a collection.
     *
     * @param action an {@link com.mongodb.client.model.MapReduceAction} to perform on the collection
     * @return this
     */
    public MapReduceOptions action(MapReduceAction action) {
        this.action = action;
        return this;
    }

    /**
     * Sets the name of the database to output into.
     *
     * @param databaseName the name of the database to output into.
     * @return this
     */
    public MapReduceOptions databaseName(String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    /**
     * Sets if the output database is sharded
     *
     * @param sharded if the output database is sharded
     * @return this
     */
    public MapReduceOptions sharded(boolean sharded) {
        this.sharded = sharded;
        return this;
    }

    /**
     * Sets if the post-processing step will prevent MongoDB from locking the database.
     * <p>
     * Valid only with the {@code MapReduceAction.MERGE} or {@code MapReduceAction.REDUCE} actions.
     *
     * @param nonAtomic if the post-processing step will prevent MongoDB from locking the database.
     * @return this
     */
    public MapReduceOptions nonAtomic(boolean nonAtomic) {
        this.nonAtomic = nonAtomic;
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
    public MapReduceOptions bypassDocumentValidation(boolean bypassDocumentValidation) {
        this.bypassDocumentValidation = bypassDocumentValidation;
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
    public MapReduceOptions collation(Collation collation) {
        this.collation = collation;
        return this;
    }

    public <T> MapReducePublisher<T> apply(MapReducePublisher<T> stream) {
        MapReducePublisher<T> publisher = stream;

        if (collectionName != null) {
            publisher = publisher.collectionName(collectionName);
        }
        if (finalizeFunction != null) {
            publisher = publisher.finalizeFunction(finalizeFunction);
        }
        if (scope != null) {
            publisher = publisher.scope(scope);
        }
        if (sort != null) {
            publisher = publisher.sort(sort);
        }
        if (filter != null) {
            publisher = publisher.filter(filter);
        }
        if (limit > 0) {
            publisher = publisher.limit(limit);
        }
        publisher = publisher.jsMode(jsMode);
        publisher = publisher.verbose(verbose);
        if (maxTime > 0) {
            publisher = publisher.maxTime(maxTime, maxTimeUnit);
        }
        if (action != null) {
            publisher = publisher.action(action);
        }
        if (databaseName != null) {
            publisher = publisher.databaseName(databaseName);
        }
        publisher = publisher.sharded(sharded);
        publisher = publisher.nonAtomic(nonAtomic);
        publisher = publisher.bypassDocumentValidation(bypassDocumentValidation);
        if (collation != null) {
            publisher = publisher.collation(collation);
        }
        return publisher;
    }
}
