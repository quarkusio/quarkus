package io.quarkus.hibernate.orm.panache.kotlin.runtime

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations
import javax.persistence.EntityManager

class KotlinJpaOperations : AbstractJpaOperations<PanacheQueryImpl<*>>() {
    override fun createPanacheQuery(em: EntityManager, query: String, orderBy: String?, paramsArrayOrMap: Any?) =
        PanacheQueryImpl<Any>(em, query, orderBy, paramsArrayOrMap)

    override fun list(query: PanacheQueryImpl<*>) = query.list()

    override fun stream(query: PanacheQueryImpl<*>) = query.stream()

    companion object {
        /**
         * Provides the default implementations for quarkus to wire up. Should not be used by third party developers.
         */
        @JvmField
        val INSTANCE = KotlinJpaOperations()
    }
}
