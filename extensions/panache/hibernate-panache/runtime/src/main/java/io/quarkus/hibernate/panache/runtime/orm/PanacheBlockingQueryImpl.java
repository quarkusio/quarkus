package io.quarkus.hibernate.panache.runtime.orm;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import org.hibernate.SharedSessionContract;
import org.hibernate.query.Page;

import io.quarkus.hibernate.orm.panache.common.runtime.CommonPanacheQueryImpl;
import io.quarkus.hibernate.panache.blocking.PanacheBlockingQuery;

public class PanacheBlockingQueryImpl<Entity> implements PanacheBlockingQuery<Entity> {

    final CommonPanacheQueryImpl<Entity> delegate;

    PanacheBlockingQueryImpl(SharedSessionContract session, String query, String originalQuery, String orderBy,
            Object paramsArrayOrMap) {
        delegate = new CommonPanacheQueryImpl<Entity>(session, query, originalQuery, orderBy, paramsArrayOrMap);
    }

    PanacheBlockingQueryImpl(CommonPanacheQueryImpl<Entity> delegate) {
        this.delegate = delegate;
    }

    @Override
    public PanacheBlockingQueryImpl<Entity> page(Page page) {
        delegate.page(page.getNumber(), page.getSize());
        return this;
    }

    @Override
    public PanacheBlockingQueryImpl<Entity> page(int pageIndex, int pageSize) {
        delegate.page(pageIndex, pageSize);
        return this;
    }

    @Override
    public PanacheBlockingQueryImpl<Entity> nextPage() {
        delegate.nextPage();
        return this;
    }

    @Override
    public PanacheBlockingQueryImpl<Entity> previousPage() {
        delegate.previousPage();
        return this;
    }

    @Override
    public PanacheBlockingQueryImpl<Entity> firstPage() {
        delegate.firstPage();
        return this;
    }

    @Override
    public PanacheBlockingQueryImpl<Entity> lastPage() {
        delegate.lastPage();
        return this;
    }

    @Override
    public Boolean hasNextPage() {
        return delegate.hasNextPage();
    }

    @Override
    public Boolean hasPreviousPage() {
        return delegate.hasPreviousPage();
    }

    @Override
    public Long pageCount() {
        return (long) delegate.pageCount();
    }

    @Override
    public Page page() {
        return Page.page(delegate.page().size, delegate.page().index);
    }

    @Override
    public PanacheBlockingQueryImpl<Entity> range(int startIndex, int lastIndex) {
        delegate.range(startIndex, lastIndex);
        return this;
    }

    @Override
    public PanacheBlockingQueryImpl<Entity> withLock(LockModeType lockModeType) {
        delegate.withLock(lockModeType);
        return this;
    }

    @Override
    public PanacheBlockingQueryImpl<Entity> withHint(String hintName, Object value) {
        delegate.withHint(hintName, value);
        return this;
    }

    @Override
    public PanacheBlockingQueryImpl<Entity> filter(String filterName, Map<String, Object> parameters) {
        delegate.filter(filterName, parameters);
        return this;
    }

    @Override
    public PanacheBlockingQueryImpl<Entity> filter(String filterName) {
        delegate.filter(filterName, Collections.emptyMap());
        return this;
    }

    @Override
    public Long count() {
        return delegate.count();
    }

    @Override
    public List<Entity> list() {
        return delegate.list();
    }

    @Override
    public Entity firstResult() {
        return delegate.firstResult();
    }

    @Override
    public Entity singleResult() {
        return delegate.singleResult();
    }

    @Override
    public <NewEntity> PanacheBlockingQueryImpl<NewEntity> project(Class<NewEntity> type) {
        return new PanacheBlockingQueryImpl<>(delegate.project(type));
    }

    @Override
    public Stream<Entity> stream() {
        return delegate.stream();
    }

    @Override
    public Optional<Entity> firstResultOptional() {
        return delegate.firstResultOptional();
    }

    @Override
    public Optional<Entity> singleResultOptional() {
        return delegate.singleResultOptional();
    }

}
