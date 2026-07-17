package io.quarkus.hibernate.panache;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.PageRequest;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;

import org.hibernate.Session;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.query.Page;

import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;

/**
 * <p>
 * Interface representing an entity query, which abstracts the use of paging, getting the number of results, and
 * operating on {@link List} or {@link Stream}.
 * </p>
 * <p>
 * Instances of this interface cannot mutate the query itself or its parameters: only paging and sort
 * information can be modified, and instances of this interface can be reused to obtain multiple pages of results.
 * </p>
 *
 * @author Stéphane Épardaud
 * @param <Entity> The result type returned by single-result operations
 * @param <SortEntity> The entity type used for typed sorting
 */
public interface PanacheQuery<Query extends PanacheQuery<Query, Entity, SortEntity, EntityList, QueryAfterCount, Confirmation, Count>, Entity, SortEntity, EntityList, QueryAfterCount, Confirmation, Count> {

    /**
     * Provides operations for limiting the number of results returned by a query using absolute row indices.
     * <p>
     * Limiting and paging are mutually exclusive: setting a limit clears any current page, and vice versa.
     * Use {@link Pages} if you need page-based navigation instead.
     *
     * @param <Query> the query type, so that fluent methods return the correct query type
     */
    public interface Limits<Query extends PanacheQuery<?, ?, ?, ?, ?, ?, ?>> {

        /**
         * Returns the current limit as a Jakarta Data {@link Limit}.
         *
         * @return the current limit
         * @throws UnsupportedOperationException if no limit or range has been set
         */
        // FIXME: do we group these appart in a Ranging interface?
        Limit limit();

        /**
         * Sets the current limit from a Jakarta Data {@link Limit}.
         *
         * @param limit the limit to apply
         * @return this query, modified
         */
        Query limit(Limit limit);

        /**
         * Limits the query to return at most {@code max} results, starting from the first result (index 0).
         *
         * @param max the maximum number of results to return
         * @return this query, modified
         * @throws IllegalArgumentException if {@code max} is 0
         */
        // FIXME: do we add these shortcuts or rely on Limit?
        Query limit(int max);

        /**
         * Limits the query to return at most {@code max} results, starting from the given row index.
         *
         * @param start the 0-based index of the first result to return
         * @param max the maximum number of results to return
         * @return this query, modified
         */
        Query limit(long start, int max);

        /**
         * Limits the query to start at the given row index, using a default maximum number of results.
         *
         * @param start the 0-based index of the first result to return
         * @return this query, modified
         */
        Query limitFrom(long start);

        /**
         * Sets a fixed range of results to return, using inclusive start and end row indices.
         * Unlike paging, a range is fixed and does not support subsequent navigation.
         *
         * @param start the 0-based index of the first result to return (inclusive)
         * @param end the 0-based index of the last result to return (inclusive)
         * @return this query, modified
         */
        Query range(long start, long end);
    }

    /**
     * Provides operations for page-based navigation over query results.
     * <p>
     * Paging supports both offset-based and cursor-based pagination modes.
     * Paging and limiting are mutually exclusive: setting a page clears any current limit, and vice versa.
     * <p>
     * A page must be set (via {@link #request(PageRequest)}, {@link #page(long, int)}, or
     * {@link #cursor(long, int)}) before calling navigation or inspection methods.
     *
     * @param <Query> the query type, so that fluent methods return the correct query type
     * @param <QueryAfterCount> the return type for operations that require a count query (may be async)
     * @param <Confirmation> the return type for boolean checks (may be async)
     * @param <Count> the return type for count values (may be async)
     */
    public interface Pages<Query extends PanacheQuery<?, ?, ?, ?, ?, ?, ?>, QueryAfterCount, Confirmation, Count> {

        /**
         * Returns the current page as a Jakarta Data {@link PageRequest}.
         *
         * @return the current page request
         * @throws UnsupportedOperationException if no page has been set
         */
        // FIXME: or page()?
        PageRequest request();

        /**
         * Sets the current page from a Jakarta Data {@link PageRequest}.
         *
         * @param request the page request to apply
         * @return this query, modified
         */
        Query request(PageRequest request);

        /**
         * Sets the current page using offset-based pagination.
         *
         * @param pageIndex the 0-based page index
         * @param pageSize the number of results per page
         * @return this query, modified
         */
        // FIXME: same question about shortcut methods
        Query page(long pageIndex, int pageSize);

        /**
         * Sets the current page using cursor-based pagination (keyset pagination).
         * The sort criteria from the original query (via {@code find()} or {@code findAll()})
         * are used to define the keyset columns.
         *
         * @param pageIndex the 0-based page index
         * @param pageSize the number of results per page
         * @return this query, modified
         * @throws UnsupportedOperationException if no sort criteria were provided when creating the query
         */
        Query cursor(long pageIndex, int pageSize);

