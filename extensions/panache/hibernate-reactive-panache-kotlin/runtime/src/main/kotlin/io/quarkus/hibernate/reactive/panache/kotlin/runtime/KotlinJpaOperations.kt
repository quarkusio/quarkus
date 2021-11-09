package io.quarkus.hibernate.reactive.panache.kotlin.runtime;

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import org.hibernate.reactive.mutiny.Mutiny

class KotlinJpaOperations : AbstractJpaOperations<PanacheQueryImpl<*>>() {

    override fun createPanacheQuery(
        session: Uni<Mutiny.Session>,
        query: String,
        orderBy: String,
        paramsArrayOrMap: Any?
    ): PanacheQueryImpl<*> {
        return PanacheQueryImpl<Any>(session, query, orderBy, paramsArrayOrMap)
    }


    override fun stream(query: PanacheQueryImpl<*>): Multi<*> =
        query.stream()


    override fun list(query: PanacheQueryImpl<*>): Uni<List<*>> =
        query.list().map { it.toList() }

    companion object {
        @JvmField
        val INSTANCE = KotlinJpaOperations()
    }
}
