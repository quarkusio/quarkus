package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import jakarta.persistence.LockModeType;

import org.hibernate.Filter;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Range;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;
import io.quarkus.panache.hibernate.common.runtime.ProjectionConstructorUtil;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public abstract class CommonAbstractPanacheQueryImpl<Entity, SessionType extends Mutiny.QueryProducer> {

    private Object paramsArrayOrMap;
    /**
     * this is the HQL query expanded from the Panache-Query
     */
    private String query;
    /**
     * this is the original Panache-Query, if any (can be null)
     */
    private String originalQuery;
    /**
     * This is only used by the Spring Data JPA extension, due to Spring's Query annotation allowing a custom count query
     * See https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.at-query.native
     * Otherwise we do not use this, and rely on ORM to generate count queries
     */
    protected String customCountQueryForSpring;
    private Sort sort;

    private Page page;
    private Uni<Long> count;

    private Range range;

    private LockModeType lockModeType;
    private Map<String, Object> hints;

    private Map<String, Map<String, Object>> filters;
    private Class<?> projectionType;

    protected Uni<SessionType> em;
    private Class<?> entityClass;

    public CommonAbstractPanacheQueryImpl(Uni<SessionType> em, Class<?> entityClass, String query, String originalQuery,
            Sort sort,
            Object paramsArrayOrMap) {
        this.em = em;
        this.entityClass = entityClass;
        this.query = query;
        this.originalQuery = originalQuery;
        this.sort = sort;
        this.paramsArrayOrMap = paramsArrayOrMap;
    }

    protected CommonAbstractPanacheQueryImpl(CommonAbstractPanacheQueryImpl<?, SessionType> previousQuery,
            String newQueryString,
            String customCountQueryForSpring,
            Class<?> projectionType) {
        this.em = previousQuery.em;
        this.entityClass = previousQuery.entityClass;
        this.query = newQueryString;
        this.customCountQueryForSpring = customCountQueryForSpring;
        this.sort = previousQuery.sort;
        this.paramsArrayOrMap = previousQuery.paramsArrayOrMap;
        this.page = previousQuery.page;
        this.count = previousQuery.count;
        this.range = previousQuery.range;
        this.lockModeType = previousQuery.lockModeType;
        this.hints = previousQuery.hints;
        this.filters = previousQuery.filters;
        this.projectionType = projectionType;
    }

    protected abstract <T> CommonAbstractPanacheQueryImpl<T, SessionType> newQuery(String query,
            String customCountQueryForSpring, Class<T> type);

    protected abstract Filter enableFilter(SessionType session, String filter);

    protected abstract void disableFilter(SessionType session, String filter);

    // Builder

    public <T> CommonAbstractPanacheQueryImpl<T, SessionType> project(Class<T> type) {
        String selectQuery = query;
        if (PanacheJpaUtil.isNamedQuery(query)) {
            selectQuery = NamedQueryUtil.getNamedQuery(query.substring(1));
        }

        String lowerCasedTrimmedQuery = PanacheJpaUtil.trimForAnalysis(selectQuery);
        if (lowerCasedTrimmedQuery.startsWith("select new ")
                || lowerCasedTrimmedQuery.startsWith("select distinct new ")) {
            throw new PanacheQueryException("Unable to perform a projection on a 'select [distinct]? new' query: " + query);
        }

        // If the query starts with a select clause, we pass it on to ORM which can handle that via a projection type
        if (lowerCasedTrimmedQuery.startsWith("select ")) {
            // I think projections do not change the result count, so we can keep the custom count query
            return newQuery(query, customCountQueryForSpring, type);
        }

        // FIXME: this assumes the query starts with "FROM " probably?

        // build select clause with a constructor expression
        String selectClause = "SELECT " + getParametersFromClass(type, null);
        // I think projections do not change the result count, so we can keep the custom count query
        return newQuery(selectClause + selectQuery, customCountQueryForSpring, null);
    }

    private StringBuilder getParametersFromClass(Class<?> type, String parentParameter) {
        BiFunction<Class<?>, String, String> nestedProjectionBuilder = (nestedType, parameterName) -> getParametersFromClass(
                nestedType, parameterName).toString();
        return new StringBuilder(
                ProjectionConstructorUtil.buildConstructorExpression(type, parentParameter, nestedProjectionBuilder));
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

    public Range range() {
        checkRange();
        return range;
    }

    public void range(int startIndex, int lastIndex) {
        this.range = Range.of(startIndex, lastIndex);
        // reset the page to its default to be able to switch from page to range
        this.page = null;
    }

    private void checkRange() {
        if (range == null) {
            throw new UnsupportedOperationException("Cannot call a range related method, " +
                    "call range(int, int) to initiate range first");
        }
        if (page != null) {
            throw new UnsupportedOperationException("Cannot call a range related method in a paged query, " +
                    "call range(int, int) to initiate range first");
        }
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
        if (count == null) {
            // FIXME: question about caching the result here
            count = em.flatMap(session -> {
                if (customCountQueryForSpring != null) {
                    Mutiny.SelectionQuery<Long> countQuery = session.createSelectionQuery(customCountQueryForSpring,
                            Long.class);
                    if (paramsArrayOrMap instanceof Map)
                        AbstractJpaOperations.bindParameters(countQuery, (Map<String, Object>) paramsArrayOrMap);
                    else
                        AbstractJpaOperations.bindParameters(countQuery, (Object[]) paramsArrayOrMap);
                    return applyFilters(session, () -> countQuery.getSingleResult());
                } else {
                    Mutiny.SelectionQuery<?> query = createBaseQuery(session);
                    return applyFilters(session, () -> query.getResultCount());
                }
            });
        }
        return count;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T extends Entity> Uni<List<T>> list() {
        return em.flatMap(session -> {
            Mutiny.SelectionQuery<?> hibernateQuery = createQuery(session);
            return (Uni) applyFilters(session, () -> hibernateQuery.getResultList());
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
            Mutiny.SelectionQuery<?> jpaQuery = createQuery(session, 1);
            return applyFilters(session, () -> jpaQuery.getResultList().map(list -> list.isEmpty() ? null : (T) list.get(0)));
        });
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Uni<T> singleResult() {
        return em.flatMap(session -> {
            Mutiny.SelectionQuery<?> jpaQuery = createQuery(session);
            return applyFilters(session, () -> jpaQuery.getSingleResult().map(v -> (T) v))
                    // FIXME: workaround https://github.com/hibernate/hibernate-reactive/issues/263
                    .onFailure(CompletionException.class).transform(t -> t.getCause());
        });
    }

    private Mutiny.SelectionQuery<?> createQuery(SessionType em) {
        Mutiny.SelectionQuery<?> jpaQuery = createBaseQuery(em);

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

    private Mutiny.SelectionQuery<?> createQuery(SessionType em, int maxResults) {
        Mutiny.SelectionQuery<?> jpaQuery = createBaseQuery(em);

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
    private Mutiny.SelectionQuery<?> createBaseQuery(SessionType em) {
        Mutiny.SelectionQuery<?> hibernateQuery;
        if (PanacheJpaUtil.isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            hibernateQuery = projectionType == null ? em.createNamedQuery(namedQuery)
                    : em.createNamedQuery(namedQuery, projectionType);
        } else {
            try {
                String orderBy = PanacheJpaUtil.toOrderBy(sort);
                hibernateQuery = em.createSelectionQuery(orderBy != null ? query + orderBy : query, projectionType);
            } catch (RuntimeException x) {
                throw NamedQueryUtil.checkForNamedQueryMistake(x, originalQuery);
            }
        }

        if (paramsArrayOrMap instanceof Map) {
            AbstractJpaOperations.bindParameters(hibernateQuery, (Map<String, Object>) paramsArrayOrMap);
        } else {
            AbstractJpaOperations.bindParameters(hibernateQuery, (Object[]) paramsArrayOrMap);
        }

        if (this.lockModeType != null) {
            hibernateQuery.setLockMode(lockModeType);
        }

        if (hints != null) {
            // FIXME: requires Hibernate support
            //            for (Map.Entry<String, Object> hint : hints.entrySet()) {
            //                jpaQuery.setHint(hint.getKey(), hint.getValue());
            //            }
        }
        return hibernateQuery;
    }

    private <T> Uni<T> applyFilters(SessionType em, Supplier<Uni<T>> uni) {
        if (filters == null)
            return uni.get();
        for (Entry<String, Map<String, Object>> entry : filters.entrySet()) {
            Filter filter = enableFilter(em, entry.getKey());
            for (Entry<String, Object> paramEntry : entry.getValue().entrySet()) {
                filter.setParameter(paramEntry.getKey(), paramEntry.getValue());
            }
            filter.validate();
        }
        return uni.get().onTermination().invoke(() -> {
            for (Entry<String, Map<String, Object>> entry : filters.entrySet()) {
                disableFilter(em, entry.getKey());
            }
        });
    }
}
