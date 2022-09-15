package io.quarkus.it.mongodb.panache.bugs;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

@Path("/bugs")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class BugResource {

    @Inject
    Bug5274EntityRepository bug5274EntityRepository;

    @Inject
    Bug13301Repository bug13301Repository;

    @Inject
    @Named("cl2")
    MongoClient mongoClient;

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

    @GET
    @Path("13301")
    public Response testReflectiveHierarchy() {
        NeedReflectionChild me = new NeedReflectionChild();
        me.parent = "François";
        me.child = "Loïc";
        bug13301Repository.persist(me);

        Optional<NeedReflectionChild> result = bug13301Repository.find("parent", "François").firstResultOptional();
        return result.isPresent() ? Response.ok().build() : Response.serverError().build();
    }

    @GET
    @Path("23813")
    public Response testDatabaseAndCollectionFromAnnotation() {
        Bug23813ImperativeEntity bug23813ImperativeEntity = new Bug23813ImperativeEntity();
        bug23813ImperativeEntity.field = "field";
        bug23813ImperativeEntity.persist();
        MongoCollection<Bug23813ImperativeEntity> imperativeCollection = mongoClient.getDatabase("Bug23813ImperativeEntity")
                .getCollection("TheBug23813ImperativeEntity", Bug23813ImperativeEntity.class);
        Bug23813ImperativeEntity findBug23813imperativeEntity = imperativeCollection
                .find(new Document("_id", bug23813ImperativeEntity.id)).first();
        if (findBug23813imperativeEntity == null) {
            return Response.status(404).build();
        }

        Bug23813ReactiveEntity bug23813ReactiveEntity = new Bug23813ReactiveEntity();
        bug23813ReactiveEntity.field = "field";
        bug23813ReactiveEntity.persist().await().indefinitely();
        MongoCollection<Bug23813ReactiveEntity> reactiveCollection = mongoClient.getDatabase("Bug23813ReactiveEntity")
                .getCollection("TheBug23813ReactiveEntity", Bug23813ReactiveEntity.class);
        Bug23813ReactiveEntity findBug23813ReactiveEntity = reactiveCollection
                .find(new Document("_id", bug23813ReactiveEntity.id)).first();
        if (findBug23813ReactiveEntity == null) {
            return Response.status(404).build();
        }

        return Response.ok().build();
    }
}
