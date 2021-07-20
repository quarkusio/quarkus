package io.quarkus.mongodb.panache.reactive.runtime;

import java.util.List;
import java.util.Optional;

import org.bson.conversions.Bson;

import com.mongodb.ReadPreference;
import com.mongodb.client.model.Collation;

import io.quarkus.mongodb.panache.common.reactive.runtime.CommonReactivePanacheQueryImpl;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@SuppressWarnings("unchecked")
public class ReactivePanacheQueryImpl<Entity> implements ReactivePanacheQuery<Entity> {
    private final CommonReactivePanacheQueryImpl<Entity> delegate;

    ReactivePanacheQueryImpl(ReactiveMongoCollection<? extends Entity> collection, Bson mongoQuery, Bson sort) {
        this.delegate = new CommonReactivePanacheQueryImpl<>(collection, mongoQuery, sort);
    }

    private ReactivePanacheQueryImpl(CommonReactivePanacheQueryImpl<Entity> delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> ReactivePanacheQuery<T> project(Class<T> type) {
        return new ReactivePanacheQueryImpl<>(delegate.project(type));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> ReactivePanacheQuery<T> page(Page page) {
        delegate.page(page);
        return (ReactivePanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> nextPage() {
        delegate.nextPage();
        return (ReactivePanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> previousPage() {
        delegate.previousPage();
        return (ReactivePanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> firstPage() {
        delegate.firstPage();
        return (ReactivePanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> Uni<ReactivePanacheQuery<T>> lastPage() {
        Uni<CommonReactivePanacheQueryImpl<T>> uni = delegate.lastPage();
        return uni.map(q -> (ReactivePanacheQuery<T>) this);
    }

    @Override
    public Uni<Boolean> hasNextPage() {
        return delegate.hasNextPage();
    }

    @Override
    public boolean hasPreviousPage() {
        return delegate.hasPreviousPage();
    }

    @Override
    public Uni<Integer> pageCount() {
        return delegate.pageCount();
    }

    @Override
    public Page page() {
        return delegate.page();
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> range(int startIndex, int lastIndex) {
        delegate.range(startIndex, lastIndex);
        return (ReactivePanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> withCollation(Collation collation) {
        delegate.withCollation(collation);
        return (ReactivePanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> ReactivePanacheQuery<T> withReadPreference(ReadPreference readPreference) {
        delegate.withReadPreference(readPreference);
        return (ReactivePanacheQuery<T>) this;
    }

    @Override
    public Uni<Long> count() {
        return delegate.count();
    }

    @Override
    public <T extends Entity> Uni<List<T>> list() {
        return delegate.list();
    }

    @Override
    public <T extends Entity> Multi<T> stream() {
        return delegate.stream();
    }

    @Override
    public <T extends Entity> Uni<T> firstResult() {
        return delegate.firstResult();
    }

    @Override
    public <T extends Entity> Uni<Optional<T>> firstResultOptional() {
        return delegate.firstResultOptional();
    }

    @Override
    public <T extends Entity> Uni<T> singleResult() {
        return delegate.singleResult();
    }

    @Override
    public <T extends Entity> Uni<Optional<T>> singleResultOptional() {
        return delegate.singleResultOptional();
    }
}
