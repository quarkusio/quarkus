package io.quarkus.hibernate.orm.panache.runtime;

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

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Range;
import io.quarkus.panache.common.exception.PanacheQueryException;

public class PanacheQueryImpl<Entity> implements PanacheQuery<Entity> {

    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s+((?:DISTINCT\\s+)?[^\\s]+)\\s+([^\\s]+.*)$",
            Pattern.CASE_INSENSITIVE);

    private Query jpaQuery;
    private Object paramsArrayOrMap;
    private String query;
    private EntityManager em;

    private Page page;
    private Long count;

    private Range range;

    PanacheQueryImpl(EntityManager em, javax.persistence.Query jpaQuery, String query, Object paramsArrayOrMap) {
        this.em = em;
        this.jpaQuery = jpaQuery;
        this.query = query;
        this.paramsArrayOrMap = paramsArrayOrMap;
    }

    // Builder

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> PanacheQuery<T> page(Page page) {
        this.page = page;
        this.range = null; // reset the range to be able to switch from range to page
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    @Override
    public <T extends Entity> PanacheQuery<T> nextPage() {
        checkPagination();
        return page(page.next());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> previousPage() {
        checkPagination();
        return page(page.previous());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> firstPage() {
        checkPagination();
        return page(page.first());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> lastPage() {
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

    @Override
    public <T extends Entity> PanacheQuery<T> range(int startIndex, int lastIndex) {
        this.range = Range.of(startIndex, lastIndex);
        // reset the page to its default to be able to switch from page to range
        this.page = null;
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> withLock(LockModeType lockModeType) {
        jpaQuery.setLockMode(lockModeType);
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> withHint(String hintName, Object value) {
        jpaQuery.setHint(hintName, value);
        return (PanacheQuery<T>) this;
    }

    // Results

    @Override
    @SuppressWarnings("unchecked")
    public long count() {
        if (JpaOperations.isNamedQuery(query)) {
            throw new PanacheQueryException("Unable to perform a count operation on a named query");
        }

        if (count == null) {
            // FIXME: this is crude but good enough for a first version
            String lcQuery = query.toLowerCase();
            int orderByIndex = lcQuery.lastIndexOf(" order by ");
            if (orderByIndex != -1)
                query = query.substring(0, orderByIndex);
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
        Matcher selectMatcher = SELECT_PATTERN.matcher(query);
        if (selectMatcher.matches()) {
            return "SELECT COUNT(" + selectMatcher.group(1) + ") " + selectMatcher.group(2);
        }
        return "SELECT COUNT(*) " + query;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> list() {
        manageOffsets();
        return jpaQuery.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream() {
        manageOffsets();
        return jpaQuery.getResultStream();
    }

    @Override
    public <T extends Entity> T firstResult() {
        manageOffsets(1);
        List<T> list = jpaQuery.getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public <T extends Entity> Optional<T> firstResultOptional() {
        return Optional.ofNullable(firstResult());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> T singleResult() {
        manageOffsets();
        return (T) jpaQuery.getSingleResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Optional<T> singleResultOptional() {
        manageOffsets(2);
        List<T> list = jpaQuery.getResultList();
        if (list.size() > 1) {
            throw new NonUniqueResultException();
        }

        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private void manageOffsets() {
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
    }

    private void manageOffsets(int maxResults) {
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
    }
}
