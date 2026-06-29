package io.quarkus.hibernate.reactive.panache.kotlin.runtime

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractManagedJpaOperations
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.Uni
import org.hibernate.reactive.mutiny.Mutiny

class KotlinJpaOperations : AbstractManagedJpaOperations<PanacheQueryImpl<*>>() {
    override fun createPanacheQuery(
        session: Uni<Mutiny.Session>,
        entityClass: Class<*>,
        query: String,
        originalQuery: String?,
        sort: Sort?,
        paramsArrayOrMap: Any?,
    ) = PanacheQueryImpl<Any>(session, entityClass, query, originalQuery, sort, paramsArrayOrMap)

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
