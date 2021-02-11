package io.quarkus.it.mongodb.panache.bugs;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/bugs")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class BugResource {

    @Inject
    Bug5274EntityRepository bug5274EntityRepository;

    @GET
    @Path("5274")
    public String testBug5274() {
        bug5274EntityRepository.count();
        return "OK";
    }

    @Inject
    Bug5885EntityRepository bug5885EntityRepository;

    @GET
    @Path("5885")
    public String testBug5885() {
        bug5885EntityRepository.findById(1L);
        return "OK";
    }

    @Inject
    Bug6324Repository bug6324Repository;

    @GET
    @Path("6324")
    public Response testNeedReflection() {
        return Response.ok(bug6324Repository.listAll()).build();
    }

    @Inject
    Bug6324ConcreteRepository bug6324ConcreteRepository;

    @GET
    @Path("6324/abstract")
    public Response testNeedReflectionAndAbstract() {
        return Response.ok(bug6324ConcreteRepository.listAll()).build();
    }

    @GET
    @Path("dates")
    public Response testDatesFormat() {
        DateEntity dateEntity = new DateEntity();
        dateEntity.persist();

        // search on all possible fields
        long millisInDay = 1000 * 60 * 60 * 24;
        Date dateTomorrow = new Date(System.currentTimeMillis() + 1000 * millisInDay);
        LocalDate localDateTomorrow = LocalDate.now().plus(1, ChronoUnit.DAYS);
        LocalDateTime localDateTimeTomorrow = LocalDateTime.now().plus(1, ChronoUnit.DAYS);
        Instant instantTomorrow = Instant.now().plus(1, ChronoUnit.DAYS);
        DateEntity result = DateEntity
                .find("dateDate < ?1 and localDate < ?2 and localDateTime < ?3 and instant < ?4",
                        dateTomorrow, localDateTomorrow, localDateTimeTomorrow, instantTomorrow)
                .firstResult();

        if (result == null) {
            return Response.status(404).build();
        }
        return Response.ok().build();
    }

    @GET
    @Path("7415")
    public Response testForeignObjectId() {
        LinkedEntity link = new LinkedEntity();
        link.name = "toto";
        link.persist();

        LinkedEntity entity = new LinkedEntity();
        entity.name = "tata";
        entity.myForeignId = link.id;
        entity.persist();

        // we should be able to retrieve `entity` from the foreignId ...
        LinkedEntity.find("myForeignId", link.id).firstResultOptional().orElseThrow(() -> new NotFoundException());
        return Response.ok().build();
    }
}
