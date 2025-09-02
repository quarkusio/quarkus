package io.quarkus.hibernate.reactive.panache.kotlin.runtime

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations
import io.smallrye.mutiny.Uni
import org.hibernate.reactive.mutiny.Mutiny

class KotlinJpaOperations : AbstractJpaOperations<PanacheQueryImpl<*>>() {
    override fun createPanacheQuery(
        session: Uni<Mutiny.Session>,
        query: String,
        originalQuery: String?,
        orderBy: String?,
        paramsArrayOrMap: Any?,
    ) = PanacheQueryImpl<Any>(session, query, originalQuery, orderBy, paramsArrayOrMap)

    override fun list(query: PanacheQueryImpl<*>): Uni<MutableList<*>> =
        query.list() as Uni<MutableList<*>>

    companion object {
        /**
         * Provides the default implementations for quarkus to wire up. Should not be used by third
         * party developers.
         */
        @JvmField val INSTANCE = KotlinJpaOperations()
    }
}
