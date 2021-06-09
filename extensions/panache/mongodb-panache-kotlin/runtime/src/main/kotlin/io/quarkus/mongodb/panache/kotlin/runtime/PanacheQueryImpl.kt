package io.quarkus.mongodb.panache.kotlin.runtime

import com.mongodb.ReadPreference
import com.mongodb.client.ClientSession
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Collation
import io.quarkus.mongodb.panache.kotlin.PanacheQuery
import io.quarkus.mongodb.panache.runtime.CommonPanacheQueryImpl
import io.quarkus.panache.common.Page
import org.bson.conversions.Bson
import java.util.stream.Stream

class PanacheQueryImpl<Entity: Any> : PanacheQuery<Entity> {
    private val delegate: CommonPanacheQueryImpl<Entity>

    internal constructor(collection: MongoCollection<out Entity>?, session: ClientSession?, mongoQuery: Bson?, sort: Bson?) {
        delegate = CommonPanacheQueryImpl(collection, session, mongoQuery, sort)
    }

    private constructor(delegate: CommonPanacheQueryImpl<Entity>) {
        this.delegate = delegate
    }

    override fun <T: Any> project(type: Class<T>): PanacheQuery<T> {
        return PanacheQueryImpl(delegate.project(type))
    }

    override fun page(page: Page): PanacheQuery<Entity> {
        delegate.page<Entity>(page)
        return this
    }

    override fun page(pageIndex: Int, pageSize: Int): PanacheQuery<Entity> {
        return page(Page.of(pageIndex, pageSize))
    }

    override fun nextPage(): PanacheQuery<Entity> {
        delegate.nextPage<Entity>()
        return this
    }

    override fun previousPage(): PanacheQuery<Entity> {
        delegate.previousPage<Entity>()
        return this
    }

    override fun firstPage(): PanacheQuery<Entity> {
        delegate.firstPage<Entity>()
        return this
    }

    override fun lastPage(): PanacheQuery<Entity> {
        delegate.lastPage<Entity>()
        return this
    }

    override fun hasNextPage(): Boolean {
        return delegate.hasNextPage()
    }

    override fun hasPreviousPage(): Boolean {
        return delegate.hasPreviousPage()
    }

    override fun pageCount(): Int {
        return delegate.pageCount()
    }

    override fun page(): Page {
        return delegate.page()
    }

    override fun range(startIndex: Int, lastIndex: Int): PanacheQuery<Entity> {
        delegate.range<Entity>(startIndex, lastIndex)
        return this
    }

    override fun withCollation(collation: Collation): PanacheQuery<Entity> {
        delegate.withCollation<Entity>(collation)
        return this
    }

    override fun withReadPreference(readPreference: ReadPreference?): PanacheQuery<Entity> {
        delegate.withReadPreference<Entity>(readPreference)
        return this
    }

    // Results
    override fun count(): Long {
        return delegate.count()
    }

    override fun list(): List<Entity> {
        return delegate.list()
    }

    override fun stream(): Stream<Entity> {
        return delegate.stream()
    }

    override fun firstResult(): Entity? {
        return delegate.firstResult()
    }

    override fun singleResult(): Entity? {
        return delegate.singleResult()
    }
}
