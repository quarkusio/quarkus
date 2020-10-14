package io.quarkus.hibernate.orm.panache.common.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import org.hibernate.Filter;
import org.hibernate.Session;
import org.hibernate.engine.spi.RowSelection;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Range;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;

public class CommonPanacheQueryImpl<Entity> {

    private interface NonThrowingCloseable extends AutoCloseable {
        @Override
        void close();
    }

    private static final NonThrowingCloseable NO_FILTERS = new NonThrowingCloseable() {
        @Override
        public void close() {
        }
    };

    private static final String SELECT = "select";

    private Object paramsArrayOrMap;
    private String query;
    protected String countQuery;
    private String orderBy;
    private EntityManager em;

    private Page page;
    private Long count;

    private Range range;

    private LockModeType lockModeType;
    private Map<String, Object> hints;

    private Map<String, Map<String, Object>> filters;

    private Function<Object[], Entity> rowMapper;

    public CommonPanacheQueryImpl(EntityManager em, String query, String orderBy, Object paramsArrayOrMap) {
        this.em = em;
        this.query = query;
        this.orderBy = orderBy;
        this.paramsArrayOrMap = paramsArrayOrMap;
    }

    private CommonPanacheQueryImpl(CommonPanacheQueryImpl<?> previousQuery, String newQueryString, String countQuery,
            Function<Object[], Entity> rowMapper) {
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
        this.rowMapper = rowMapper;
    }

    // Builder

    public <T> CommonPanacheQueryImpl<T> project(Class<T> type) {
        if (PanacheJpaUtil.isNamedQuery(query)) {
            throw new PanacheQueryException("Unable to perform a projection on a named query");
        }
        if (query != null && query.toLowerCase().startsWith(SELECT)) {
            throw new PanacheQueryException("Cannot perform projection on a query with an existing select clause: " + query);
        }
        // We use the first constructor that we found and use the parameter names,
        // so the projection class must have only one constructor,
        // and the application must be built with parameter names.
        // Maybe this should be improved some days ...
        Constructor<?> constructor = type.getDeclaredConstructors()[0];

        // build select clause with a constructor expression
        StringBuilder select = new StringBuilder(SELECT).append(" new ").append(type.getName()).append(" (");
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

        return new CommonPanacheQueryImpl<>(this, select.toString() + query, "select count(*) " + query, null);
    }

