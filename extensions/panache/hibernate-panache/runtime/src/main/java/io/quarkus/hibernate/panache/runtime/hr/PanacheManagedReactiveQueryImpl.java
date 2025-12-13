package io.quarkus.hibernate.panache.runtime.hr;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.LockModeType;

import org.hibernate.query.Page;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.panache.reactive.PanacheReactiveQuery;
import io.quarkus.hibernate.reactive.panache.common.runtime.CommonManagedPanacheQueryImpl;
import io.smallrye.mutiny.Uni;

public class PanacheManagedReactiveQueryImpl<Entity> implements PanacheReactiveQuery<Entity> {

    final CommonManagedPanacheQueryImpl<Entity> delegate;

    PanacheManagedReactiveQueryImpl(Uni<Mutiny.Session> session, String query, String originalQuery, String orderBy,
            Object paramsArrayOrMap) {
        delegate = new CommonManagedPanacheQueryImpl<Entity>(session, query, originalQuery, orderBy, paramsArrayOrMap);
    }

    PanacheManagedReactiveQueryImpl(CommonManagedPanacheQueryImpl<Entity> delegate) {
        this.delegate = delegate;
    }

    @Override
    public PanacheManagedReactiveQueryImpl<Entity> page(Page page) {
        delegate.page(page.getNumber(), page.getSize());
        return this;
    }

    @Override
    public PanacheManagedReactiveQueryImpl<Entity> page(int pageIndex, int pageSize) {
        delegate.page(pageIndex, pageSize);
        return this;
    }

    @Override
    public PanacheManagedReactiveQueryImpl<Entity> nextPage() {
        delegate.nextPage();
        return this;
    }

    @Override
    public PanacheManagedReactiveQueryImpl<Entity> previousPage() {
        delegate.previousPage();
        return this;
    }

    @Override
    public PanacheManagedReactiveQueryImpl<Entity> firstPage() {
        delegate.firstPage();
        return this;
    }

    @Override
    public PanacheManagedReactiveQueryImpl<Entity> lastPage() {
        delegate.lastPage();
        return this;
    }

    @Override
    public Uni<Boolean> hasNextPage() {
        return delegate.hasNextPage();
    }

    @Override
    public Uni<Boolean> hasPreviousPage() {
        return Uni.createFrom().item(delegate.hasPreviousPage());
    }

    @Override
    public Uni<Long> pageCount() {
        return delegate.pageCount().map(i -> i.longValue());
    }

    @Override
    public Page page() {
        return Page.page(delegate.page().size, delegate.page().index);
    }

    @Override
    public PanacheManagedReactiveQueryImpl<Entity> range(int startIndex, int lastIndex) {
        delegate.range(startIndex, lastIndex);
        return this;
    }

    @Override
    public PanacheManagedReactiveQueryImpl<Entity> withLock(LockModeType lockModeType) {
        delegate.withLock(lockModeType);
        return this;
    }

    @Override
    public PanacheManagedReactiveQueryImpl<Entity> withHint(String hintName, Object value) {
        delegate.withHint(hintName, value);
        return this;
    }

    @Override
    public PanacheManagedReactiveQueryImpl<Entity> filter(String filterName, Map<String, Object> parameters) {
        delegate.filter(filterName, parameters);
        return this;
    }

    @Override
    public PanacheManagedReactiveQueryImpl<Entity> filter(String filterName) {
        delegate.filter(filterName, Collections.emptyMap());
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
    public Uni<Entity> firstResult() {
        return delegate.firstResult();
    }

    @Override
    public Uni<Entity> singleResult() {
        return delegate.singleResult();
    }

    @Override
    public <NewEntity> PanacheReactiveQuery project(Class<NewEntity> type) {
        return new PanacheManagedReactiveQueryImpl<>(delegate.project(type));
    }
}
