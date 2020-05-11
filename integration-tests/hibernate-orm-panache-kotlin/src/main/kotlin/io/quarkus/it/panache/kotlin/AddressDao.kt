package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
open class AddressDao : PanacheRepository<Address> {
    companion object {
        fun shouldBeOverridden(): Nothing {
            throw UnsupportedOperationException("this should be called and not be overwritten by the quarkus plugin")
        }
    }

    override fun count(query: String, params: Map<String, Any>): Long = shouldBeOverridden()
}