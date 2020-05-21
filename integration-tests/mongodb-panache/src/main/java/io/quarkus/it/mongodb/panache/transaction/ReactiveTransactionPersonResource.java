package io.quarkus.it.mongodb.panache.transaction;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.client.MongoClient;

import io.smallrye.mutiny.Uni;

@Path("/transaction/reactive")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReactiveTransactionPersonResource {
    @Inject
    MongoClient mongoClient;

    @PostConstruct
    void initDb() {
        // in case of transaction, the collection needs to exist prior to using it
        if (!mongoClient.getDatabase("transaction-person").listCollectionNames().into(new ArrayList<>())
                .contains("ReactiveTransactionPerson")) {
            mongoClient.getDatabase("transaction-person").createCollection("ReactiveTransactionPerson");
        }
    }

    @GET
    public Uni<List<ReactiveTransactionPerson>> getPersons() {
        return ReactiveTransactionPerson.listAll();
    }

    @POST
    @Transactional
    public Uni<Response> addPerson(ReactiveTransactionPerson person) {
        return person.persist().map(v -> {
            //the ID is populated before sending it to the database
            String id = person.id.toString();
            return Response.created(URI.create("/persons/entity" + id)).build();
        });
    }

    @POST
    @Path("/twice")
    @Transactional
    public Uni<Void> addPersonTwice(ReactiveTransactionPerson person) {
        //this should throw an exception, and the first person should not have been created
        return Uni.combine().all().unis(person.persist(), person.persist()).combinedWith((v1, v2) -> null);
    }

    @PUT
    @Transactional
    public Uni<Response> updatePerson(ReactiveTransactionPerson person) {
        return person.update().map(v -> Response.accepted().build());
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Uni<Void> deletePerson(@PathParam("id") String id) {
        return ReactiveTransactionPerson.findById(Long.parseLong(id)).flatMap(person -> person.delete());
    }

    @GET
    @Path("/{id}")
    public Uni<ReactiveTransactionPerson> getPerson(@PathParam("id") String id) {
        return ReactiveTransactionPerson.findById(Long.parseLong(id));
    }

    @GET
    @Path("/count")
    public Uni<Long> countAll() {
        return ReactiveTransactionPerson.count();
    }

    @DELETE
    @Transactional
    public Uni<Void> deleteAll() {
        return ReactiveTransactionPerson.deleteAll().map(r -> null);
    }

    @POST
    @Path("/rename")
    @Transactional
    public Uni<Response> rename(@QueryParam("previousName") String previousName, @QueryParam("newName") String newName) {
        return ReactiveTransactionPerson.update("lastname", newName)
                .where("lastname", previousName)
                .map(v -> Response.ok().build());
    }
}
