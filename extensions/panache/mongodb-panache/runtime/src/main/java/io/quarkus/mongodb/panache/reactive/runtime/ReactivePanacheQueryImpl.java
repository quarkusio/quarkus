package io.quarkus.mongodb.panache.reactive.runtime;

import java.util.List;
import java.util.Optional;

import org.bson.Document;

import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class ReactivePanacheQueryImpl<Entity> implements ReactivePanacheQuery<Entity> {
    private ReactiveMongoCollection<? extends Entity> collection;
    private Document mongoQuery;
    private Document sort;

    /*
     * We store the pageSize and apply it for each request because getFirstResult()
     * sets the page size to 1
     */
    private Page page;
    private Uni<Long> count;

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
        return (ReactivePanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> nextPage() {
        return page(page.next());
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> previousPage() {
        return page(page.previous());
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> firstPage() {
        return page(page.first());
    }

    @Override
    public <T extends Entity> Uni<ReactivePanacheQuery<T>> lastPage() {
        return pageCount().map(pageCount -> page(page.index(pageCount - 1)));
    }

    @Override
    public Uni<Boolean> hasNextPage() {
        return pageCount().map(pageCount -> page.index < (pageCount - 1));
    }

    @Override
    public boolean hasPreviousPage() {
        return page.index > 0;
    }

    @Override
    public Uni<Integer> pageCount() {
        return count().map(c -> {
            if (c == 0) {
                return 1; // a single page of zero results
            }
            return (int) Math.ceil((double) c / (double) page.size);
        });
    }

    @Override
    public Page page() {
        return page;
    }

    // Results

    @Override
    public Uni<Long> count() {
        if (count == null) {
            count = collection.countDocuments(mongoQuery);
        }
        return count;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends Entity> Uni<List<T>> list() {
        FindOptions options = new FindOptions();
        options.sort(sort).skip(page.index).limit(page.size);
        Multi results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results.collectItems().asList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Multi<T> asMulti() {
        FindOptions options = new FindOptions();
        options.sort(sort).skip(page.index).limit(page.size);
        return mongoQuery == null ? (Multi<T>) collection.find(options) : (Multi<T>) collection.find(mongoQuery, options);
    }

    @Override
    public <T extends Entity> Uni<T> firstResult() {
        Uni<Optional<T>> optionalEntity = firstResultOptional();
        return optionalEntity.map(optional -> optional.orElse(null));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Entity> Uni<Optional<T>> firstResultOptional() {
        FindOptions options = new FindOptions();
        options.sort(sort).skip(page.index).limit(1);
        Multi<?> multi = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return multi
                .collectItems().first()
                .map(x -> (T) x)
                .map(Optional::ofNullable);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Uni<T> singleResult() {
        FindOptions options = new FindOptions();
        options.sort(sort).skip(page.index).limit(2);
        Multi<?> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results
                .collectItems().asList()
                .onItem().produceUni(list -> {
                    if (list.size() != 1) {
                        return Uni.createFrom().failure(new PanacheQueryException("There should be only one result"));
                    } else {
                        return Uni.createFrom().item((T) list.get(0));
                    }
                });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Entity> Uni<Optional<T>> singleResultOptional() {
        FindOptions options = new FindOptions();
        options.sort(sort).skip(page.index).limit(2);
        Multi<?> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results
                .collectItems().asList()
                .onItem().produceUni(list -> {
                    if (list.size() > 1) {
                        return Uni.createFrom().failure(new PanacheQueryException("There should be only one result"));
                    } else if (list.isEmpty()) {
                        return Uni.createFrom().item(Optional.empty());
                    } else {
                        return Uni.createFrom().item(Optional.of((T) list.get(0)));
                    }
                });
    }
}
