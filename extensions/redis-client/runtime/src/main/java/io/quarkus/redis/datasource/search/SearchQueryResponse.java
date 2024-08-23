package io.quarkus.redis.datasource.search;

import java.util.Collections;
import java.util.List;

/**
 * Represents the response of a {@code ft.search} command.
 */
public class SearchQueryResponse {

    private final int count;
    private final List<Document> documents;

    public SearchQueryResponse(int count, List<Document> documents) {
        this.count = count;
        this.documents = Collections.unmodifiableList(documents);
    }

    /**
     * @return the number of documents
     */
    public int count() {
        return count;
    }

    /**
     * @return the documents
     */
    public List<Document> documents() {
        return documents;
    }
}
