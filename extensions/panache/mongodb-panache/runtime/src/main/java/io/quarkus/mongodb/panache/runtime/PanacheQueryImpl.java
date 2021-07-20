package io.quarkus.mongodb.panache.runtime;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.bson.conversions.Bson;

import com.mongodb.ReadPreference;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.common.runtime.CommonPanacheQueryImpl;
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
    @SuppressWarnings("unchecked")
    public <T extends Entity> PanacheQuery<T> page(Page page) {
        delegate.page(page);
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> page(int pageIndex, int pageSize) {
        delegate.page(Page.of(pageIndex, pageSize));
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> nextPage() {
        delegate.nextPage();
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> previousPage() {
        delegate.previousPage();
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> firstPage() {
        delegate.firstPage();
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> lastPage() {
        delegate.lastPage();
        return (PanacheQuery<T>) this;
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
    public <T extends Entity> PanacheQuery<T> range(int startIndex, int lastIndex) {
        delegate.range(startIndex, lastIndex);
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> withCollation(Collation collation) {
        delegate.withCollation(collation);
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> withReadPreference(ReadPreference readPreference) {
        delegate.withReadPreference(readPreference);
        return (PanacheQuery<T>) this;
    }

    // Results

    @Override
    public long count() {
        return delegate.count();
    }

    @Override
    public <T extends Entity> List<T> list() {
        return delegate.list();
    }

    @Override
    public <T extends Entity> Stream<T> stream() {
        return delegate.stream();
    }

    @Override
    public <T extends Entity> T firstResult() {
        return delegate.firstResult();
    }

    @Override
    public <T extends Entity> Optional<T> firstResultOptional() {
        return delegate.firstResultOptional();
    }

    @Override
    public <T extends Entity> T singleResult() {
        return delegate.singleResult();
    }

    @Override
    public <T extends Entity> Optional<T> singleResultOptional() {
        return delegate.singleResultOptional();
    }
}
