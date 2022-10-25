package io.quarkus.it.mongodb.panache.person.resources

import com.mongodb.ReadPreference
import io.quarkus.it.mongodb.panache.person.MockablePersonRepository
import io.quarkus.it.mongodb.panache.person.Person
import io.quarkus.it.mongodb.panache.person.PersonName
import io.quarkus.it.mongodb.panache.person.PersonRepository
import io.quarkus.it.mongodb.panache.person.Status
import io.quarkus.panache.common.Sort
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

@Path("/persons/repository")
class PersonRepositoryResource {
    // fake unused injection point to force ArC to not remove this otherwise I can't mock it in the tests
    @Inject
    lateinit var mockablePersonRepository: MockablePersonRepository

    @Inject
    lateinit var personRepository: PersonRepository

    @GET
    fun getPersons(@QueryParam("sort") sort: String?): List<Person> {
        return sort?.let {
            personRepository.listAll(Sort.ascending(sort))
        } ?: personRepository.listAll()
    }

    @GET
    @Path("/search/{name}")
    fun searchPersons(@PathParam("name") name: String): Set<PersonName> {
        return personRepository.find("lastname = ?1 and status = ?2", name, Status.ALIVE)
            .project(PersonName::class.java)
            .withReadPreference(ReadPreference.primaryPreferred())
            .list()
            .toSet()
    }

    @POST
    fun addPerson(person: Person): Response {
        personRepository.persist(person)
        return Response.created(URI.create("/persons/repository/${person.id}")).build()
    }

    @POST
    @Path("/multiple")
    fun addPersons(persons: List<Person>) {
        personRepository.persist(persons)
    }

    @PUT
    fun updatePerson(person: Person): Response {
        personRepository.update(person)
        return Response.accepted().build()
    }

    // PATCH is not correct here but it allows to test persistOrUpdate without a specific subpath
    @PATCH
    fun upsertPerson(person: Person): Response {
        personRepository.persistOrUpdate(person)
        return Response.accepted().build()
    }

    @DELETE
    @Path("/{id}")
    fun deletePerson(@PathParam("id") id: String) {
        val person = personRepository.findById(id.toLong())
        personRepository.delete(person!!)
    }

    @GET
    @Path("/{id}")
    fun getPerson(@PathParam("id") id: String) = personRepository.findById(id.toLong())

    @GET
    @Path("/count")
    fun countAll(): Long = personRepository.count()

    @DELETE
    fun deleteAll() {
        personRepository.deleteAll()
    }

    @POST
    @Path("/rename")
    fun rename(@QueryParam("previousName") previousName: String, @QueryParam("newName") newName: String): Response {
        personRepository.update("lastname", newName).where("lastname", previousName)
        return Response.ok().build()
    }
}
