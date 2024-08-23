package io.quarkus.redis.datasource.search;

import java.util.Collections;
import java.util.List;

/**
 * Represents the response of the {@code ft.aggregate} command.
 */
public class AggregationResponse {

    private final long cursor;

    private final List<AggregateDocument> documents;
    private final int count;

    public AggregationResponse(long cursor, List<AggregateDocument> results) {
        this.cursor = cursor;
        this.count = results.size();
        this.documents = Collections.unmodifiableList(results);
    }

    public AggregationResponse(List<AggregateDocument> results) {
        this(-1, results);
    }

    /**
     * @return the number of document.
     */
    public int count() {
        return count;
    }

    /**
     * @return the cursor id, {@code -1} if the command didn't pass {@code WITHCURSOR}, 0 when the cursor reached the
     *         end of the data set.
     */
    public long cursor() {
        return cursor;
    }

    /**
     * @return the list of document.
     */
    public List<AggregateDocument> documents() {
        return documents;
    }
}
