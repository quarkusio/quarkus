package io.quarkus.it.mongodb.panache.reactive.transaction;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.mongodb.client.MongoClient;

import io.quarkus.mongodb.panache.common.reactive.Panache;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;

@Path("/reactive-transaction")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionPersonResource {
    @Inject
    @Named("cl2")
    MongoClient mongoClient;

    void initDb(@Observes StartupEvent startupEvent) {
        // in case of transaction, the collection needs to exist prior to using it
        if (!mongoClient.getDatabase("transaction-person").listCollectionNames().into(new ArrayList<>())
                .contains("ReactiveTransactionPerson")) {
            mongoClient.getDatabase("transaction-person").createCollection("ReactiveTransactionPerson");
        }
    }

    @GET
    public Uni<List<ReactivePanacheMongoEntityBase>> getPersons() {
        return Panache.withTransaction(() -> ReactiveTransactionPerson.listAll());
    }

    @POST
    public Uni<Response> addPerson(ReactiveTransactionPerson person) {
        return Panache.withTransaction(() -> person.persist().map(v -> {
            //the ID is populated before sending it to the database
            String id = person.id.toString();
            return Response.created(URI.create("/reactive-transaction/" + id)).build();
        }));
    }

    @POST
    @Path("/exception")
    public Uni<ReactivePanacheMongoEntityBase> addPersonTwice(ReactiveTransactionPerson person) {
        return Panache.withTransaction(() -> person.persist().call(p -> {
            throw new RuntimeException("You shall not pass");
        }));
    }

    @PUT
    public Uni<Response> updatePerson(ReactiveTransactionPerson person) {
        return Panache.withTransaction(() -> person.update().map(p -> Response.accepted().build()));
    }

    @DELETE
    @Path("/{id}")
    public Uni<Response> deletePerson(@PathParam("id") String id) {
        return Panache.withTransaction(() -> ReactiveTransactionPerson.findById(Long.parseLong(id))
                .flatMap(p -> p.delete()))
                .map(ignore -> Response.noContent().build());
    }

    @GET
    @Path("/{id}")
    public Uni<ReactivePanacheMongoEntityBase> getPerson(@PathParam("id") String id) {
        return Panache.withTransaction(() -> ReactiveTransactionPerson.findById(Long.parseLong(id)));
    }

    @GET
    @Path("/count")
    public Uni<Long> countAll() {
        return Panache.withTransaction(() -> ReactiveTransactionPerson.count());
    }

    @DELETE
    public Uni<Long> deleteAll() {
        return Panache.withTransaction(() -> ReactiveTransactionPerson.deleteAll());
    }

    @POST
    @Path("/rename")
    public Uni<Response> rename(@QueryParam("previousName") String previousName, @QueryParam("newName") String newName) {
        return Panache
                .withTransaction(() -> ReactiveTransactionPerson.update("lastname", newName).where("lastname", previousName)
                        .map(l -> Response.ok().build()));
    }
}
