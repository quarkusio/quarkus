package io.quarkus.hibernate.orm.panache;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import io.quarkus.panache.common.Page;

/**
 * <p>
 * Interface representing an entity query, which abstracts the use of paging, getting the number of results, and
 * operating on {@link List} or {@link Stream}.
 * </p>
 * <p>
 * Instances of this interface cannot mutate the query itself or its parameters: only paging information can be
 * modified, and instances of this interface can be reused to obtain multiple pages of results.
 * </p>
 *
 * @author Stéphane Épardaud
 * @param <Entity> The entity type being queried
 */
public interface PanacheQuery<Entity> {

    // Builder

    /**
     * Defines a projection class: the getters, and the public fields, will be used to restrict which fields should be
     * retrieved from the database.
     *
     * @return a new query with the same state as the previous one (params, page, range, lockMode, hints, ...).
     */
    public <T> PanacheQuery<T> project(Class<T> type);

    /**
     * Sets the current page.
     * 
     * @param page the new page
     * @return this query, modified
     * @see #page(int, int)
     * @see #page()
     */
    public <T extends Entity> PanacheQuery<T> page(Page page);

    /**
     * Sets the current page.
     * 
     * @param pageIndex the page index
     * @param pageSize the page size
     * @return this query, modified
     * @see #page(Page)
     * @see #page()
     */
    public <T extends Entity> PanacheQuery<T> page(int pageIndex, int pageSize);

    /**
     * Sets the current page to the next page
     * 
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see #previousPage()
     */
    public <T extends Entity> PanacheQuery<T> nextPage();

    /**
     * Sets the current page to the previous page (or the first page if there is no previous page)
     * 
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see #nextPage()
     */
    public <T extends Entity> PanacheQuery<T> previousPage();

    /**
     * Sets the current page to the first page
     * 
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see #lastPage()
     */
    public <T extends Entity> PanacheQuery<T> firstPage();

    /**
     * Sets the current page to the last page. This will cause reading of the entity count.
     * 
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see #firstPage()
     * @see #count()
     */
    public <T extends Entity> PanacheQuery<T> lastPage();

    /**
     * Returns true if there is another page to read after the current one.
     * This will cause reading of the entity count.
     * 
     * @return true if there is another page to read
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see #hasPreviousPage()
     * @see #count()
     */
    public boolean hasNextPage();

    /**
     * Returns true if there is a page to read before the current one.
     * 
     * @return true if there is a previous page to read
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see #hasNextPage()
     */
    public boolean hasPreviousPage();

    /**
     * Returns the total number of pages to be read using the current page size.
     * This will cause reading of the entity count.
     * 
     * @return the total number of pages to be read using the current page size.
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     */
    public int pageCount();

    /**
     * Returns the current page.
     * 
     * @return the current page
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see #page(Page)
     * @see #page(int,int)
     */
    public Page page();

    /**
     * Switch the query to use a fixed range (start index - last index) instead of a page.
     * As the range is fixed, subsequent pagination of the query is not possible.
     *
     * @param startIndex the index of the first element, starting at 0
     * @param lastIndex the index of the last element
     * @return this query, modified
     */
    public <T extends Entity> PanacheQuery<T> range(int startIndex, int lastIndex);

    /**
     * Define the locking strategy used for this query.
     *
     * @param lockModeType the locking strategy to be used for this query.
     * @return this query, modified
     */
    public <T extends Entity> PanacheQuery<T> withLock(LockModeType lockModeType);

    /**
     * Set a query property or hint on the underlying JPA Query.
     *
     * @param hintName name of the property or hint.
     * @param value value for the property or hint.
     * @return this query, modified
     */
    public <T extends Entity> PanacheQuery<T> withHint(String hintName, Object value);

    // Results

    /**
     * Reads and caches the total number of entities this query operates on. This causes a database
     * query with <code>SELECT COUNT(*)</code> and a query equivalent to the current query, minus
     * ordering.
     * 
     * @return the total number of entities this query operates on, cached.
     */
    public long count();

    /**
     * Returns the current page of results as a {@link List}.
     * 
     * @return the current page of results as a {@link List}.
     * @see #stream()
     * @see #page(Page)
     * @see #page()
     */
    public <T extends Entity> List<T> list();

    /**
     * Returns the current page of results as a {@link Stream}.
     * 
     * @return the current page of results as a {@link Stream}.
     * @see #list()
     * @see #page(Page)
     * @see #page()
     */
    public <T extends Entity> Stream<T> stream();

    /**
     * Returns the first result of the current page index. This ignores the current page size to fetch
     * a single result.
     * 
     * @return the first result of the current page index, or null if there are no results.
     * @see #singleResult()
     */
    public <T extends Entity> T firstResult();

    /**
     * Returns the first result of the current page index. This ignores the current page size to fetch
     * a single result.
     *
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     * @see #singleResultOptional()
     */
    public <T extends Entity> Optional<T> firstResultOptional();

    /**
     * Executes this query for the current page and return a single result.
     * 
     * @return the single result (throws if there is not exactly one)
     * @throws NoResultException if there is no result
     * @throws NonUniqueResultException if there are more than one result
     * @see #firstResult()
     */
    public <T extends Entity> T singleResult();

    /**
     * Executes this query for the current page and return a single result.
     *
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     * @throws NonUniqueResultException if there are more than one result
     * @see #firstResultOptional()
     */
    public <T extends Entity> Optional<T> singleResultOptional();
}
