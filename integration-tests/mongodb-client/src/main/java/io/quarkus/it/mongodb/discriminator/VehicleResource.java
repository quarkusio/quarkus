package io.quarkus.it.mongodb.discriminator;

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

@Path("/vehicles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VehicleResource {
    @Inject
    MongoClient client;

    private MongoCollection<Vehicle> collection;

    @PostConstruct
    public void init() {
        MongoDatabase database = client.getDatabase("books");
        collection = database.getCollection("vehicle", Vehicle.class);

    }

    @GET
    public List<Vehicle> getVehicles() {
        FindIterable<Vehicle> iterable = collection.find();
        List<Vehicle> vehicles = new ArrayList<>();
        for (Vehicle doc : iterable) {
            vehicles.add(doc);
        }
        return vehicles;
    }

    @POST
    public Response addVehicle(Vehicle vehicle) throws UnsupportedEncodingException {
        collection.insertOne(vehicle);
        return Response.created(URI.create("/vehicle/" + URLEncoder.encode(vehicle.name, StandardCharsets.UTF_8.toString())))
                .build();
    }
}
