package io.quarkus.hibernate.orm.panache.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import org.hibernate.engine.spi.RowSelection;
import org.jetbrains.annotations.NotNull;

import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Range;
import io.quarkus.panache.common.exception.PanacheQueryException;

public class PanacheQueryImpl<Entity> implements PanacheQuery<Entity> {

    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s+((?:DISTINCT\\s+)?[^\\s]+)\\s+([^\\s]+.*)$",
            Pattern.CASE_INSENSITIVE);

    private Object paramsArrayOrMap;
    private String query;
    private String countQuery;
    private String orderBy;
    private EntityManager em;

    private Page page;
    private Long count;

    private Range range;

    private LockModeType lockModeType;
    private Map<String, Object> hints = new HashMap<>();

    PanacheQueryImpl(EntityManager em, String query, String orderBy, Object paramsArrayOrMap) {
        this.em = em;
        this.query = query;
        this.orderBy = orderBy;
        this.paramsArrayOrMap = paramsArrayOrMap;
    }

    private PanacheQueryImpl(PanacheQueryImpl previousQuery, String newQueryString, String countQuery) {
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
    }

    // Builder

    @NotNull
    @Override
    public <Entity> PanacheQuery<Entity> project(Class<Entity> type) {
        if (JpaOperations.isNamedQuery(query)) {
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

        return new PanacheQueryImpl<>(this, select.toString() + query, "select count(*) " + query);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public PanacheQuery<Entity> page(@NotNull Page page) {
        this.page = page;
        this.range = null; // reset the range to be able to switch from range to page
        return (PanacheQuery<Entity>) this;
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> nextPage() {
        checkPagination();
        return page(page.next());
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> previousPage() {
        checkPagination();
        return page(page.previous());
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> firstPage() {
        checkPagination();
        return page(page.first());
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> lastPage() {
        checkPagination();
        return page(page.index(pageCount() - 1));
    }

    @Override
    public boolean hasNextPage() {
        checkPagination();
        return page.index < (pageCount() - 1);
    }

    @Override
    public boolean hasPreviousPage() {
        checkPagination();
        return page.index > 0;
    }

    @Override
    public int pageCount() {
        checkPagination();
        long count = count();
        if (count == 0)
            return 1; // a single page of zero results
        return (int) Math.ceil((double) count / (double) page.size);
    }

    @NotNull
    @Override
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

    @NotNull
    @Override
    public PanacheQuery<Entity> range(int startIndex, int lastIndex) {
        this.range = Range.of(startIndex, lastIndex);
        // reset the page to its default to be able to switch from page to range
        this.page = null;
        return (PanacheQuery<Entity>) this;
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> withLock(@NotNull LockModeType lockModeType) {
        this.lockModeType = lockModeType;
        return (PanacheQuery<Entity>) this;
    }

    @NotNull
    @Override
    public PanacheQuery<Entity> withHint(@NotNull String hintName, @NotNull Object value) {
        hints.put(hintName, value);
        return (PanacheQuery<Entity>) this;
    }

    // Results

    @Override
    @SuppressWarnings("unchecked")
    public long count() {
        if (JpaOperations.isNamedQuery(query)) {
            throw new PanacheQueryException("Unable to perform a count operation on a named query");
        }

        if (count == null) {
            Query countQuery = em.createQuery(countQuery());
            if (paramsArrayOrMap instanceof Map)
                JpaOperations.bindParameters(countQuery, (Map<String, Object>) paramsArrayOrMap);
            else
                JpaOperations.bindParameters(countQuery, (Object[]) paramsArrayOrMap);
            count = (Long) countQuery.getSingleResult();
        }
        return count;
    }

    protected String countQuery() {
        if (countQuery != null) {
            return countQuery;
        }

        // try to generate a good count query from the existing query
        Matcher selectMatcher = SELECT_PATTERN.matcher(query);
        String countQuery;
        if (selectMatcher.matches()) {
            countQuery = "SELECT COUNT(" + selectMatcher.group(1) + ") " + selectMatcher.group(2);
        } else {
            countQuery = "SELECT COUNT(*) " + query;
        }

        // remove the order by clause
        String lcQuery = countQuery.toLowerCase();
        int orderByIndex = lcQuery.lastIndexOf(" order by ");
        if (orderByIndex != -1) {
            countQuery = countQuery.substring(0, orderByIndex);
        }
        return countQuery;
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public List<Entity> list() {
        Query jpaQuery = createQuery();
        return jpaQuery.getResultList();
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Stream<Entity> stream() {
        Query jpaQuery = createQuery();
        return jpaQuery.getResultStream();
    }

    @NotNull
    @Override
    public Entity firstResult() {
        Query jpaQuery = createQuery(1);
        List<Entity> list = jpaQuery.getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @NotNull
    @Override
    public Optional<Entity> firstResultOptional() {
        return Optional.ofNullable(firstResult());
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Entity singleResult() {
        Query jpaQuery = createQuery();
        return (Entity) jpaQuery.getSingleResult();
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public Optional<Entity> singleResultOptional() {
        Query jpaQuery = createQuery(2);
        List<Entity> list = jpaQuery.getResultList();
        if (list.size() > 1) {
            throw new NonUniqueResultException();
        }

        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
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
            RowSelection options = jpaQuery.unwrap(org.hibernate.query.Query.class).getQueryOptions();
            options.setFirstRow(null);
        }
        jpaQuery.setMaxResults(maxResults);

        return jpaQuery;
    }

    private Query createBaseQuery() {
        Query jpaQuery;
        if (JpaOperations.isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            jpaQuery = em.createNamedQuery(namedQuery);
        } else {
            jpaQuery = em.createQuery(orderBy != null ? query + orderBy : query);
        }

        if (paramsArrayOrMap instanceof Map) {
            JpaOperations.bindParameters(jpaQuery, (Map<String, Object>) paramsArrayOrMap);
        } else {
            JpaOperations.bindParameters(jpaQuery, (Object[]) paramsArrayOrMap);
        }

        if (this.lockModeType != null) {
            jpaQuery.setLockMode(lockModeType);
        }

        for (Map.Entry<String, Object> hint : hints.entrySet()) {
            jpaQuery.setHint(hint.getKey(), hint.getValue());
        }
        return jpaQuery;
    }

}
