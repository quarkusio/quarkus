package io.quarkus.data.hibernate.runtime.hr;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.data.Limit;
import jakarta.data.page.PageRequest;
import jakarta.persistence.LockModeType;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.data.hibernate.reactive.ReactiveDataQuery;
import io.quarkus.hibernate.reactive.panache.common.runtime.CommonManagedPanacheQueryImpl;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;

public class PanacheManagedReactiveQueryImpl<Entity> implements ReactiveDataQuery<Entity> {

    final CommonManagedPanacheQueryImpl<Entity> delegate;
    final Limits<ReactiveDataQuery<Entity>> limitingDelegate = new Limits<ReactiveDataQuery<Entity>>() {
        @Override
        public Limit limit() {
            io.quarkus.panache.common.Range range = delegate.range();
            // convert 0-based range to 1-based Jakarta Data Limit
            return Limit.range(range.getStartIndex() + 1, range.getLastIndex() + 1);
        }

        @Override
        public ReactiveDataQuery<Entity> limit(Limit limit) {
            // startAt is 1-based in Jakarta Data, convert to 0-based; end is inclusive, hence -1 on size
            delegate.range((int) (limit.startAt() - 1), (int) (limit.startAt() + limit.maxResults() - 2));
            return PanacheManagedReactiveQueryImpl.this;
        }

        @Override
        public ReactiveDataQuery<Entity> limit(int max) {
            if (max == 0) {
                throw new IllegalArgumentException("Limiting to 0 values is not supported");
            }
            // end is inclusive, hence -1 on size
            delegate.range(0, max - 1);
            return PanacheManagedReactiveQueryImpl.this;
        }

        @Override
        public ReactiveDataQuery<Entity> limit(long start, int max) {
            // end is inclusive, hence -1 on size
            delegate.range((int) start, (int) (start + max - 1));
            return PanacheManagedReactiveQueryImpl.this;
        }

        @Override
        public ReactiveDataQuery<Entity> limitFrom(long start) {
            // end is inclusive, hence -1 on size
            // default page size of Jakarta Data is 10 (see PageRequest)
            delegate.range((int) start, (int) (start + 10 - 1));
            return PanacheManagedReactiveQueryImpl.this;
        }

        @Override
        public ReactiveDataQuery<Entity> range(long start, long end) {
            delegate.range((int) start, (int) end);
            return PanacheManagedReactiveQueryImpl.this;
        }
    };
    final Pages<ReactiveDataQuery<Entity>, Uni<ReactiveDataQuery<Entity>>, Uni<Boolean>, Uni<Long>> pagesDelegate = new Pages<ReactiveDataQuery<Entity>, Uni<ReactiveDataQuery<Entity>>, Uni<Boolean>, Uni<Long>>() {
        @Override
        public PageRequest request() {
            Page page = delegate.page();
            // FIXME: let's hope they fix their page indices to 0-based
            return PageRequest.ofPage(page.index + 1, page.size, false);
        }

        @Override
        public ReactiveDataQuery<Entity> request(PageRequest request) {
            // FIXME: let's hope they fix their page indices to 0-based
            delegate.page((int) (request.page() - 1), request.size());
            return PanacheManagedReactiveQueryImpl.this;
        }

        @Override
        public ReactiveDataQuery<Entity> page(long pageIndex, int pageSize) {
            delegate.page((int) pageIndex, pageSize);
            return PanacheManagedReactiveQueryImpl.this;
        }

        @Override
        public ReactiveDataQuery<Entity> cursor(long pageIndex, int pageSize) {
            throw new UnsupportedOperationException("Cursor-based pagination is not supported by Hibernate Reactive");
        }

        @Override
        public ReactiveDataQuery<Entity> next() {
            delegate.nextPage();
            return PanacheManagedReactiveQueryImpl.this;
        }

        @Override
        public ReactiveDataQuery<Entity> previous() {
            delegate.previousPage();
            return PanacheManagedReactiveQueryImpl.this;
        }

        @Override
        public ReactiveDataQuery<Entity> first() {
            delegate.firstPage();
            return PanacheManagedReactiveQueryImpl.this;
        }

        @Override
        public Uni<ReactiveDataQuery<Entity>> last() {
            return delegate.lastPage().map(v -> PanacheManagedReactiveQueryImpl.this);
        }

        @Override
        public Uni<Boolean> hasNext() {
            return delegate.hasNextPage();
        }

        @Override
        public Uni<Boolean> hasPrevious() {
            return Uni.createFrom().item(delegate.hasPreviousPage());
        }

        @Override
        public Uni<Long> count() {
            return delegate.pageCount().map(i -> (long) i);
        }
    };

    PanacheManagedReactiveQueryImpl(Uni<Mutiny.Session> session, Class<?> entityClass, String query, String originalQuery,
            Sort sort,
            Object paramsArrayOrMap) {
        delegate = new CommonManagedPanacheQueryImpl<Entity>(session, entityClass, query, originalQuery, sort,
                paramsArrayOrMap);
    }

    PanacheManagedReactiveQueryImpl(CommonManagedPanacheQueryImpl<Entity> delegate) {
        this.delegate = delegate;
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
    public Limits<ReactiveDataQuery<Entity>> limits() {
        return limitingDelegate;
    }

    @Override
    public Pages<ReactiveDataQuery<Entity>, Uni<ReactiveDataQuery<Entity>>, Uni<Boolean>, Uni<Long>> pages() {
        return pagesDelegate;
    }

    @Override
    public <NewEntity> ReactiveDataQuery project(Class<NewEntity> type) {
        return new PanacheManagedReactiveQueryImpl<>(delegate.project(type));
    }
}
