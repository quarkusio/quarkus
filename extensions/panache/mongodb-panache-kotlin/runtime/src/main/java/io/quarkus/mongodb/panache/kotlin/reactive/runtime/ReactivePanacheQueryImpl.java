package io.quarkus.mongodb.panache.kotlin.reactive.runtime;

import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.ReadPreference;
import com.mongodb.client.model.Collation;

import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.panache.reactive.runtime.CommonReactivePanacheQueryImpl;
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
    public ReactivePanacheQuery<Entity> page(Page page) {
        delegate.page(page);
        return this;
    }

    @Override
    public ReactivePanacheQuery<Entity> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    @Override
    public ReactivePanacheQuery<Entity> nextPage() {
        delegate.nextPage();
        return this;
    }

    @Override
    public ReactivePanacheQuery<Entity> previousPage() {
        delegate.previousPage();
        return this;
    }

    @Override
    public ReactivePanacheQuery<Entity> firstPage() {
        delegate.firstPage();
        return this;
    }

    @Override
    public Uni<ReactivePanacheQuery<Entity>> lastPage() {
        Uni<CommonReactivePanacheQueryImpl<Entity>> uni = delegate.lastPage();
        return uni.map(q -> this);
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
    public ReactivePanacheQuery<Entity> range(int startIndex, int lastIndex) {
        delegate.range(startIndex, lastIndex);
        return this;
    }

    @Override
    public ReactivePanacheQuery<Entity> withCollation(Collation collation) {
        delegate.withCollation(collation);
        return this;
    }

    @Override
    public ReactivePanacheQuery<Entity> withReadPreference(ReadPreference readPreference) {
        delegate.withReadPreference(readPreference);
        return this;
    }

    @Override
    public Uni<Long> count() {
        return delegate.count();
    }

    @Override
    public Uni<List<Entity>> list() {
        return delegate.list();
    }

    @Override
    public Multi<Entity> stream() {
        return delegate.stream();
    }

    @Override
    public Uni<Entity> firstResult() {
        return delegate.firstResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Uni<Entity> singleResult() {
        return delegate.singleResult();
    }
}
