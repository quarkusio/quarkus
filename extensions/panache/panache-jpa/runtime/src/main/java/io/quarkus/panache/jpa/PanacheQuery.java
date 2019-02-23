package io.quarkus.panache.jpa;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.jpa.impl.JpaOperations;

public class PanacheQuery<Entity> {

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

    public PanacheQuery(EntityManager em, javax.persistence.Query jpaQuery, String query, Object paramsArrayOrMap) {
        this.em = em;
        this.jpaQuery = jpaQuery;
        this.query = query;
        this.paramsArrayOrMap = paramsArrayOrMap;
        page = new Page(0, Integer.MAX_VALUE);
    }

    // Builder

    @SuppressWarnings("unchecked")
    public <T extends Entity> PanacheQuery<T> page(Page page) {
        this.page = page;
        jpaQuery.setFirstResult(page.index * page.size);
        return (PanacheQuery<T>) this;
    }

    public <T extends Entity> PanacheQuery<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    public <T extends Entity> PanacheQuery<T> nextPage() {
        return page(page.next());
    }

    public <T extends Entity> PanacheQuery<T> previousPage() {
        return page(page.previous());
    }

    public <T extends Entity> PanacheQuery<T> firstPage() {
        return page(page.first());
    }

    public <T extends Entity> PanacheQuery<T> lastPage() {
        return page(page.index(pageCount() - 1));
    }

    public boolean hasNextPage() {
        return page.index < (pageCount() - 1);
    }

    public boolean hasPreviousPage() {
        return page.index > 0;
    }

    public int pageCount() {
        long count = count();
        if (count == 0)
            return 1; // a single page of zero results
        return (int) Math.ceil((double) count / (double) page.size);
    }

    public Page page() {
        return page;
    }

    // Results

    @SuppressWarnings("unchecked")
    public long count() {
        if (count == null) {
            // FIXME: this is crude but good enough for a first version
            String lcQuery = query.toLowerCase();
            int orderByIndex = lcQuery.lastIndexOf(" order by ");
            if (orderByIndex != -1)
                query = query.substring(0, orderByIndex);
            Query countQuery = em.createQuery("SELECT COUNT(*) " + query);
            if (paramsArrayOrMap instanceof Map)
                JpaOperations.bindParameters(countQuery, (Map<String, Object>) paramsArrayOrMap);
            else
                JpaOperations.bindParameters(countQuery, (Object[]) paramsArrayOrMap);
            count = (Long) countQuery.getSingleResult();
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> list() {
        jpaQuery.setMaxResults(page.size);
        return jpaQuery.getResultList();
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream() {
        jpaQuery.setMaxResults(page.size);
        return jpaQuery.getResultStream();
    }

    public <T extends Entity> T firstResult() {
        List<T> list = list();
        jpaQuery.setMaxResults(1);
        return list.isEmpty() ? null : list.get(0);
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> T singleResult() {
        jpaQuery.setMaxResults(page.size);
        return (T) jpaQuery.getSingleResult();
    }
}
