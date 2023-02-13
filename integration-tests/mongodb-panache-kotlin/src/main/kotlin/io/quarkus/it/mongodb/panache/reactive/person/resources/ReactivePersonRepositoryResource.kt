package io.quarkus.it.mongodb.panache.reactive.person.resources

import com.mongodb.ReadPreference
import io.quarkus.it.mongodb.panache.person.Person
import io.quarkus.it.mongodb.panache.person.PersonName
import io.quarkus.it.mongodb.panache.reactive.person.ReactivePersonRepository
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
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
            .withReadPreference(ReadPreference.primaryPreferred())
            .list()
            .await()
            .indefinitely()
        lastnames.forEach { p -> uniqueNames.add(p) } // this will throw if it's not the right type
        return uniqueNames
    }

    @POST
    fun addPerson(person: Person): Uni<Response> {
        return reactivePersonRepository.persist(person).map {
            // the ID is populated before sending it to the database
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
        reactivePersonRepository.persistOrUpdate(person).map { Response.accepted().build() }

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
