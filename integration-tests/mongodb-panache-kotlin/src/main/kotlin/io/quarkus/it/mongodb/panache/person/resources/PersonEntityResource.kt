package io.quarkus.it.mongodb.panache.person.resources

import com.mongodb.ReadPreference
import io.quarkus.it.mongodb.panache.person.PersonEntity
import io.quarkus.it.mongodb.panache.person.PersonName
import io.quarkus.it.mongodb.panache.person.Status
import io.quarkus.panache.common.Sort
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PATCH
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import java.net.URI

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
            .withReadPreference(ReadPreference.primaryPreferred())
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
