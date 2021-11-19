package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheRepositoryBase
import io.smallrye.mutiny.Uni
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
open class AddressDao : PanacheRepositoryBase<Address, Int> {
    companion object {
        fun shouldBeOverridden(): Nothing {
            throw UnsupportedOperationException("this should be called and not be overwritten by the quarkus plugin")
        }
    }

    override fun count(query: String, params: Map<String, Any>): Uni<Long> = shouldBeOverridden()
}