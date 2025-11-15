package io.quarkus.hibernate.orm.panache.kotlin.runtime

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations
import org.hibernate.Session

class KotlinJpaOperations : AbstractJpaOperations<PanacheQueryImpl<*>>() {
    override fun createPanacheQuery(
        session: Session,
        hqlQuery: String,
        originalQuery: String?,
        orderBy: String?,
        paramsArrayOrMap: Any?,
    ) = PanacheQueryImpl<Any>(session, hqlQuery, originalQuery, orderBy, paramsArrayOrMap)

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
