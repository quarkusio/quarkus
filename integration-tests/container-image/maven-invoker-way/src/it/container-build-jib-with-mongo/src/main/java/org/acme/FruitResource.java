package org.acme;

import io.smallrye.mutiny.Uni;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.List;

import org.bson.Document;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;

@Path("fruits")
public class FruitResource {

    private final ReactiveMongoClient mongoClient;

    public FruitResource(ReactiveMongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @GET
    public Uni<List<Fruit>> list() {
        return getCollection().find()
                .map(doc -> {
                    Fruit fruit = new Fruit();
                    fruit.setName(doc.getString("name"));
                    fruit.setDescription(doc.getString("description"));
                    return fruit;
                }).collect().asList();
    }

    private ReactiveMongoCollection<Document> getCollection() {
        return mongoClient.getDatabase("fruit").getCollection("fruit");
    }
}