        /**
         * Moves to the next page.
         *
         * @return this query, modified
         * @throws UnsupportedOperationException if no page has been set
         */
        // FIXME: harmonize with PageRequest?
        Query next();

        /**
         * Moves to the previous page, or stays on the first page if already there.
         *
         * @return this query, modified
         * @throws UnsupportedOperationException if no page has been set
         */
        Query previous();

        /**
         * Moves to the first page.
         *
         * @return this query, modified
         * @throws UnsupportedOperationException if no page has been set
         */
        Query first();

        /**
         * Moves to the last page. This requires reading the entity count.
         *
         * @return this query, modified (may be async if the query is reactive)
         * @throws UnsupportedOperationException if no page has been set
         * @see #count()
         */
        QueryAfterCount last();

        /**
         * Returns true if there is another page to read after the current one.
         * This requires reading the entity count.
         *
         * @return true if there is a next page (may be async if the query is reactive)
         * @throws UnsupportedOperationException if no page has been set
         * @see #hasPrevious()
         * @see #count()
         */
        Confirmation hasNext();

        /**
         * Returns true if there is a page before the current one.
         *
         * @return true if there is a previous page (may be async if the query is reactive)
         * @throws UnsupportedOperationException if no page has been set
         * @see #hasNext()
         */
        Confirmation hasPrevious();

        /**
         * Returns the total number of pages, based on the current page size.
         * This requires reading the entity count.
         *
         * @return the total number of pages (may be async if the query is reactive)
         * @throws UnsupportedOperationException if no page has been set
         */
        Count count();
    }

    /**
     * Returns the {@link Limits} interface for this query, which provides operations to
     * restrict results using absolute row indices and ranges.
     *
     * @return the limiting interface for this query
     */
    Limits<Query> limits();

    /**
     * Returns the {@link Pages} interface for this query, which provides operations for
     * page-based navigation over query results.
     *
     * @return the paging interface for this query
     */
    Pages<Query, QueryAfterCount, Confirmation, Count> pages();

    // Builder

    /**
     * Applies sort criteria to this query.
     *
     * @param order the sort order to use
     * @return this query, modified
     */
    public Query sort(Order<? super SortEntity> order);

    /**
     * Applies sort criteria to this query.
     *
     * @param sort the sort strategy to use
     * @return this query, modified
     */
    default Query sort(Sort<? super SortEntity> sort) {
        return sort(PanacheJpaUtil.toOrder(sort));
    }

    /**
     * Define the locking strategy used for this query.
     *
     * @param lockModeType the locking strategy to be used for this query.
     * @return this query, modified
     */
    public Query withLock(LockModeType lockModeType);

    /**
     * Set a query property or hint on the underlying JPA Query.
     *
     * @param hintName name of the property or hint.
     * @param value value for the property or hint.
     * @return this query, modified
     */
    public Query withHint(String hintName, Object value);

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
    public Query filter(String filterName,
            Map<String, Object> parameters);

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
    public Query filter(String filterName);

    // Results

    /**
     * Reads and caches the total number of entities this query operates on. This causes a database
     * query with <code>SELECT COUNT(*)</code> and a query equivalent to the current query, minus
     * ordering.
     *
     * @return the total number of entities this query operates on, cached.
     */
    public Count count();

    /**
     * Returns the current page of results as a {@link List}.
     *
     * @return the current page of results as a {@link List}.
     * @see #stream()
     * @see #page(Page)
     * @see #page()
     */
    public EntityList list();

    /**
     * Returns the current page of results as a {@link Stream}.
     *
     * @return the current page of results as a {@link Stream}.
     * @see #list()
     * @see #page(Page)
     * @see #page()
     */
    // FIXME: down
    //    public Stream<Entity> stream();

    /**
     * Returns the first result of the current page index. This ignores the current page size to fetch
     * a single result.
     *
     * @return the first result of the current page index, or null if there are no results.
     * @see #singleResult()
     */
    public Entity firstResult();

    /**
     * Returns the first result of the current page index. This ignores the current page size to fetch
     * a single result.
     *
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     * @see #singleResultOptional()
     */
    // FIXME: down
    //    public Optional<Entity> firstResultOptional();

    /**
     * Executes this query for the current page and return a single result.
     *
     * @return the single result (throws if there is not exactly one)
     * @throws NoResultException if there is no result
     * @throws NonUniqueResultException if there are more than one result
     * @see #firstResult()
     */
    public Entity singleResult();

    /**
     * Executes this query for the current page and return a single result.
     *
     * @return if found, an optional containing the entity, else <code>Optional.empty()</code>.
     * @throws NonUniqueResultException if there are more than one result
     * @see #firstResultOptional()
     */
    // FIXME: down
    //    public Optional<Entity> singleResultOptional();
}
