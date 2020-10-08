package io.quarkus.it.mongodb.pojo;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

@Path("/pojos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PojoResource {
    @Inject
    MongoClient client;

    private MongoCollection<Pojo> collection;

    @PostConstruct
    public void init() {
        MongoDatabase database = client.getDatabase("books");
        collection = database.getCollection("pojo", Pojo.class);

    }

    @GET
    public List<Pojo> getPojos() {
        FindIterable<Pojo> iterable = collection.find();
        List<Pojo> pojos = new ArrayList<>();
        for (Pojo doc : iterable) {
            pojos.add(doc);
        }
        return pojos;
    }

    @POST
    public Response addPojo(Pojo pojo) throws UnsupportedEncodingException {
        collection.insertOne(pojo);
        return Response
                .created(URI.create("/pojos/" + URLEncoder.encode(pojo.id.toString(), StandardCharsets.UTF_8.toString())))
                .build();
    }
}
