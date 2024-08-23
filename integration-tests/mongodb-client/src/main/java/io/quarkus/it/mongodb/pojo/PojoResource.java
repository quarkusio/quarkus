package io.quarkus.it.mongodb.pojo;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import io.smallrye.common.annotation.Blocking;

@Path("/pojos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class PojoResource {
    @Inject
    MongoClient client;

    private MongoCollection<Pojo> getCollection() {
        return client.getDatabase("books").getCollection("pojo", Pojo.class);
    }

    @DELETE
    public Response clearCollection() {
        getCollection().deleteMany(new Document());
        return Response.ok().build();
    }

    @GET
    public List<Pojo> getPojos() {
        FindIterable<Pojo> iterable = getCollection().find();
        List<Pojo> pojos = new ArrayList<>();
        for (Pojo doc : iterable) {
            pojos.add(doc);
        }
        return pojos;
    }

    @POST
    public Response addPojo(Pojo pojo) throws UnsupportedEncodingException {
        getCollection().insertOne(pojo);
        return Response
                .created(URI.create("/pojos/" + URLEncoder.encode(pojo.id.toString(), StandardCharsets.UTF_8.toString())))
                .build();
    }
}
