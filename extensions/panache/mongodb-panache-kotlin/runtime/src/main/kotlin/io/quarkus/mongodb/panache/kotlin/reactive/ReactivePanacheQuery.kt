package io.quarkus.mongodb.panache.kotlin.reactive

import com.mongodb.client.model.Collation
import io.quarkus.panache.common.Page
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni

/**
 * Interface representing an entity query, which abstracts the use of paging, getting the number of results, and
 * operating on [List] or [Stream].
 *
 * Instances of this interface cannot mutate the query itself or its parameters: only paging information can be
 * modified, and instances of this interface can be reused to obtain multiple pages of results.
 *
 * @param Entity The entity type being queried
 */
interface ReactivePanacheQuery<Entity> {
    /**
     * Defines a projection class: the getters, and the public fields, will be used to restrict which fields should be
     * retrieved from the database.
     *
     * @return @return a new query with the same state as the previous one (params, page, range, ...).
     */
    fun <NewEntity> project(type: Class<NewEntity>): ReactivePanacheQuery<NewEntity>

    /**
     * Sets the current page.
     *
     * @param page the new page
     * @return this query, modified
     * @see [page]
     */
    fun page(page: Page): ReactivePanacheQuery<Entity>

    /**
     * Sets the current page.
     *
     * @param pageIndex the page index
     * @param pageSize the page size
     * @return this query, modified
     * @see [page]
     */
    fun page(pageIndex: Int, pageSize: Int): ReactivePanacheQuery<Entity>

    /**
     * Sets the current page to the next page
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [previousPage]
     */
    fun nextPage(): ReactivePanacheQuery<Entity>

    /**
     * Sets the current page to the previous page (or the first page if there is no previous page)
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [nextPage]
     */
    fun previousPage(): ReactivePanacheQuery<Entity>

    /**
     * Sets the current page to the first page
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [lastPage]
     */
    fun firstPage(): ReactivePanacheQuery<Entity>

    /**
     * Sets the current page to the last page. This will cause reading of the entity count.
     *
     * @return this query, modified
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [firstPage]
     * @see [count]
     */
    fun lastPage(): Uni<ReactivePanacheQuery<Entity>>

    /**
     * Returns true if there is another page to read after the current one.
     * This will cause reading of the entity count.
     *
     * @return true if there is another page to read
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [hasPreviousPage]
     * @see [count]
     */
    fun hasNextPage(): Uni<Boolean>

    /**
     * Returns true if there is a page to read before the current one.
     *
     * @return true if there is a previous page to read
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [hasNextPage]
     */
    fun hasPreviousPage(): Boolean

    /**
     * Returns the total number of pages to be read using the current page size.
     * This will cause reading of the entity count.
     *
     * @return the total number of pages to be read using the current page size.
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     */
    fun pageCount(): Uni<Int>

    /**
     * Returns the current page.
     *
     * @return the current page
     * @throws UnsupportedOperationException if a page hasn't been set or if a range is already set
     * @see [page]
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
    fun range(startIndex: Int, lastIndex: Int): ReactivePanacheQuery<Entity>

    /**
     * Define the collation used for this query.
     *
     * @param collation the collation to be used for this query.
     * @return this query, modified
     */
    fun withCollation(collation: Collation): ReactivePanacheQuery<Entity>

    /**
     * Reads and caches the total number of entities this query operates on. This causes a database
     * query with `SELECT COUNT(*)` and a query equivalent to the current query, minus
     * ordering.
     *
     * @return the total number of entities this query operates on, cached.
     */
    fun count(): Uni<Long>

    /**
     * Returns the current page of results as a [List].
     *
     * @return the current page of results as a [List].
     * @see [page]
     */
    fun list(): Uni<List<Entity>>

    /**
     * Returns the current page of results as a [Stream].
     *
     * @return the current page of results as a [Stream].
     * @see [list]
     * @see [page]
     */
    fun stream(): Multi<Entity>

    /**
     * Returns the first result of the current page index. This ignores the current page size to fetch
     * a single result.
     *
     * @return the first result of the current page index, or null if there are no results.
     * @see [singleResult]
     */
    fun firstResult(): Uni<Entity?>

    /**
     * Executes this query for the current page and return a single result.
     *
     * @return the single result.
     * @throws io.quarkus.panache.common.exception.PanacheQueryException if there are more than one result.
     * @see [firstResult]
     */
    fun singleResult(): Uni<Entity?>
}