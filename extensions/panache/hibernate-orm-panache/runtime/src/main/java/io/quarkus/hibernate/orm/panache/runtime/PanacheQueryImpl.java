package io.quarkus.hibernate.orm.panache.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;

public class PanacheQueryImpl<Entity> implements PanacheQuery<Entity> {

    private Query jpaQuery;
    private Object paramsArrayOrMap;
    private String query;
    private EntityManager em;

    /*
     * We store the pageSize and apply it for each request because getFirstResult()
     * sets the page size to 1
     */
    private Page page;
    private Long count;

    PanacheQueryImpl(EntityManager em, javax.persistence.Query jpaQuery, String query, Object paramsArrayOrMap) {
        this.em = em;
        this.jpaQuery = jpaQuery;
        this.query = query;
        this.paramsArrayOrMap = paramsArrayOrMap;
        page = new Page(0, Integer.MAX_VALUE);
    }

    // Builder

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> PanacheQuery<T> page(Page page) {
        this.page = page;
        jpaQuery.setFirstResult(page.index * page.size);
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    @Override
    public <T extends Entity> PanacheQuery<T> nextPage() {
        return page(page.next());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> previousPage() {
        return page(page.previous());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> firstPage() {
        return page(page.first());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> lastPage() {
        return page(page.index(pageCount() - 1));
    }

    @Override
    public boolean hasNextPage() {
        return page.index < (pageCount() - 1);
    }

    @Override
    public boolean hasPreviousPage() {
        return page.index > 0;
    }

    @Override
    public int pageCount() {
        long count = count();
        if (count == 0)
            return 1; // a single page of zero results
        return (int) Math.ceil((double) count / (double) page.size);
    }

    @Override
    public Page page() {
        return page;
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
        return "SELECT COUNT(*) " + query;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> list() {
        jpaQuery.setMaxResults(page.size);
        return jpaQuery.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream() {
        jpaQuery.setMaxResults(page.size);
        return jpaQuery.getResultStream();
    }

    @Override
    public <T extends Entity> T firstResult() {
        jpaQuery.setMaxResults(1);
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
        jpaQuery.setMaxResults(page.size);
        return (T) jpaQuery.getSingleResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Optional<T> singleResultOptional() {
        jpaQuery.setMaxResults(2);
        List<T> list = jpaQuery.getResultList();
        if (list.size() == 2) {
            throw new NonUniqueResultException();
        }

        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
