package io.quarkus.hibernate.orm.panache.kotlin.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.jetbrains.annotations.NotNull;

import io.quarkus.hibernate.orm.panache.common.runtime.CommonPanacheQueryImpl;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;

public class PanacheQueryImpl<Entity> implements PanacheQuery<Entity> {

    private CommonPanacheQueryImpl<Entity> delegate;

    PanacheQueryImpl(EntityManager em, String query, String orderBy, Object paramsArrayOrMap) {
        this.delegate = new CommonPanacheQueryImpl<>(em, query, orderBy, paramsArrayOrMap);
    }

    protected PanacheQueryImpl(CommonPanacheQueryImpl<Entity> delegate) {
        this.delegate = delegate;
    }

    // Builder

    @NotNull
    @Override
    public <NewEntity> PanacheQuery<NewEntity> project(Class<NewEntity> type) {
        return new PanacheQueryImpl<>(delegate.project(type));
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> page(@NotNull Page page) {
        delegate.page(page);
        return this;
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> page(int pageIndex, int pageSize) {
        delegate.page(pageIndex, pageSize);
        return this;
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> nextPage() {
        delegate.nextPage();
        return this;
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> previousPage() {
        delegate.previousPage();
        return this;
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> firstPage() {
        delegate.firstPage();
        return this;
    }

    @NotNull
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

    @NotNull
    @Override
    public Page page() {
        return delegate.page();
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> range(int startIndex, int lastIndex) {
        delegate.range(startIndex, lastIndex);
        return this;
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> withLock(@NotNull LockModeType lockModeType) {
        delegate.withLock(lockModeType);
        return this;
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> withHint(@NotNull String hintName, @NotNull Object value) {
        delegate.withHint(hintName, value);
        return this;
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> filter(@NotNull String filterName, @NotNull Parameters parameters) {
        delegate.filter(filterName, parameters.map());
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @NotNull
    @Override
    public PanacheQuery<Entity> filter(@NotNull String filterName, @NotNull Map<String, ? extends Object> parameters) {
        delegate.filter(filterName, (Map) parameters);
        return this;
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> filter(@NotNull String filterName) {
        delegate.filter(filterName, Collections.emptyMap());
        return this;
    }

    // Results

    @Override
    public long count() {
        return delegate.count();
    }

    @NotNull
    @Override
    public List<Entity> list() {
        return delegate.list();
    }

    @NotNull
    @Override
    public Stream<Entity> stream() {
        return delegate.stream();
    }

    @Override
    public Entity firstResult() {
        return delegate.firstResult();
    }

    @NotNull
    @Override
    public Entity singleResult() {
        return delegate.singleResult();
    }
}
