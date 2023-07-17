package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.Panache
import io.smallrye.mutiny.Uni
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path

/**
 * Run a simple, no paged PanacheQueryTest in order to log the generated SQL.
 *
 * @see io.quarkus.it.panache.reactive.kotlin.NoPagingPMT
 */
@Path("no-paging-test")
class NoPagingTestEndpoint {
    @GET
    fun test(): Uni<String> {
        return Panache.withTransaction { PageItem.findAll().list().map { v -> "OK" } }
    }
}
