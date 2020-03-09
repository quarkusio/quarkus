package io.quarkus.mongodb.panache.reactive.runtime;

import java.util.List;
import java.util.Optional;

import org.bson.Document;

import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Range;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ReactivePanacheQueryImpl<Entity> implements ReactivePanacheQuery<Entity> {
    private ReactiveMongoCollection collection;
    private Document mongoQuery;
    private Document sort;

    /*
     * We store the pageSize and apply it for each request because getFirstResult()
     * sets the page size to 1
     */
    private Page page;
    private Uni<Long> count;

    private Range range;

    ReactivePanacheQueryImpl(ReactiveMongoCollection<? extends Entity> collection, Class<? extends Entity> entityClass,
            Document mongoQuery,
            Document sort) {
        this.collection = collection;
        this.mongoQuery = mongoQuery;
        this.sort = sort;
        page = new Page(0, Integer.MAX_VALUE);
    }

    // Builder

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> ReactivePanacheQuery<T> page(Page page) {
        this.page = page;
        this.range = null; // reset the range to be able to switch from range to page
        return (ReactivePanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> nextPage() {
        checkNotInRange();
        return page(page.next());
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> previousPage() {
        checkNotInRange();
        return page(page.previous());
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> firstPage() {
        checkNotInRange();
        return page(page.first());
    }

    @Override
    public <T extends Entity> Uni<ReactivePanacheQuery<T>> lastPage() {
        checkNotInRange();
        return pageCount().map(pageCount -> page(page.index(pageCount - 1)));
    }

    @Override
    public Uni<Boolean> hasNextPage() {
        checkNotInRange();
        return pageCount().map(pageCount -> page.index < (pageCount - 1));
    }

    @Override
    public boolean hasPreviousPage() {
        checkNotInRange();
        return page.index > 0;
    }

    @Override
    public Uni<Integer> pageCount() {
        checkNotInRange();
        return count().map(count -> {
            if (count == 0)
                return 1; // a single page of zero results
            return (int) Math.ceil((double) count / (double) page.size);
        });
    }

    @Override
    public Page page() {
        checkNotInRange();
        return page;
    }

    private void checkNotInRange() {
        if (range != null) {
            throw new UnsupportedOperationException("Cannot call a page related method in a ranged query, " +
                    "call page(Page) or page(int, int) to initiate pagination first");
        }
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> range(int startIndex, int lastIndex) {
        this.range = Range.of(startIndex, lastIndex);
        // reset the page to its default to be able to switch from page to range
        this.page = new Page(0, Integer.MAX_VALUE);
        return (ReactivePanacheQuery<T>) this;
    }

    // Results

    @Override
    @SuppressWarnings("unchecked")
    public Uni<Long> count() {
        if (count == null) {
            count = collection.countDocuments(mongoQuery);
        }
        return count;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Uni<List<T>> list() {
        FindOptions options = buildOptions();
        Multi<T> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results.collectItems().asList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Multi<T> stream() {
        FindOptions options = buildOptions();
        return mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
    }

    @Override
    public <T extends Entity> Uni<T> firstResult() {
        Uni<Optional<T>> optionalEntity = firstResultOptional();
        return optionalEntity.map(optional -> optional.orElse(null));
    }

    @Override
    public <T extends Entity> Uni<Optional<T>> firstResultOptional() {
        FindOptions options = buildOptions(1);
        Multi<T> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results.collectItems().first().map(o -> Optional.ofNullable(o));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Uni<T> singleResult() {
        FindOptions options = buildOptions(2);
        Multi<T> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results.collectItems().asList().map(list -> {
            if (list.size() != 1) {
                throw new PanacheQueryException("There should be only one result");
            } else {
                return list.get(0);
            }
        });
    }

    @Override
    public <T extends Entity> Uni<Optional<T>> singleResultOptional() {
        FindOptions options = buildOptions(2);
        Multi<T> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results.collectItems().asList().map(list -> {
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
        } else {
            options.skip(page.index * page.size).limit(page.size);
        }
        return options;
    }

    private FindOptions buildOptions(int maxResults) {
        FindOptions options = new FindOptions();
        options.sort(sort);
        if (range != null) {
            // range is 0 based, so we add 1 to the limit
            options.skip(range.getStartIndex()).limit(maxResults);
        } else {
            options.skip(page.index * page.size).limit(maxResults);
        }
        return options;
    }
}
