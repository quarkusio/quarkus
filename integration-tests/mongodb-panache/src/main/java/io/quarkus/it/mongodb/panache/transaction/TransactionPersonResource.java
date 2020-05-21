package io.quarkus.it.mongodb.panache.transaction;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.client.MongoClient;

@Path("/transaction/imperative")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionPersonResource {
    @Inject
    MongoClient mongoClient;

    @PostConstruct
    void initDb() {
        // in case of transaction, the collection needs to exist prior to using it
        if (!mongoClient.getDatabase("transaction-person").listCollectionNames().into(new ArrayList<>())
                .contains("TransactionPerson")) {
            mongoClient.getDatabase("transaction-person").createCollection("TransactionPerson");
        }
    }

    @GET
    public List<TransactionPerson> getPersons() {
        return TransactionPerson.listAll();
    }

    @POST
    @Transactional
    public Response addPerson(TransactionPerson person) {
        person.persist();
        return Response.created(URI.create("/transaction/imperative/" + person.id.toString())).build();
    }

    @POST
    @Path("/twice")
    @Transactional
    public void addPersonTwice(TransactionPerson person) {
        person.persist();
        person.persist();//this should throw an exception, and the first person should not have been created
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
    public TransactionPerson getPerson(@PathParam("id") String id) {
        return TransactionPerson.findById(Long.parseLong(id));
    }

    @GET
    @Path("/count")
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
