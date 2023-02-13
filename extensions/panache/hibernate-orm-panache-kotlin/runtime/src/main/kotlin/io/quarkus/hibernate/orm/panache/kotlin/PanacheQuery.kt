package io.quarkus.hibernate.orm.panache.kotlin

import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Parameters
import jakarta.persistence.LockModeType
import jakarta.persistence.NonUniqueResultException
import org.hibernate.Session
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import java.util.stream.Stream

/**
 * Interface representing an entity query, which abstracts the use of paging, getting the number of results, and
 * operating on [List] or java.util.Stream.
 *
 * Instances of this interface cannot mutate the query itself or its parameters: only paging information can be
 * modified, and instances of this interface can be reused to obtain multiple pages of results.
 *
 * @param Entity The entity type being queried
 */
interface PanacheQuery<Entity : Any> {
    /**
     * Defines a projection class: the getters, and the public fields, will be used to restrict which fields should be
     * retrieved from the database.
     *
     * @return a new query with the same state as the previous one (params, page, range, lockMode, hints, ...).
     */
    fun <NewEntity : Any> project(type: Class<NewEntity>): PanacheQuery<NewEntity>

    /**
     * Sets the current page.
     *
     * @param page the new page
     * @return this query, modified
     * @see [PanacheQuery.page]
     */
    fun page(page: Page): PanacheQuery<Entity>

    /**
     * Sets the current page.
     *
     * @param pageIndex the page index (0-based)
     * @param pageSize the page size
     * @return this query, modified
     * @see [PanacheQuery.page]
     */
    fun page(pageIndex: Int, pageSize: Int): PanacheQuery<Entity>

    /**
     * Sets the current page to the next page
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [PanacheQuery.previousPage]
     */
    fun nextPage(): PanacheQuery<Entity>

    /**
     * Sets the current page to the previous page (or the first page if there is no previous page)
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [PanacheQuery.nextPage]
     */
    fun previousPage(): PanacheQuery<Entity>

    /**
     * Sets the current page to the first page
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [PanacheQuery.lastPage]
     */
    fun firstPage(): PanacheQuery<Entity>

    /**
     * Sets the current page to the last page. This will cause reading of the entity count.
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [PanacheQuery.firstPage]
     * @see [PanacheQuery.count]
     */
    fun lastPage(): PanacheQuery<Entity>

    /**
     * Returns true if there is another page to read after the current one.
     * This will cause reading of the entity count.
     *
     * @return true if there is another page to read
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [PanacheQuery.hasPreviousPage]
     * @see [PanacheQuery.count]
     */
    fun hasNextPage(): Boolean

    /**
     * Returns true if there is a page to read before the current one.
     *
     * @return true if there is a previous page to read
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [PanacheQuery.hasNextPage]
     */
    fun hasPreviousPage(): Boolean

    /**
     * Returns the total number of pages to be read using the current page size.
     * This will cause reading of the entity count.
     *
     * @return the total number of pages to be read using the current page size.
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     */
    fun pageCount(): Int

    /**
     * Returns the current page.
     *
     * @return the current page
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [PanacheQuery.page]
     */
    fun page(): Page

    /**
     * Switch the query to use a fixed range (start index - last index) instead of a page.
     * As the range is fixed, subsequent pagination of the query is not possible.
     *
     * @param startIndex the index of the first element, starting at 0
     * @param lastIndex the index of the last element
     * @return this query, modified
     */
    fun range(startIndex: Int, lastIndex: Int): PanacheQuery<Entity>

    /**
     * Define the locking strategy used for this query.
     *
     * @param lockModeType the locking strategy to be used for this query.
     * @return this query, modified
     */
    fun withLock(lockModeType: LockModeType): PanacheQuery<Entity>

    /**
     * Set a query property or hint on the underlying JPA Query.
     *
     * @param hintName name of the property or hint.
     * @param value value for the property or hint.
     * @return this query, modified
     */
    fun withHint(hintName: String, value: Any): PanacheQuery<Entity>

    /**
     *
     * Enables a Hibernate filter during fetching of results for this query. Your filter must be declared
     * with [FilterDef] on your entity or package, and enabled with [Filter] on your entity.
     *
     * WARNING: setting filters can only be done on the underlying Hibernate [Session] and so this
     * will modify the session's filters for the duration of obtaining the results (not while building
     * the query). Enabled filters will be removed from the session afterwards, but no effort is made to
     * preserve filters enabled on the session outside this API.
     *
     * @param filterName The name of the filter to enable
     * @param parameters The set of parameters for the filter, if the filter requires parameters
     * @return this query, modified
     */
    fun filter(filterName: String, parameters: Parameters): PanacheQuery<Entity>

    /**
     *
     * Enables a Hibernate filter during fetching of results for this query. Your filter must be declared
     * with [FilterDef] on your entity or package, and enabled with [Filter] on your entity.
     *
     * WARNING: setting filters can only be done on the underlying Hibernate [Session] and so this
     * will modify the session's filters for the duration of obtaining the results (not while building
     * the query). Enabled filters will be removed from the session afterwards, but no effort is made to
     * preserve filters enabled on the session outside this API.
     *
     * @param filterName The name of the filter to enable
     * @param parameters The set of parameters for the filter, if the filter requires parameters
     * @return this query, modified
     */
    fun filter(filterName: String, parameters: Map<String, Any>): PanacheQuery<Entity>

    /**
     *
     * Enables a Hibernate filter during fetching of results for this query. Your filter must be declared
     * with [FilterDef] on your entity or package, and enabled with [Filter] on your entity.
     *
     * WARNING: setting filters can only be done on the underlying Hibernate [Session] and so this
     * will modify the session's filters for the duration of obtaining the results (not while building
     * the query). Enabled filters will be removed from the session afterwards, but no effort is made to
     * preserve filters enabled on the session outside this API.
     *
     * @param filterName The name of the filter to enable
     * @return this query, modified
     */
    fun filter(filterName: String): PanacheQuery<Entity>

    /**
     * Reads and caches the total number of entities this query operates on. This causes a database
     * query with `SELECT COUNT(*)` and a query equivalent to the current query, minus
     * ordering.
     *
     * @return the total number of entities this query operates on, cached.
     */
    fun count(): Long

    /**
     * Returns the current page of results as a [List].
     *
     * @return the current page of results as a [List].
     * @see [PanacheQuery.stream]
     * @see [PanacheQuery.page]
     */
    fun list(): List<Entity>

    /**
     * Returns the current page of results as a Stream.
     *
     * @return the current page of results as a Stream.
     * @see [PanacheQuery.list]
     * @see [PanacheQuery.page]
     */
    fun stream(): Stream<Entity>

    /**
     * Returns the first result of the current page index. This ignores the current page size to fetch
     * a single result.
     *
     * @return the first result of the current page index, or null if there are no results.
     * @see [PanacheQuery.singleResult]
     */
    fun firstResult(): Entity?

    /**
     * Executes this query for the current page and return a single result.
     *
     * @return the single result (throws if there is not exactly one)
     * @throws NoResultException if there is no result
     * @throws NonUniqueResultException if there are more than one result
     * @see [PanacheQuery.firstResult]
     */
    fun singleResult(): Entity
}
