package io.quarkus.mongodb.panache.kotlin

import com.mongodb.client.model.Collation
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.exception.PanacheQueryException
import java.util.stream.Stream

/**
 * Interface representing an entity query, which abstracts the use of paging, getting the number of results, and
 * operating on [List] or [Stream].
 *
 * Instances of this interface cannot mutate the query itself or its parameters: only paging information can be
 * modified, and instances of this interface can be reused to obtain multiple pages of results.
 *
 * @param Entity The entity type being queried
 */
interface PanacheQuery<Entity: Any> {
    /**
     * Defines a projection class: the getters, and the public fields, will be used to restrict which fields should be
     * retrieved from the database.
     *
     * @return a new query with the same state as the previous one (params, page, range, ...).
     */
    fun <NewEntity: Any> project(type: Class<NewEntity>): PanacheQuery<NewEntity>

    /**
     * Sets the current page.
     *
     * @param page the new page
     * @return this query, modified
     * @see .page
     * @see .page
     */
    fun page(page: Page): PanacheQuery<Entity>

    /**
     * Sets the current page.
     *
     * @param pageIndex the page index
     * @param pageSize the page size
     * @return this query, modified
     * @see .page
     * @see .page
     */
    fun page(pageIndex: Int, pageSize: Int): PanacheQuery<Entity>

    /**
     * Sets the current page to the next page
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see .previousPage
     */
    fun nextPage(): PanacheQuery<Entity>

    /**
     * Sets the current page to the previous page (or the first page if there is no previous page)
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see .nextPage
     */
    fun previousPage(): PanacheQuery<Entity>

    /**
     * Sets the current page to the first page
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see .lastPage
     */
    fun firstPage(): PanacheQuery<Entity>

    /**
     * Sets the current page to the last page. This will cause reading of the entity count.
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see .firstPage
     * @see .count
     */
    fun lastPage(): PanacheQuery<Entity>

    /**
     * Returns true if there is another page to read after the current one.
     * This will cause reading of the entity count.
     *
     * @return true if there is another page to read
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see .hasPreviousPage
     * @see .count
     */
    fun hasNextPage(): Boolean

    /**
     * Returns true if there is a page to read before the current one.
     *
     * @return true if there is a previous page to read
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see .hasNextPage
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
     * @see .page
     * @see .page
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
     * Define the collation used for this query.
     *
     * @param collation the collation to be used for this query.
     * @return this query, modified
     */
    fun withCollation(collation: Collation): PanacheQuery<Entity>
    // Results
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
     * @see .stream
     * @see .page
     * @see .page
     */
    fun list(): List<Entity>

    /**
     * Returns the current page of results as a [Stream].
     *
     * @return the current page of results as a [Stream].
     * @see .list
     * @see .page
     * @see .page
     */
    fun stream(): Stream<Entity>

    /**
     * Returns the first result of the current page index. This ignores the current page size to fetch
     * a single result.
     *
     * @return the first result of the current page index, or null if there are no results.
     * @see .singleResult
     */
    fun firstResult(): Entity?

    /**
     * Executes this query for the current page and return a single result.
     *
     * @return the single result
     * @throws PanacheQueryException if there is not exactly one result.
     * @see .firstResult
     */
    fun singleResult(): Entity?
}