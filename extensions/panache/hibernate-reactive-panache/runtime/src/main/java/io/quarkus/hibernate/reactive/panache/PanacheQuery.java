package io.quarkus.hibernate.reactive.panache;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;

import org.hibernate.Session;
import org.hibernate.annotations.FilterDef;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

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
     * @param pageIndex the page index (0-based)
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
    @CheckReturnValue
    public <T extends Entity> Uni<PanacheQuery<T>> lastPage();

    /**
     * Returns true if there is another page to read after the current one.
     * This will cause reading of the entity count.
     *
     * @return true if there is another page to read
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see #hasPreviousPage()
     * @see #count()
     */
    @CheckReturnValue
    public Uni<Boolean> hasNextPage();

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
    @CheckReturnValue
    public Uni<Integer> pageCount();

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

    /**
     * <p>
     * Enables a Hibernate filter during fetching of results for this query. Your filter must be declared
     * with {@link FilterDef} on your entity or package, and enabled with {@link Filter} on your entity.
     * <p>
     * WARNING: setting filters can only be done on the underlying Hibernate {@link Session} and so this
     * will modify the session's filters for the duration of obtaining the results (not while building
     * the query). Enabled filters will be removed from the session afterwards, but no effort is made to
     * preserve filters enabled on the session outside of this API.
     *
     * @param filterName The name of the filter to enable
     * @param parameters The set of parameters for the filter, if the filter requires parameters
     * @return this query, modified
     */
    public <T extends Entity> PanacheQuery<T> filter(String filterName, Parameters parameters);

    /**
     * <p>
     * Enables a Hibernate filter during fetching of results for this query. Your filter must be declared
     * with {@link FilterDef} on your entity or package, and enabled with {@link Filter} on your entity.
     * <p>
     * WARNING: setting filters can only be done on the underlying Hibernate {@link Session} and so this
     * will modify the session's filters for the duration of obtaining the results (not while building
     * the query). Enabled filters will be removed from the session afterwards, but no effort is made to
     * preserve filters enabled on the session outside of this API.
     *
     * @param filterName The name of the filter to enable
     * @param parameters The set of parameters for the filter, if the filter requires parameters
     * @return this query, modified
     */
    public <T extends Entity> PanacheQuery<T> filter(String filterName, Map<String, Object> parameters);

    /**
     * <p>
     * Enables a Hibernate filter during fetching of results for this query. Your filter must be declared
     * with {@link FilterDef} on your entity or package, and enabled with {@link Filter} on your entity.
     * <p>
     * WARNING: setting filters can only be done on the underlying Hibernate {@link Session} and so this
     * will modify the session's filters for the duration of obtaining the results (not while building
     * the query). Enabled filters will be removed from the session afterwards, but no effort is made to
     * preserve filters enabled on the session outside of this API.
     *
     * @param filterName The name of the filter to enable
     * @return this query, modified
     */
    public <T extends Entity> PanacheQuery<T> filter(String filterName);

    // Results

    /**
     * Reads and caches the total number of entities this query operates on. This causes a database
     * query with <code>SELECT COUNT(*)</code> and a query equivalent to the current query, minus
     * ordering.
     *
     * @return the total number of entities this query operates on, cached.
     */
    @CheckReturnValue
    public Uni<Long> count();

    /**
     * Returns the current page of results as a {@link List}.
     *
     * @return the current page of results as a {@link List}.
     * @see #stream()
     * @see #page(Page)
     * @see #page()
     */
    @CheckReturnValue
    public <T extends Entity> Uni<List<T>> list();

    /**
     * Returns the current page of results as a {@link Stream}.
     *
     * @return the current page of results as a {@link Stream}.
     * @see #list()
     * @see #page(Page)
     * @see #page()
     */
    @CheckReturnValue
    public <T extends Entity> Multi<T> stream();

    /**
     * Returns the first result of the current page index. This ignores the current page size to fetch
     * a single result.
     *
     * @return the first result of the current page index, or null if there are no results.
     * @see #singleResult()
     */
    @CheckReturnValue
    public <T extends Entity> Uni<T> firstResult();

    /**
     * Executes this query for the current page and return a single result.
     *
     * @return the single result (throws if there is not exactly one)
     * @throws NoResultException if there is no result
     * @throws NonUniqueResultException if there are more than one result
     * @see #firstResult()
     */
    @CheckReturnValue
    public <T extends Entity> Uni<T> singleResult();
}
