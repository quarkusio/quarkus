package io.quarkus.mongodb.panache.common.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ReadPreference;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Collation;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Range;
import io.quarkus.panache.common.exception.PanacheQueryException;

public class CommonPanacheQueryImpl<Entity> {
    private MongoCollection collection;
    private ClientSession clientSession;
    private Bson mongoQuery;
    private Bson sort;
    private Bson projections;

    private Page page;
    private Long count;

    private Range range;

    private Collation collation;

    public CommonPanacheQueryImpl(MongoCollection<? extends Entity> collection, ClientSession session, Bson mongoQuery,
            Bson sort) {
        this.collection = collection;
        this.clientSession = session;
        this.mongoQuery = mongoQuery;
        this.sort = sort;
    }

    private CommonPanacheQueryImpl(CommonPanacheQueryImpl<?> previousQuery, Bson projections, Class<?> documentClass) {
        this.collection = previousQuery.collection.withDocumentClass(documentClass);
        this.clientSession = previousQuery.clientSession;
        this.mongoQuery = previousQuery.mongoQuery;
        this.sort = previousQuery.sort;
        this.projections = projections;
        this.page = previousQuery.page;
        this.count = previousQuery.count;
        this.range = previousQuery.range;
        this.collation = previousQuery.collation;
    }

    public <T> CommonPanacheQueryImpl<T> project(Class<T> type) {
        // collect field names from public fields and getters
        Set<String> fieldNames = MongoPropertyUtil.collectFields(type);

        // create the projection document
        Document projections = new Document();
        for (String fieldName : fieldNames) {
            projections.append(fieldName, 1);
        }

        return new CommonPanacheQueryImpl<>(this, projections, type);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> CommonPanacheQueryImpl<T> page(Page page) {
        this.page = page;
        this.range = null; // reset the range to be able to switch from range to page
        return (CommonPanacheQueryImpl<T>) this;
    }

    public <T extends Entity> CommonPanacheQueryImpl<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    public <T extends Entity> CommonPanacheQueryImpl<T> nextPage() {
        checkPagination();
        return page(page.next());
    }

    public <T extends Entity> CommonPanacheQueryImpl<T> previousPage() {
        checkPagination();
        return page(page.previous());
    }

    public <T extends Entity> CommonPanacheQueryImpl<T> firstPage() {
        checkPagination();
        return page(page.first());
    }

    public <T extends Entity> CommonPanacheQueryImpl<T> lastPage() {
        checkPagination();
        return page(page.index(pageCount() - 1));
    }

    public boolean hasNextPage() {
        checkPagination();
        return page.index < (pageCount() - 1);
    }

    public boolean hasPreviousPage() {
        checkPagination();
        return page.index > 0;
    }

    public int pageCount() {
        checkPagination();
        long count = count();
        if (count == 0)
            return 1; // a single page of zero results
        return (int) Math.ceil((double) count / (double) page.size);
    }

    public Page page() {
        checkPagination();
        return page;
    }

    private void checkPagination() {
        if (page == null) {
            throw new UnsupportedOperationException(
                    "Cannot call a page related method, "
                            + "call page(Page) or page(int, int) to initiate pagination first");
        }
        if (range != null) {
            throw new UnsupportedOperationException("Cannot call a page related method in a ranged query, " +
                    "call page(Page) or page(int, int) to initiate pagination first");
        }
    }

    public <T extends Entity> CommonPanacheQueryImpl<T> range(int startIndex, int lastIndex) {
        this.range = Range.of(startIndex, lastIndex);
        // reset the page to its default to be able to switch from page to range
        this.page = null;
        return (CommonPanacheQueryImpl<T>) this;
    }

    public <T extends Entity> CommonPanacheQueryImpl<T> withCollation(Collation collation) {
        this.collation = collation;
        return (CommonPanacheQueryImpl<T>) this;
    }

    public <T extends Entity> CommonPanacheQueryImpl<T> withReadPreference(ReadPreference readPreference) {
        this.collection = this.collection.withReadPreference(readPreference);
        return (CommonPanacheQueryImpl<T>) this;
    }

    // Results

    @SuppressWarnings("unchecked")
    public long count() {
        if (count == null) {
            Bson query = getQuery();
            count = clientSession == null ? collection.countDocuments(query) : collection.countDocuments(clientSession, query);
        }
        return count;
    }

    public <T extends Entity> List<T> list() {
        return list(null);
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> List<T> list(Integer limit) {
        List<T> list = new ArrayList<>();
        Bson query = getQuery();
        FindIterable find = clientSession == null ? collection.find(query) : collection.find(clientSession, query);
        if (this.projections != null) {
            find.projection(projections);
        }
        if (this.collation != null) {
            find.collation(collation);
        }
        manageOffsets(find, limit);

        try (MongoCursor<T> cursor = find.sort(sort).iterator()) {
            while (cursor.hasNext()) {
                T entity = cursor.next();
                list.add(entity);
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream() {
        return (Stream<T>) list().stream();
    }

    public <T extends Entity> T firstResult() {
        List<T> list = list(1);
        return list.isEmpty() ? null : list.get(0);
    }

    public <T extends Entity> Optional<T> firstResultOptional() {
        return Optional.ofNullable(firstResult());
    }

    public <T extends Entity> T singleResult() {
        List<T> list = list(2);
        if (list.size() != 1) {
            throw new PanacheQueryException("There should be only one result");
        }

        return list.get(0);
    }

    public <T extends Entity> Optional<T> singleResultOptional() {
        List<T> list = list(2);
        if (list.size() > 1) {
            throw new PanacheQueryException("There should be no more than one result");
        }

        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private void manageOffsets(FindIterable find, Integer limit) {
        if (range != null) {
            find.skip(range.getStartIndex());
            if (limit == null) {
                // range is 0 based, so we add 1 to the limit
                find.limit(range.getLastIndex() - range.getStartIndex() + 1);
            }
        } else if (page != null) {
            find.skip(page.index * page.size);
            if (limit == null) {
                find.limit(page.size);
            }
        }
        if (limit != null) {
            find.limit(limit);
        }
    }

    private Bson getQuery() {
        return mongoQuery == null ? new BsonDocument() : mongoQuery;
    }
}
