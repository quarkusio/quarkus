package io.quarkus.it.mongodb.panache.person

import io.quarkus.panache.common.Sort
import java.net.URI
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.PATCH
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

@Path("/persons/entity")
class PersonEntityResource {
    @GET
    fun getPersons(@QueryParam("sort") sort: String?): List<PersonEntity> =
            sort?.let { PersonEntity.listAll(Sort.ascending(sort)) }
                    ?: PersonEntity.listAll()

    @GET
    @Path("/search/{name}")
    fun searchPersons(@PathParam("name") name: String): Set<PersonName> {
        return PersonEntity.find("lastname = ?1 and status = ?2", name, Status.ALIVE)
                .project(PersonName::class.java)
                .list()
                .toSet()
    }

    @POST
    fun addPerson(person: PersonEntity): Response {
        person.persist()
        return Response.created(URI.create("/persons/entity/${person.id}")).build()
    }

    @POST
    @Path("/multiple")
    fun addPersons(persons: List<PersonEntity>) {
        PersonEntity.persist(persons)
    }

    @PUT
    fun updatePerson(person: PersonEntity): Response {
        person.update()
        return Response.accepted().build()
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    fun upsertPerson(person: PersonEntity): Response {
        person.persistOrUpdate()
        return Response.accepted().build()
    }

    @DELETE
    @Path("/{id}")
    fun deletePerson(@PathParam("id") id: String) {
        PersonEntity.deleteById(id.toLong())
    }

    @GET
    @Path("/{id}")
    fun getPerson(@PathParam("id") id: String) = PersonEntity.findById(id.toLong())

    @GET
    @Path("/count")
    fun countAll(): Long = PersonEntity.count()

    @DELETE
    fun deleteAll() {
        PersonEntity.deleteAll()
    }

    @POST
    @Path("/rename")
    fun rename(@QueryParam("previousName") previousName: String, @QueryParam("newName") newName: String): Response {
        PersonEntity.update("lastname", newName)
                .where("lastname", previousName)
        return Response.ok().build()
    }
}