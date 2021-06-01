package io.quarkus.it.mongodb.panache.reactive.person.resources


import com.mongodb.ReadPreference
import io.quarkus.it.mongodb.panache.person.PersonName
import io.quarkus.it.mongodb.panache.reactive.person.ReactivePersonEntity
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.Uni
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

@Path("/reactive/persons/entity")
class ReactivePersonEntityResource {
    @GET
    fun getPersons(@QueryParam("sort") sort: String?): Uni<List<ReactivePersonEntity>> {
        return if (sort != null) {
            ReactivePersonEntity.listAll(Sort.ascending(sort))
        } else ReactivePersonEntity.listAll()
    }

    @GET
    @Path("/search/{name}")
    fun searchPersons(@PathParam("name") name: String): Set<PersonName> {
        val uniqueNames = mutableSetOf<PersonName>()
        val lastnames: List<PersonName> = ReactivePersonEntity.find("lastname", name)
                .project(PersonName::class.java)
                .withReadPreference(ReadPreference.primaryPreferred())
                .list()
                .await()
                .indefinitely()
        lastnames.forEach { p -> uniqueNames.add(p) }
        return uniqueNames
    }

    @POST
    fun addPerson(person: ReactivePersonEntity): Uni<Response> {
        return person.persist<ReactivePersonEntity>()
                .map { Response.created(URI.create("/persons/entity${person.id}")).build() }
    }

    @POST
    @Path("/multiple")
    fun addPersons(persons: List<ReactivePersonEntity>): Uni<Void> = ReactivePersonEntity.persist(persons)

    @PUT
    fun updatePerson(person: ReactivePersonEntity): Uni<Response> =
            person.update<ReactivePersonEntity>().map { Response.accepted().build() }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    fun upsertPerson(person: ReactivePersonEntity): Uni<Response> =
            person.persistOrUpdate<ReactivePersonEntity>().map { Response.accepted().build() }

    @DELETE
    @Path("/{id}")
    fun deletePerson(@PathParam("id") id: String): Uni<Void> =
            ReactivePersonEntity.findById(id.toLong()).flatMap { person -> person?.delete() }

    @GET
    @Path("/{id}")
    fun getPerson(@PathParam("id") id: String): Uni<ReactivePersonEntity?> =
            ReactivePersonEntity.findById(id.toLong())

    @GET
    @Path("/count")
    fun countAll(): Uni<Long> = ReactivePersonEntity.count()

    @DELETE
    fun deleteAll(): Uni<Void> = ReactivePersonEntity.deleteAll().map { null }

    @POST
    @Path("/rename")
    fun rename(@QueryParam("previousName") previousName: String, @QueryParam("newName") newName: String): Uni<Response> {
        return ReactivePersonEntity.update("lastname", newName)
                .where("lastname", previousName)
                .map { count -> Response.ok().build() }
    }
}