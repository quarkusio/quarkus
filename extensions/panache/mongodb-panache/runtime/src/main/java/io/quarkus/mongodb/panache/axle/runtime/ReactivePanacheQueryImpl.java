package io.quarkus.mongodb.panache.axle.runtime;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.bson.Document;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.reactivestreams.Publisher;

import io.quarkus.mongodb.FindOptions;
import io.quarkus.mongodb.ReactiveMongoCollection;
import io.quarkus.mongodb.panache.axle.ReactivePanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.exception.PanacheQueryException;

public class ReactivePanacheQueryImpl<Entity> implements ReactivePanacheQuery<Entity> {
    private ReactiveMongoCollection collection;
    private Class<? extends Entity> entityClass;
    private Document mongoQuery;
    private Document sort;

    /*
     * We store the pageSize and apply it for each request because getFirstResult()
     * sets the page size to 1
     */
    private Page page;
    private CompletionStage<Long> count;

    ReactivePanacheQueryImpl(ReactiveMongoCollection<? extends Entity> collection, Class<? extends Entity> entityClass,
            Document mongoQuery,
            Document sort) {
        this.collection = collection;
        this.entityClass = entityClass;
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
    public <T extends Entity> CompletionStage<ReactivePanacheQuery<T>> lastPage() {
        return pageCount().thenApply(pageCount -> {
            return page(page.index(pageCount - 1));
        });
    }

    @Override
    public CompletionStage<Boolean> hasNextPage() {
        return pageCount().thenApply(pageCount -> {
            return page.index < (pageCount - 1);
        });
    }

    @Override
    public boolean hasPreviousPage() {
        return page.index > 0;
    }

    @Override
    public CompletionStage<Integer> pageCount() {
        return count().thenApply(count -> {
            if (count == 0)
                return 1; // a single page of zero results
            return (int) Math.ceil((double) count / (double) page.size);
        });
    }

    @Override
    public Page page() {
        return page;
    }

    // Results

    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<Long> count() {
        if (count == null) {
            count = collection.countDocuments(mongoQuery);
        }
        return count;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> CompletionStage<List<T>> list() {
        FindOptions options = new FindOptions();
        options.sort(sort).skip(page.index).limit(page.size);
        PublisherBuilder<T> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results.toList().run();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Publisher<T> stream() {
        FindOptions options = new FindOptions();
        options.sort(sort).skip(page.index).limit(page.size);
        return mongoQuery == null ? collection.find(options).buildRs() : collection.find(mongoQuery, options).buildRs();
    }

    @Override
    public <T extends Entity> CompletionStage<T> firstResult() {
        CompletionStage<Optional<T>> optionalEntity = firstResultOptional();
        return optionalEntity.thenApply(optional -> optional.orElse(null));
    }

    @Override
    public <T extends Entity> CompletionStage<Optional<T>> firstResultOptional() {
        FindOptions options = new FindOptions();
        options.sort(sort).skip(page.index).limit(1);
        return mongoQuery == null ? collection.find(options).findFirst().run()
                : collection.find(mongoQuery, options).findFirst().run();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> CompletionStage<T> singleResult() {
        FindOptions options = new FindOptions();
        options.sort(sort).skip(page.index).limit(2);
        PublisherBuilder<T> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results.toList().run().thenApply(list -> {
            if (list.size() == 0 || list.size() > 1) {
                throw new PanacheQueryException("There should be only one result");
            } else {
                return list.get(0);
            }
        });
    }

    @Override
    public <T extends Entity> CompletionStage<Optional<T>> singleResultOptional() {
        FindOptions options = new FindOptions();
        options.sort(sort).skip(page.index).limit(2);
        PublisherBuilder<T> results = mongoQuery == null ? collection.find(options) : collection.find(mongoQuery, options);
        return results.toList().run().thenApply(list -> {
            if (list.size() == 2) {
                throw new PanacheQueryException("There should be no more than one result");
            }
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        });
    }
}
