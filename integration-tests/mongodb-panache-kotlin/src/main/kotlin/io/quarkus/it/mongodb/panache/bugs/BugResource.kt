package io.quarkus.it.mongodb.panache.bugs

import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Date

@Path("/bugs")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
class BugResource {
    @Inject
    lateinit var bug5274EntityRepository: Bug5274EntityRepository

    @Inject
    lateinit var bug5885EntityRepository: Bug5885EntityRepository

    @Inject
    lateinit var bug6324Repository: Bug6324Repository

    @Inject
    lateinit var bug6324ConcreteRepository: Bug6324ConcreteRepository

    @GET
    @Path("5274")
    fun testBug5274(): String {
        bug5274EntityRepository.count()
        return "OK"
    }

    @GET
    @Path("5885")
    fun testBug5885(): String {
        bug5885EntityRepository.findById(1L)
        return "OK"
    }

    @GET
    @Path("6324")
    fun testNeedReflection(): Response {
        return Response.ok(bug6324Repository.listAll()).build()
    }

    @GET
    @Path("6324/abstract")
    fun testNeedReflectionAndAbstract(): Response {
        return Response.ok(bug6324ConcreteRepository.listAll()).build()
    }

    @GET
    @Path("dates")
    fun testDatesFormat(): Response {
        val dateEntity = DateEntity()
        dateEntity.persist()

        // search on all possible fields
        val millisInDay = 1000 * 60 * 60 * 24.toLong()
        val dateTomorrow = Date(System.currentTimeMillis() + 1000 * millisInDay)
        val localDateTomorrow: LocalDate = LocalDate.now().plus(1, ChronoUnit.DAYS)
        val localDateTimeTomorrow: LocalDateTime = LocalDateTime.now().plus(1, ChronoUnit.DAYS)
        val instantTomorrow: Instant = Instant.now().plus(1, ChronoUnit.DAYS)
        DateEntity
            .find(
                "dateDate < ?1 and localDate < ?2 and localDateTime < ?3 and instant < ?4",
                dateTomorrow,
                localDateTomorrow,
                localDateTimeTomorrow,
                instantTomorrow
            )
            .firstResult()
            ?: return Response.status(404).build()
        return Response.ok().build()
    }

    @GET
    @Path("7415")
    fun testForeignObjectId(): Response {
        val link = LinkedEntity()
        link.name = "toto"
        link.persist()
        val entity = LinkedEntity()
        entity.name = "tata"
        entity.myForeignId = link.id
        entity.persist()

        // we should be able to retrieve `entity` from the foreignId ...
        LinkedEntity.find("myForeignId", link.id!!).firstResult() ?: throw NotFoundException()
        return Response.ok().build()
    }
}
