package io.quarkus.panache.jpa;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.jpa.impl.JpaOperations;

/**
 * <p>
 * Class representing an entity query, which abstracts the use of paging, getting the number of results, and
 * operating on {@link List} or {@link Stream}.
 * </p>
 * <p>
 * Instances of this class cannot mutate the query itself or its parameters: only paging information can be
 * modified, and instances of this class can be reused to obtain multiple pages of results.
 * </p>
 *
 * @author Stéphane Épardaud
 * @param <Entity> The entity type being queried
 */
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

    /**
     * Sets the current page.
     * @param page the new page
     * @return this query, modified
     * @see #page(int, int)
     * @see #page()
     */
    @SuppressWarnings("unchecked")
    public <T extends Entity> PanacheQuery<T> page(Page page) {
        this.page = page;
        jpaQuery.setFirstResult(page.index * page.size);
        return (PanacheQuery<T>) this;
    }

    /**
     * Sets the current page.
     * @param pageIndex the page index
     * @param pageSize the page size
     * @return this query, modified
     * @see #page(Page)
     * @see #page()
     */
    public <T extends Entity> PanacheQuery<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    /**
     * Sets the current page to the next page
     * @return this query, modified
     * @see #previousPage()
     */
    public <T extends Entity> PanacheQuery<T> nextPage() {
        return page(page.next());
    }

    /**
     * Sets the current page to the previous page (or the first page if there is no previous page)
     * @return this query, modified
     * @see #nextPage()
     */
    public <T extends Entity> PanacheQuery<T> previousPage() {
        return page(page.previous());
    }

    /**
     * Sets the current page to the first page
     * @return this query, modified
     * @see #lastPage()
     */
    public <T extends Entity> PanacheQuery<T> firstPage() {
        return page(page.first());
    }

    /**
     * Sets the current page to the last page. This will cause reading of the entity count.
     * @return this query, modified
     * @see #firstPage()
     * @see #count()
     */
    public <T extends Entity> PanacheQuery<T> lastPage() {
        return page(page.index(pageCount() - 1));
    }

    /**
     * Returns true if there is another page to read after the current one.
     * This will cause reading of the entity count.
     * @return true if there is another page to read
     * @see #hasPreviousPage()
     * @see #count()
     */
    public boolean hasNextPage() {
        return page.index < (pageCount() - 1);
    }

    /**
     * Returns true if there is a page to read before the current one.
     * @return true if there is a previous page to read
     * @see #hasNextPage()
     */
    public boolean hasPreviousPage() {
        return page.index > 0;
    }

    /**
     * Returns the total number of pages to be read using the current page size.
     * This will cause reading of the entity count.
     * @return the total number of pages to be read using the current page size.
     */
    public int pageCount() {
        long count = count();
        if (count == 0)
            return 1; // a single page of zero results
        return (int) Math.ceil((double) count / (double) page.size);
    }

    /**
     * Returns the current page.
     * @return the current page
     * @see #page(Page)
     * @see #page(int,int)
     */
    public Page page() {
        return page;
    }

    // Results

    /**
     * Reads and caches the total number of entities this query operates on. This causes a database
     * query with <code>SELECT COUNT(*)</code> and a query equivalent to the current query, minus
     * ordering.
     * @return the total number of entities this query operates on, cached.
     */
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

    /**
     * Returns the current page of results as a {@link List}.
     * @return the current page of results as a {@link List}.
     * @see #stream()
     * @see #page(Page)
     * @see #page()
     */
    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> list() {
        jpaQuery.setMaxResults(page.size);
        return jpaQuery.getResultList();
    }

    /**
     * Returns the current page of results as a {@link Stream}.
     * @return the current page of results as a {@link Stream}.
     * @see #list()
     * @see #page(Page)
     * @see #page()
     */
    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream() {
        jpaQuery.setMaxResults(page.size);
        return jpaQuery.getResultStream();
    }

    /**
     * Returns the first result of the current page index. This ignores the current page size to fetch
     * a single result.
     * @return the first result of the current page index, or null if there are no results.
     * @see #singleResult()
     */
    public <T extends Entity> T firstResult() {
        List<T> list = list();
        jpaQuery.setMaxResults(1);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Executes this query for the current page and return a single result.
     * @return the single result (throws if there is not exactly one)
     * @throws NoResultException if there is no result
     * @throws NonUniqueResultException if there are more than one result
     * @see #firstResult()
     */
    @SuppressWarnings("unchecked")
    public <T extends Entity> T singleResult() {
        jpaQuery.setMaxResults(page.size);
        return (T) jpaQuery.getSingleResult();
    }
}
