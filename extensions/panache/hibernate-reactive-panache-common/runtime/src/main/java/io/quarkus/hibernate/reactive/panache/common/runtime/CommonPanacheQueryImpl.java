package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import javax.persistence.LockModeType;

import org.hibernate.Filter;
import org.hibernate.internal.util.LockModeConverter;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Range;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class CommonPanacheQueryImpl<Entity> {

    private Object paramsArrayOrMap;
    private String query;
    protected String countQuery;
    private String orderBy;
    private Uni<Mutiny.Session> em;

    private Page page;
    private Uni<Long> count;

    private Range range;

    private LockModeType lockModeType;
    private Map<String, Object> hints;

    private Map<String, Map<String, Object>> filters;

    public CommonPanacheQueryImpl(Uni<Mutiny.Session> em, String query, String orderBy, Object paramsArrayOrMap) {
        this.em = em;
        this.query = query;
        this.orderBy = orderBy;
        this.paramsArrayOrMap = paramsArrayOrMap;
    }

    private CommonPanacheQueryImpl(CommonPanacheQueryImpl<?> previousQuery, String newQueryString, String countQuery) {
        this.em = previousQuery.em;
        this.query = newQueryString;
        this.countQuery = countQuery;
        this.orderBy = previousQuery.orderBy;
        this.paramsArrayOrMap = previousQuery.paramsArrayOrMap;
        this.page = previousQuery.page;
        this.count = previousQuery.count;
        this.range = previousQuery.range;
        this.lockModeType = previousQuery.lockModeType;
        this.hints = previousQuery.hints;
        this.filters = previousQuery.filters;
    }

    // Builder

    public <T> CommonPanacheQueryImpl<T> project(Class<T> type) {
        if (PanacheJpaUtil.isNamedQuery(query)) {
            throw new PanacheQueryException("Unable to perform a projection on a named query");
        }

        // We use the first constructor that we found and use the parameter names,
        // so the projection class must have only one constructor,
        // and the application must be built with parameter names.
        // Maybe this should be improved some days ...
        Constructor<?> constructor = type.getDeclaredConstructors()[0];

        // build select clause with a constructor expression
        StringBuilder select = new StringBuilder("SELECT new ").append(type.getName()).append(" (");
        int selectInitialLength = select.length();
        for (Parameter parameter : constructor.getParameters()) {
            if (!parameter.isNamePresent()) {
                throw new PanacheQueryException(
                        "Your application must be built with parameter names, this should be the default if" +
                                " using Quarkus artifacts. Check the maven or gradle compiler configuration to include '-parameters'.");
            }

            if (select.length() > selectInitialLength) {
                select.append(", ");
            }
            select.append(parameter.getName());
        }
        select.append(") ");

        return new CommonPanacheQueryImpl<>(this, select.toString() + query, "select count(*) " + query);
    }

    public void filter(String filterName, Map<String, Object> parameters) {
        if (filters == null)
            filters = new HashMap<>();
        filters.put(filterName, parameters);
    }

    public void page(Page page) {
        this.page = page;
        this.range = null; // reset the range to be able to switch from range to page
    }

    public void page(int pageIndex, int pageSize) {
        page(Page.of(pageIndex, pageSize));
    }

    public void nextPage() {
        checkPagination();
        page(page.next());
    }

    public void previousPage() {
        checkPagination();
        page(page.previous());
    }

    public void firstPage() {
        checkPagination();
        page(page.first());
    }

    public Uni<Void> lastPage() {
        checkPagination();
        return pageCount().map(count -> {
            page(page.index(count - 1));
            return null;
        });
    }

    public Uni<Boolean> hasNextPage() {
        checkPagination();
        return pageCount().map(pageCount -> page.index < (pageCount - 1));
    }

    public boolean hasPreviousPage() {
        checkPagination();
        return page.index > 0;
    }

    public Uni<Integer> pageCount() {
        checkPagination();
        return count().map(count -> {
            if (count == 0)
                return 1; // a single page of zero results
            return (int) Math.ceil((double) count / (double) page.size);
        });
    }

    public Page page() {
        checkPagination();
        return page;
    }

    private void checkPagination() {
        // FIXME: turn into Uni
        if (page == null) {
            throw new UnsupportedOperationException("Cannot call a page related method, " +
                    "call page(Page) or page(int, int) to initiate pagination first");
        }
        if (range != null) {
            throw new UnsupportedOperationException("Cannot call a page related method in a ranged query, " +
                    "call page(Page) or page(int, int) to initiate pagination first");
        }
    }

    public void range(int startIndex, int lastIndex) {
        this.range = Range.of(startIndex, lastIndex);
        // reset the page to its default to be able to switch from page to range
        this.page = null;
    }

    public void withLock(LockModeType lockModeType) {
        this.lockModeType = lockModeType;
    }

    public void withHint(String hintName, Object value) {
        if (hints == null) {
            hints = new HashMap<>();
        }
        hints.put(hintName, value);
        throw new UnsupportedOperationException("Hints not supported yet");
    }

    // Results

    @SuppressWarnings("unchecked")
    public Uni<Long> count() {
        if (PanacheJpaUtil.isNamedQuery(query)) {
            throw new PanacheQueryException("Unable to perform a count operation on a named query");
        }

        if (count == null) {
            // FIXME: question about caching the result here
            count = em.flatMap(session -> {
                Mutiny.Query<Long> countQuery = session.createQuery(countQuery());
                if (paramsArrayOrMap instanceof Map)
                    AbstractJpaOperations.bindParameters(countQuery, (Map<String, Object>) paramsArrayOrMap);
                else
                    AbstractJpaOperations.bindParameters(countQuery, (Object[]) paramsArrayOrMap);
                return applyFilters(session, () -> countQuery.getSingleResult());
            });
        }
        return count;
    }

    private String countQuery() {
        if (countQuery != null) {
            return countQuery;
        }
        return PanacheJpaUtil.getCountQuery(query);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends Entity> Uni<List<T>> list() {
        return em.flatMap(session -> {
            Mutiny.Query<?> jpaQuery = createQuery(session);
            return (Uni) applyFilters(session, () -> jpaQuery.getResultList());
        });
    }

    public <T extends Entity> Multi<T> stream() {
        // FIXME: requires Hibernate support
        //        Mutiny.Query<?> jpaQuery = createQuery();
        //        return applyFilters(jpaQuery.getResultStream());
        Uni<List<T>> results = list();
        return (Multi<T>) results.toMulti().flatMap(list -> {
            return Multi.createFrom().iterable(list);
        });
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Uni<T> firstResult() {
        return em.flatMap(session -> {
            Mutiny.Query<?> jpaQuery = createQuery(session, 1);
            return applyFilters(session, () -> jpaQuery.getResultList().map(list -> list.isEmpty() ? null : (T) list.get(0)));
        });
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Uni<T> singleResult() {
        return em.flatMap(session -> {
            Mutiny.Query<?> jpaQuery = createQuery(session);
            return applyFilters(session, () -> jpaQuery.getSingleResult().map(v -> (T) v))
                    // FIXME: workaround https://github.com/hibernate/hibernate-reactive/issues/263
                    .onFailure(CompletionException.class).transform(t -> t.getCause());
        });
    }

    private Mutiny.Query<?> createQuery(Mutiny.Session em) {
        Mutiny.Query<?> jpaQuery = createBaseQuery(em);

        if (range != null) {
            jpaQuery.setFirstResult(range.getStartIndex());
            // range is 0 based, so we add 1
            jpaQuery.setMaxResults(range.getLastIndex() - range.getStartIndex() + 1);
        } else if (page != null) {
            jpaQuery.setFirstResult(page.index * page.size);
            jpaQuery.setMaxResults(page.size);
        } else {
            // Use deprecated API in org.hibernate.Query that will be moved to org.hibernate.query.Query on Hibernate 6.0
            // FIXME: requires Hibernate support
            //            @SuppressWarnings("deprecation")
            //            RowSelection options = jpaQuery.unwrap(org.hibernate.query.Query.class).getQueryOptions();
            //            options.setFirstRow(null);
            //            options.setMaxRows(null);
            // FIXME: why would we even do that? those are the defaults, let's leave them blank
            // if we don't, we get a LIMIT
            //            jpaQuery.setFirstResult(0);
            //            jpaQuery.setMaxResults(Integer.MAX_VALUE);
        }

        return jpaQuery;
    }

    private Mutiny.Query<?> createQuery(Mutiny.Session em, int maxResults) {
        Mutiny.Query<?> jpaQuery = createBaseQuery(em);

        if (range != null) {
            jpaQuery.setFirstResult(range.getStartIndex());
        } else if (page != null) {
            jpaQuery.setFirstResult(page.index * page.size);
        } else {
            // Use deprecated API in org.hibernate.Query that will be moved to org.hibernate.query.Query on Hibernate 6.0
            // FIXME: requires Hibernate support
            //            @SuppressWarnings("deprecation")
            //            RowSelection options = jpaQuery.unwrap(org.hibernate.query.Query.class).getQueryOptions();
            //            options.setFirstRow(null);
            jpaQuery.setFirstResult(0);
        }
        jpaQuery.setMaxResults(maxResults);

        return jpaQuery;
    }

    @SuppressWarnings("unchecked")
    private Mutiny.Query<?> createBaseQuery(Mutiny.Session em) {
        Mutiny.Query<?> jpaQuery;
        if (PanacheJpaUtil.isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            jpaQuery = em.createNamedQuery(namedQuery);
        } else {
            jpaQuery = em.createQuery(orderBy != null ? query + orderBy : query);
        }

        if (paramsArrayOrMap instanceof Map) {
            AbstractJpaOperations.bindParameters(jpaQuery, (Map<String, Object>) paramsArrayOrMap);
        } else {
            AbstractJpaOperations.bindParameters(jpaQuery, (Object[]) paramsArrayOrMap);
        }

        if (this.lockModeType != null) {
            jpaQuery.setLockMode(LockModeConverter.convertToLockMode(lockModeType));
        }

        if (hints != null) {
            // FIXME: requires Hibernate support
            //            for (Map.Entry<String, Object> hint : hints.entrySet()) {
            //                jpaQuery.setHint(hint.getKey(), hint.getValue());
            //            }
        }
        return jpaQuery;
    }

    private <T> Uni<T> applyFilters(Mutiny.Session em, Supplier<Uni<T>> uni) {
        if (filters == null)
            return uni.get();
        for (Entry<String, Map<String, Object>> entry : filters.entrySet()) {
            Filter filter = em.enableFilter(entry.getKey());
            for (Entry<String, Object> paramEntry : entry.getValue().entrySet()) {
                filter.setParameter(paramEntry.getKey(), paramEntry.getValue());
            }
            filter.validate();
        }
        return uni.get().onTermination().invoke(() -> {
            for (Entry<String, Map<String, Object>> entry : filters.entrySet()) {
                em.disableFilter(entry.getKey());
            }
        });
    }
}
