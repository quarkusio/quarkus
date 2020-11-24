package io.quarkus.it.mongodb.panache.person

import io.quarkus.panache.common.Sort
import java.net.URI
import javax.inject.Inject
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.PATCH
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

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