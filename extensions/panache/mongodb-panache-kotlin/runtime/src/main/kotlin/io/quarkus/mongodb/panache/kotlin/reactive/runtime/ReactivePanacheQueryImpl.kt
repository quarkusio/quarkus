package io.quarkus.mongodb.panache.kotlin.reactive.runtime

import io.quarkus.mongodb.panache.common.reactive.runtime.CommonReactivePanacheQueryImpl
import io.quarkus.mongodb.reactive.ReactiveMongoCollection
import org.bson.conversions.Bson
import io.smallrye.mutiny.Uni
import com.mongodb.ReadPreference
import com.mongodb.client.model.Collation
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheQuery
import io.quarkus.panache.common.Page
import io.smallrye.mutiny.Multi

class ReactivePanacheQueryImpl<Entity> : ReactivePanacheQuery<Entity> {
    private val delegate: CommonReactivePanacheQueryImpl<Entity>

    internal constructor(collection: ReactiveMongoCollection<out Entity>?, mongoQuery: Bson?, sort: Bson?) {
        delegate = CommonReactivePanacheQueryImpl(collection, mongoQuery, sort)
    }

    private constructor(delegate: CommonReactivePanacheQueryImpl<Entity>) {
        this.delegate = delegate
    }

    override fun <T> project(type: Class<T>): ReactivePanacheQuery<T> {
        return ReactivePanacheQueryImpl(delegate.project(type))
    }

    override fun page(page: Page): ReactivePanacheQuery<Entity> {
        delegate.page<Entity>(page)
        return this
    }

    override fun page(pageIndex: Int, pageSize: Int): ReactivePanacheQuery<Entity> {
        return page(Page.of(pageIndex, pageSize))
    }

    override fun nextPage(): ReactivePanacheQuery<Entity> {
        delegate.nextPage<Entity>()
        return this
    }

    override fun previousPage(): ReactivePanacheQuery<Entity> {
        delegate.previousPage<Entity>()
        return this
    }

    override fun firstPage(): ReactivePanacheQuery<Entity> {
        delegate.firstPage<Entity>()
        return this
    }

    override fun lastPage(): Uni<ReactivePanacheQuery<Entity>> {
        val uni: Uni<CommonReactivePanacheQueryImpl<Entity>> = delegate.lastPage()
        return uni.map { this }
    }

    override fun hasNextPage(): Uni<Boolean> {
        return delegate.hasNextPage()
    }

    override fun hasPreviousPage(): Boolean {
        return delegate.hasPreviousPage()
    }

    override fun pageCount(): Uni<Int> {
        return delegate.pageCount()
    }

    override fun page(): Page {
        return delegate.page()
    }

    override fun range(startIndex: Int, lastIndex: Int): ReactivePanacheQuery<Entity> {
        delegate.range<Entity>(startIndex, lastIndex)
        return this
    }

    override fun withCollation(collation: Collation): ReactivePanacheQuery<Entity> {
        delegate.withCollation<Entity>(collation)
        return this
    }

    override fun withReadPreference(readPreference: ReadPreference): ReactivePanacheQuery<Entity> {
        delegate.withReadPreference<Entity>(readPreference)
        return this
    }

    override fun count(): Uni<Long> {
        return delegate.count()
    }

    override fun list(): Uni<List<Entity>> {
        return delegate.list()
    }

    override fun stream(): Multi<Entity> {
        return delegate.stream()
    }

    override fun firstResult(): Uni<Entity?> {
        return delegate.firstResult()
    }

    override fun singleResult(): Uni<Entity?> {
        return delegate.singleResult()
    }
}
