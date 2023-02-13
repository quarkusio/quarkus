package io.quarkus.it.panache.kotlin

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
open class AddressDao(private val dummyService: DummyService) : PanacheRepositoryBase<Address, Int> {
    companion object {
        fun shouldBeOverridden(): Nothing {
            throw UnsupportedOperationException("this should be called and not be overwritten by the quarkus plugin")
        }
    }

    override fun count(query: String, params: Map<String, Any>): Long = shouldBeOverridden()
}
