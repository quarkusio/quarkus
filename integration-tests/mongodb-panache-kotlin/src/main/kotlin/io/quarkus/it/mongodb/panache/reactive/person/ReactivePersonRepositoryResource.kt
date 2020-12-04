package io.quarkus.it.mongodb.panache.reactive.person


import io.quarkus.it.mongodb.panache.person.Person
import io.quarkus.it.mongodb.panache.person.PersonName
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.Uni
import java.net.URI
import javax.inject.Inject
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.PATCH
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/reactive/persons/repository")
class ReactivePersonRepositoryResource {
    @Inject
    lateinit var reactivePersonRepository: ReactivePersonRepository

    @GET
    fun getPersons(@QueryParam("sort") sort: String?): Uni<List<Person>> {
        return if (sort != null) {
            reactivePersonRepository.listAll(Sort.ascending(sort))
        } else reactivePersonRepository.listAll()
    }

    @GET
    @Path("/search/{name}")
    fun searchPersons(@PathParam("name") name: String): Set<PersonName> {
        val uniqueNames = mutableSetOf<PersonName>()
        val lastnames: List<PersonName> = reactivePersonRepository.find("lastname", name)
                .project(PersonName::class.java)
                .list()
                .await()
                .indefinitely()
        lastnames.forEach { p -> uniqueNames.add(p) } // this will throw if it's not the right type
        return uniqueNames
    }

    @POST
    fun addPerson(person: Person): Uni<Response> {
        return reactivePersonRepository.persist(person).map {
            //the ID is populated before sending it to the database
            Response.created(URI.create("/persons/entity${person.id}")).build()
        }
    }

    @POST
    @Path("/multiple")
    fun addPersons(persons: List<Person>): Uni<Void> = reactivePersonRepository.persist(persons)

    @PUT
    fun updatePerson(person: Person): Uni<Response> =
            reactivePersonRepository.update(person).map { Response.accepted().build() }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    fun upsertPerson(person: Person): Uni<Response> =
            reactivePersonRepository.persistOrUpdate(person).map { v -> Response.accepted().build() }

    @DELETE
    @Path("/{id}")
    fun deletePerson(@PathParam("id") id: String): Uni<Void> {
        return reactivePersonRepository.findById(id.toLong())
                .flatMap { person -> person?.let { reactivePersonRepository.delete(it) } }
    }

    @GET
    @Path("/{id}")
    fun getPerson(@PathParam("id") id: String) = reactivePersonRepository.findById(id.toLong())

    @GET
    @Path("/count")
    fun countAll(): Uni<Long> = reactivePersonRepository.count()

    @DELETE
    fun deleteAll(): Uni<Void> = reactivePersonRepository.deleteAll().map { null }

    @POST
    @Path("/rename")
    fun rename(@QueryParam("previousName") previousName: String, @QueryParam("newName") newName: String): Uni<Response> {
        return reactivePersonRepository.update("lastname", newName).where("lastname", previousName)
                .map { count -> Response.ok().build() }
    }
}