    public <T> CommonPanacheQueryImpl<T> project(List<String> properties, Function<Object[], T> rowMapper) {
        if (PanacheJpaUtil.isNamedQuery(query)) {
            throw new PanacheQueryException("Unable to perform a select on a named query");
        }
        if (query != null && query.toLowerCase().startsWith(SELECT)) {
            throw new PanacheQueryException("Cannot perform projection on a query with an existing select clause: " + query);
        }
        if (properties.isEmpty()) {
            throw new PanacheQueryException("At least one property reference must be set");
        }
        if (rowMapper == null) {
            throw new PanacheQueryException("A row mapper must be set");
        }
        StringBuilder select = new StringBuilder(SELECT).append(" ");
        for (Iterator<String> it = properties.iterator(); it.hasNext();) {
            select.append(it.next());
            if (it.hasNext()) {
                select.append(",");
            } else {
                select.append(" ");
            }
        }
        return new CommonPanacheQueryImpl<>(this, select.toString() + query, "select count(*) " + query, rowMapper);
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

    public void lastPage() {
        checkPagination();
        page(page.index(pageCount() - 1));
    }

    public boolean hasNextPage() {
        checkPagination();
        return page.index < (pageCount() - 1);
    }

    public boolean hasPreviousPage() {
        checkPagination();
        return page.index > 0;
    }

    public int pageCount() {
        checkPagination();
        long count = count();
        if (count == 0)
            return 1; // a single page of zero results
        return (int) Math.ceil((double) count / (double) page.size);
    }

    public Page page() {
        checkPagination();
        return page;
    }

    private void checkPagination() {
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
    }

    // Results

    @SuppressWarnings("unchecked")
    public long count() {
        if (count == null) {
            String selectQuery = query;
            if (PanacheJpaUtil.isNamedQuery(query)) {
                org.hibernate.query.Query q = (org.hibernate.query.Query) em.createNamedQuery(query.substring(1));
                selectQuery = q.getQueryString();
            }

            Query countQuery = em.createQuery(countQuery(selectQuery));
            if (paramsArrayOrMap instanceof Map)
                AbstractJpaOperations.bindParameters(countQuery, (Map<String, Object>) paramsArrayOrMap);
            else
                AbstractJpaOperations.bindParameters(countQuery, (Object[]) paramsArrayOrMap);
            try (NonThrowingCloseable c = applyFilters()) {
                count = (Long) countQuery.getSingleResult();
            }
        }
        return count;
    }

    private String countQuery(String selectQuery) {
        if (countQuery != null) {
            return countQuery;
        }

        return PanacheJpaUtil.getCountQuery(selectQuery);
    }

    @SuppressWarnings({ "unchecked" })
    public <T extends Entity> List<T> list() {
        Query jpaQuery = createQuery();
        try (NonThrowingCloseable c = applyFilters()) {
            List<T> results = jpaQuery.getResultList();
            return mapRows(results);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream() {
        Query jpaQuery = createQuery();
        try (NonThrowingCloseable c = applyFilters()) {
            if (rowMapper != null) {
                return jpaQuery.getResultStream().map(r -> mapRow((T) r));
            }
            return jpaQuery.getResultStream();
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T firstResult() {
        Query jpaQuery = createQuery(1);
        try (NonThrowingCloseable c = applyFilters()) {
            List<T> results = jpaQuery.getResultList();
            if (results.isEmpty()) {
                return null;
            }
            return mapRow(results.get(0));
        }
    }

    public <T extends Entity> Optional<T> firstResultOptional() {
        return Optional.ofNullable(firstResult());
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T singleResult() {
        Query jpaQuery = createQuery();
        try (NonThrowingCloseable c = applyFilters()) {
            return mapRow((T) jpaQuery.getSingleResult());
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Optional<T> singleResultOptional() {
        Query jpaQuery = createQuery(2);
        try (NonThrowingCloseable c = applyFilters()) {
            List<T> list = jpaQuery.getResultList();
            if (list.size() > 1) {
                throw new NonUniqueResultException();
            }
            return list.isEmpty() ? Optional.empty() : Optional.of(mapRow(list.get(0)));
        }
    }

    private Query createQuery() {
        Query jpaQuery = createBaseQuery();

        if (range != null) {
            jpaQuery.setFirstResult(range.getStartIndex());
            // range is 0 based, so we add 1
            jpaQuery.setMaxResults(range.getLastIndex() - range.getStartIndex() + 1);
        } else if (page != null) {
            jpaQuery.setFirstResult(page.index * page.size);
            jpaQuery.setMaxResults(page.size);
        } else {
            // Use deprecated API in org.hibernate.Query that will be moved to org.hibernate.query.Query on Hibernate 6.0
            @SuppressWarnings("deprecation")
            RowSelection options = jpaQuery.unwrap(org.hibernate.query.Query.class).getQueryOptions();
            options.setFirstRow(null);
            options.setMaxRows(null);
        }

        return jpaQuery;
    }

    private Query createQuery(int maxResults) {
        Query jpaQuery = createBaseQuery();

        if (range != null) {
            jpaQuery.setFirstResult(range.getStartIndex());
        } else if (page != null) {
            jpaQuery.setFirstResult(page.index * page.size);
        } else {
            // Use deprecated API in org.hibernate.Query that will be moved to org.hibernate.query.Query on Hibernate 6.0
            @SuppressWarnings("deprecation")
            RowSelection options = jpaQuery.unwrap(org.hibernate.query.Query.class).getQueryOptions();
            options.setFirstRow(null);
        }
        jpaQuery.setMaxResults(maxResults);

        return jpaQuery;
    }

    @SuppressWarnings("unchecked")
    private Query createBaseQuery() {
        Query jpaQuery;
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
            jpaQuery.setLockMode(lockModeType);
        }

        if (hints != null) {
            for (Map.Entry<String, Object> hint : hints.entrySet()) {
                jpaQuery.setHint(hint.getKey(), hint.getValue());
            }
        }
        return jpaQuery;
    }

    private NonThrowingCloseable applyFilters() {
        if (filters == null)
            return NO_FILTERS;
        Session session = em.unwrap(Session.class);
        for (Entry<String, Map<String, Object>> entry : filters.entrySet()) {
            Filter filter = session.enableFilter(entry.getKey());
            for (Entry<String, Object> paramEntry : entry.getValue().entrySet()) {
                filter.setParameter(paramEntry.getKey(), paramEntry.getValue());
            }
            filter.validate();
        }
        return new NonThrowingCloseable() {
            @Override
            public void close() {
                for (Entry<String, Map<String, Object>> entry : filters.entrySet()) {
                    session.disableFilter(entry.getKey());
                }
            }
        };
    }

    private <T extends Entity> List<T> mapRows(List<T> results) {
        if (rowMapper != null) {
            List<T> mappedResults = new ArrayList<>(results.size());
            for (T result : results) {
                mappedResults.add((mapRow(result)));
            }
            results = mappedResults;
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity> T mapRow(T result) {
        if (rowMapper != null) {
            Object[] row = result.getClass().isArray() ? (Object[]) result : new Object[] { result };
            result = (T) rowMapper.apply(row);
        }
        return result;
    }
}
