package io.quarkus.mongo.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.mongodb.client.MongoClient;

@ApplicationScoped
public class MongoClientProducer {

    private MongoClient client;
    private io.vertx.ext.mongo.MongoClient vertxMongoClient;
    private io.vertx.axle.ext.mongo.MongoClient axleMongoClient;

    void initialize(MongoClient client, io.vertx.ext.mongo.MongoClient vertxMongoClient) {
        this.client = client;
        this.vertxMongoClient = vertxMongoClient;
        //        this.axleMongoClient = io.vertx.axle.ext.mongo.MongoClient.newInstance(this.vertxMongoClient);
    }

    @Singleton
    @Produces
    public MongoClient client() {
        System.out.println("Returning the client " + client);
        return client;
    }

    // TODO Give access to the Bare Vert.x, Axle and RX Java 2 clients

    // TODO Configuration for the main client and Vert.x client

    // TODO How do we deal with the amount of JSON Object used in the Vert.x one.

}
