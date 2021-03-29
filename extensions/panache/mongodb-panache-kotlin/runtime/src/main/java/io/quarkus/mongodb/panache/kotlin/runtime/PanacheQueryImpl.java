package io.quarkus.mongodb.panache.kotlin.runtime;

import java.util.List;
import java.util.stream.Stream;

import org.bson.conversions.Bson;

import com.mongodb.ReadPreference;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;

import io.quarkus.mongodb.panache.kotlin.PanacheQuery;
import io.quarkus.mongodb.panache.runtime.CommonPanacheQueryImpl;
import io.quarkus.panache.common.Page;

public class PanacheQueryImpl<Entity> implements PanacheQuery<Entity> {
    private final CommonPanacheQueryImpl<Entity> delegate;

    PanacheQueryImpl(MongoCollection<? extends Entity> collection, ClientSession session, Bson mongoQuery, Bson sort) {
        this.delegate = new CommonPanacheQueryImpl<>(collection, session, mongoQuery, sort);
    }

    private PanacheQueryImpl(CommonPanacheQueryImpl<Entity> delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> PanacheQuery<T> project(Class<T> type) {
        return new PanacheQueryImpl<>(delegate.project(type));
    }

    @Override
    public PanacheQuery<Entity> page(Page page) {
        delegate.page(page);
        return this;
    }

    @Override
    public PanacheQuery<Entity> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    @Override
    public PanacheQuery<Entity> nextPage() {
        delegate.nextPage();
        return this;
    }

    @Override
    public PanacheQuery<Entity> previousPage() {
        delegate.previousPage();
        return this;
    }

    @Override
    public PanacheQuery<Entity> firstPage() {
        delegate.firstPage();
        return this;
    }

    @Override
    public PanacheQuery<Entity> lastPage() {
        delegate.lastPage();
        return this;
    }

    @Override
    public boolean hasNextPage() {
        return delegate.hasNextPage();
    }

    @Override
    public boolean hasPreviousPage() {
        return delegate.hasPreviousPage();
    }

    @Override
    public int pageCount() {
        return delegate.pageCount();
    }

    @Override
    public Page page() {
        return delegate.page();
    }

    @Override
    public PanacheQuery<Entity> range(int startIndex, int lastIndex) {
        delegate.range(startIndex, lastIndex);
        return this;
    }

    @Override
    public PanacheQuery<Entity> withCollation(Collation collation) {
        delegate.withCollation(collation);
        return this;
    }

    @Override
    public PanacheQuery<Entity> withReadPreference(ReadPreference readPreference) {
        delegate.withReadPreference(readPreference);
        return this;
    }

    // Results

    @Override
    public long count() {
        return delegate.count();
    }

    @Override
    public List<Entity> list() {
        return delegate.list();
    }

    @Override
    public Stream<Entity> stream() {
        return delegate.stream();
    }

    @Override
    public Entity firstResult() {
        return delegate.firstResult();
    }

    @Override
    public Entity singleResult() {
        return delegate.singleResult();
    }

}
