package io.quarkus.it.mongodb.panache.transaction;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.mongodb.client.MongoClient;

import io.quarkus.runtime.StartupEvent;

@Path("/transaction")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionPersonResource {
    @Inject
    @Named("cl2")
    MongoClient mongoClient;

    void initDb(@Observes StartupEvent startupEvent) {
        // in case of transaction, the collection needs to exist prior to using it
        if (!mongoClient.getDatabase("transaction-person").listCollectionNames().into(new ArrayList<>())
                .contains("TransactionPerson")) {
            mongoClient.getDatabase("transaction-person").createCollection("TransactionPerson");
        }
    }

    @GET
    @Transactional
    public List<TransactionPerson> getPersons() {
        return TransactionPerson.listAll();
    }

    @POST
    @Transactional
    public Response addPerson(TransactionPerson person) {
        person.persist();
        return Response.created(URI.create("/transaction/" + person.id.toString())).build();
    }

    @POST
    @Path("/exception")
    @Transactional
    public void addPersonTwice(TransactionPerson person) {
        person.persist();
        throw new RuntimeException("You shall not pass");
    }

    @PUT
    @Transactional
    public Response updatePerson(TransactionPerson person) {
        person.update();
        return Response.accepted().build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void deletePerson(@PathParam("id") String id) {
        TransactionPerson person = TransactionPerson.findById(Long.parseLong(id));
        person.delete();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public TransactionPerson getPerson(@PathParam("id") String id) {
        return TransactionPerson.findById(Long.parseLong(id));
    }

    @GET
    @Path("/count")
    @Transactional
    public long countAll() {
        return TransactionPerson.count();
    }

    @DELETE
    @Transactional
    public void deleteAll() {
        TransactionPerson.deleteAll();
    }

    @POST
    @Path("/rename")
    @Transactional
    public Response rename(@QueryParam("previousName") String previousName, @QueryParam("newName") String newName) {
        TransactionPerson.update("lastname", newName).where("lastname", previousName);
        return Response.ok().build();
    }
}
