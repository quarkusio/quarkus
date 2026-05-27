package io.quarkus.hibernate.panache.runtime.orm;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.data.Limit;
import jakarta.data.page.PageRequest;
import jakarta.persistence.LockModeType;

import org.hibernate.SharedSessionContract;

import io.quarkus.hibernate.orm.panache.common.runtime.CommonPanacheQueryImpl;
import io.quarkus.hibernate.panache.blocking.PanacheBlockingQuery;
import io.quarkus.panache.common.Page;

public class PanacheBlockingQueryImpl<Entity> implements PanacheBlockingQuery<Entity> {

    final CommonPanacheQueryImpl<Entity> delegate;
    final Limits<PanacheBlockingQuery<Entity>> limitingDelegate = new Limits<PanacheBlockingQuery<Entity>>() {
        @Override
        public Limit limit() {
            io.quarkus.panache.common.Range range = delegate.range();
            // convert 0-based range to 1-based Jakarta Data Limit
            return Limit.range(range.getStartIndex() + 1, range.getLastIndex() + 1);
        }

        @Override
        public PanacheBlockingQuery<Entity> limit(Limit limit) {
            // startAt is 1-based in Jakarta Data, convert to 0-based; end is inclusive, hence -1 on size
            delegate.range((int) (limit.startAt() - 1), (int) (limit.startAt() + limit.maxResults() - 2));
            return PanacheBlockingQueryImpl.this;
        }

        @Override
        public PanacheBlockingQuery<Entity> limit(int max) {
            if (max == 0) {
                throw new IllegalArgumentException("Limiting to 0 values is not supported");
            }
            // end is inclusive, hence -1 on size
            delegate.range(0, max - 1);
            return PanacheBlockingQueryImpl.this;
        }

        @Override
        public PanacheBlockingQuery<Entity> limit(long start, int max) {
            // end is inclusive, hence -1 on size
            delegate.range((int) start, (int) (start + max - 1));
            return PanacheBlockingQueryImpl.this;
        }

        @Override
        public PanacheBlockingQuery<Entity> limitFrom(long start) {
            // end is inclusive, hence -1 on size
            // default page size of Jakarta Data is 10 (see PageRequest)
            delegate.range((int) start, (int) (start + 10 - 1));
            return PanacheBlockingQueryImpl.this;
        }

        @Override
        public PanacheBlockingQuery<Entity> range(long start, long end) {
            delegate.range((int) start, (int) end);
            return PanacheBlockingQueryImpl.this;
        }
    };
    final Pages<PanacheBlockingQuery<Entity>, PanacheBlockingQuery<Entity>, Boolean, Long> pagesDelegate = new Pages<PanacheBlockingQuery<Entity>, PanacheBlockingQuery<Entity>, Boolean, Long>() {
        @Override
        public PageRequest request() {
            Page page = delegate.page();
            // FIXME: let's hope they fix their page indices to 0-based
            return PageRequest.ofPage(page.index + 1, page.size, false);
        }

        @Override
        public PanacheBlockingQuery<Entity> request(PageRequest request) {
            // FIXME: let's hope they fix their page indices to 0-based
            delegate.page((int) (request.page() - 1), request.size());
            return PanacheBlockingQueryImpl.this;
        }

        @Override
        public PanacheBlockingQuery<Entity> page(long pageIndex, int pageSize) {
            delegate.page((int) pageIndex, pageSize);
            return PanacheBlockingQueryImpl.this;
        }

        @Override
        public PanacheBlockingQuery<Entity> cursor(long pageIndex, int pageSize, jakarta.data.Order<?> order) {
            List<org.hibernate.query.Order<?>> hibernateOrders = new java.util.ArrayList<>();
            for (jakarta.data.Sort<?> sort : order.sorts()) {
                org.hibernate.query.SortDirection direction = sort.isAscending()
                        ? org.hibernate.query.SortDirection.ASCENDING
                        : org.hibernate.query.SortDirection.DESCENDING;
                org.hibernate.query.Order<?> hibernateOrder = org.hibernate.query.Order
                        .by((Class) delegate.entityClass(), sort.property(), direction, sort.ignoreCase());
                hibernateOrders.add(hibernateOrder);
            }
            delegate.cursored((int) pageIndex, pageSize, hibernateOrders);
            return PanacheBlockingQueryImpl.this;
        }

        @Override
        public PanacheBlockingQuery<Entity> next() {
            delegate.nextPage();
            return PanacheBlockingQueryImpl.this;
        }

        @Override
        public PanacheBlockingQuery<Entity> previous() {
            delegate.previousPage();
            return PanacheBlockingQueryImpl.this;
        }

        @Override
        public PanacheBlockingQuery<Entity> first() {
            delegate.firstPage();
            return PanacheBlockingQueryImpl.this;
        }

        @Override
        public PanacheBlockingQuery<Entity> last() {
            delegate.lastPage();
            return PanacheBlockingQueryImpl.this;
        }

        @Override
        public Boolean hasNext() {
            return delegate.hasNextPage();
        }

        @Override
        public Boolean hasPrevious() {
            return delegate.hasPreviousPage();
        }

        @Override
        public Long count() {
            return (long) delegate.pageCount();
        }
    };

    PanacheBlockingQueryImpl(SharedSessionContract session, Class<?> entityClass, String query, String originalQuery,
            String orderBy,
            Object paramsArrayOrMap) {
        delegate = new CommonPanacheQueryImpl<Entity>(session, entityClass, query, originalQuery, orderBy, paramsArrayOrMap);
    }

    PanacheBlockingQueryImpl(CommonPanacheQueryImpl<Entity> delegate) {
        this.delegate = delegate;
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
    public Limits<PanacheBlockingQuery<Entity>> limits() {
        return limitingDelegate;
    }

    @Override
    public Pages<PanacheBlockingQuery<Entity>, PanacheBlockingQuery<Entity>, Boolean, Long> pages() {
        return pagesDelegate;
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
