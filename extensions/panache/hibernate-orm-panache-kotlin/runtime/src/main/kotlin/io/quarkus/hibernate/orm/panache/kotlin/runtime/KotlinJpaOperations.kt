package io.quarkus.hibernate.orm.panache.kotlin.runtime

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractManagedJpaOperations
import io.quarkus.panache.common.Sort
import org.hibernate.Session

class KotlinJpaOperations : AbstractManagedJpaOperations<PanacheQueryImpl<*>>() {
    override fun createPanacheQuery(
        session: Session,
        entityClass: Class<*>,
        hqlQuery: String,
        originalQuery: String?,
        sort: Sort?,
        paramsArrayOrMap: Any?,
    ) = PanacheQueryImpl<Any>(session, entityClass, hqlQuery, originalQuery, sort, paramsArrayOrMap)

    override fun list(query: PanacheQueryImpl<*>) = query.list()

    override fun stream(query: PanacheQueryImpl<*>) = query.stream()

    companion object {
        /**
         * Provides the default implementations for quarkus to wire up. Should not be used by third
         * party developers.
         */
        @JvmField val INSTANCE = KotlinJpaOperations()
    }
}
