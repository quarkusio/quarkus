package io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu

import io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.first.FirstEntity
import io.quarkus.hibernate.orm.panache.kotlin.deployment.test.multiple_pu.second.SecondEntity
import jakarta.transaction.Transactional
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/persistence-unit")
class PanacheTestResource {
    @GET
    @Path("/first/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    fun createWithFirstPuAndReturnCount(@PathParam("name") name: String?): String? {
        val entity = FirstEntity()
        entity.name = name
        entity.persistAndFlush()
        return name
    }

    @GET
    @Path("/second/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    fun createWithSecondPUAndReturnCount(@PathParam("name") name: String?): String? {
        val entity = SecondEntity()
        entity.name = name
        entity.persistAndFlush()
        return name
    }
}
