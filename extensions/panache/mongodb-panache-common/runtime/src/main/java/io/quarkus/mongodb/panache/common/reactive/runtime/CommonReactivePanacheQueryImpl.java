package io.quarkus.mongodb.panache.common.reactive.runtime;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ReadPreference;
import com.mongodb.client.model.Collation;

import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.panache.common.runtime.MongoPropertyUtil;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Range;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class CommonReactivePanacheQueryImpl<Entity> {
    private ReactiveMongoCollection collection;
    private Bson mongoQuery;
    private Bson sort;
    private Bson projections;

    private Page page;
    private Uni<Long> count;

    private Range range;

    private Collation collation;

    public CommonReactivePanacheQueryImpl(ReactiveMongoCollection<? extends Entity> collection, Bson mongoQuery, Bson sort) {
        this.collection = collection;
        this.mongoQuery = mongoQuery;
        this.sort = sort;
    }

    private CommonReactivePanacheQueryImpl(CommonReactivePanacheQueryImpl previousQuery, Bson projections, Class<?> type) {
        this.collection = previousQuery.collection.withDocumentClass(type);
        this.mongoQuery = previousQuery.mongoQuery;
        this.sort = previousQuery.sort;
        this.projections = projections;
        this.page = previousQuery.page;
        this.count = previousQuery.count;
        this.range = previousQuery.range;
        this.collation = previousQuery.collation;
    }

    // Builder

    public <T> CommonReactivePanacheQueryImpl<T> project(Class<T> type) {
        // collect field names from public fields and getters
        Set<String> fieldNames = MongoPropertyUtil.collectFields(type);

        // create the projection document
        Document projections = new Document();
        for (String fieldName : fieldNames) {
            projections.append(fieldName, 1);
        }

        return new CommonReactivePanacheQueryImpl(this, projections, type);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> CommonReactivePanacheQueryImpl<T> page(Page page) {
        this.page = page;
        this.range = null; // reset the range to be able to switch from range to page
        return (CommonReactivePanacheQueryImpl<T>) this;
    }

    public <T extends Entity> CommonReactivePanacheQueryImpl<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    public <T extends Entity> CommonReactivePanacheQueryImpl<T> nextPage() {
        checkPagination();
        return page(page.next());
    }

    public <T extends Entity> CommonReactivePanacheQueryImpl<T> previousPage() {
        checkPagination();
        return page(page.previous());
    }

    public <T extends Entity> CommonReactivePanacheQueryImpl<T> firstPage() {
        checkPagination();
        return page(page.first());
    }

    public <T extends Entity> Uni<CommonReactivePanacheQueryImpl<T>> lastPage() {
        checkPagination();
        Uni<CommonReactivePanacheQueryImpl<T>> map = pageCount().map(pageCount -> page(page.index(pageCount - 1)));
        return map;
    }

    public Uni<Boolean> hasNextPage() {
        checkPagination();
        return pageCount().map(pageCount -> page.index < (pageCount - 1));
    }

    public boolean hasPreviousPage() {
        checkPagination();
        return page.index > 0;
    }

    public Uni<Integer> pageCount() {
        checkPagination();
        return count().map(count -> {
            if (count == 0)
                return 1; // a single page of zero results
            return (int) Math.ceil((double) count / (double) page.size);
        });
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

    public <T extends Entity> CommonReactivePanacheQueryImpl<T> range(int startIndex, int lastIndex) {
        this.range = Range.of(startIndex, lastIndex);
        // reset the page to its default to be able to switch from page to range
        this.page = null;
        return (CommonReactivePanacheQueryImpl<T>) this;
    }

    public <T extends Entity> CommonReactivePanacheQueryImpl<T> withCollation(Collation collation) {
        this.collation = collation;
        return (CommonReactivePanacheQueryImpl<T>) this;
    }

    public <T extends Entity> CommonReactivePanacheQueryImpl<T> withReadPreference(ReadPreference readPreference) {
        this.collection = this.collection.withReadPreference(readPreference);
        return (CommonReactivePanacheQueryImpl<T>) this;
    }

    // Results

    @SuppressWarnings("unchecked")
    public Uni<Long> count() {
        if (count == null) {
            count = mongoQuery == null ? collection.countDocuments() : collection.countDocuments(mongoQuery);
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Uni<List<T>> list() {
        Multi<T> results = stream();
        return results.collect().asList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Multi<T> stream() {
        FindOptions options = buildOptions();
        return mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
    }

    public <T extends Entity> Uni<T> firstResult() {
        Uni<Optional<T>> optionalEntity = firstResultOptional();
        return optionalEntity.map(optional -> optional.orElse(null));
    }

    public <T extends Entity> Uni<Optional<T>> firstResultOptional() {
        FindOptions options = buildOptions(1);
        Multi<T> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results.collect().first().map(o -> Optional.ofNullable(o));
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Uni<T> singleResult() {
        FindOptions options = buildOptions(2);
        Multi<T> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results.collect().asList().map(list -> {
            if (list.size() != 1) {
                throw new PanacheQueryException("There should be only one result");
            } else {
                return list.get(0);
            }
        });
    }

    public <T extends Entity> Uni<Optional<T>> singleResultOptional() {
        FindOptions options = buildOptions(2);
        Multi<T> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results.collect().asList().map(list -> {
            if (list.size() == 2) {
                throw new PanacheQueryException("There should be no more than one result");
            }
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        });
    }

    private FindOptions buildOptions() {
        FindOptions options = new FindOptions();
        options.sort(sort);
        if (range != null) {
            // range is 0 based, so we add 1 to the limit
            options.skip(range.getStartIndex()).limit(range.getLastIndex() - range.getStartIndex() + 1);
        } else if (page != null) {
            options.skip(page.index * page.size).limit(page.size);
        }
        if (projections != null) {
            options.projection(this.projections);
        }
        if (this.collation != null) {
            options.collation(collation);
        }
        return options;
    }

    private FindOptions buildOptions(int maxResults) {
        FindOptions options = new FindOptions();
        options.sort(sort);
        if (range != null) {
            // range is 0 based, so we add 1 to the limit
            options.skip(range.getStartIndex());
        } else if (page != null) {
            options.skip(page.index * page.size);
        }
        if (projections != null) {
            options.projection(this.projections);
        }
        if (this.collation != null) {
            options.collation(collation);
        }
        return options.limit(maxResults);
    }
}
