package io.quarkus.mongodb.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import com.mongodb.client.MongoClient;

import io.quarkus.mongodb.ReactiveMongoClient;

@ApplicationScoped
public class MongoClientProducer {

    private MongoClient client;
    private ReactiveMongoClient reactiveMongoClient;

    public void onStop(@Observes @BeforeDestroyed(ApplicationScoped.class) Object event) {
        this.client.close();
        this.reactiveMongoClient.close();
    }

    @Singleton
    @Produces
    public MongoClient client() {
        return this.client;
    }

    @Singleton
    @Produces
    public ReactiveMongoClient axle() {
        return this.reactiveMongoClient;
    }

    public void initialize(MongoClient client, ReactiveMongoClient reactiveMongoClient) {
        this.client = client;
        this.reactiveMongoClient = reactiveMongoClient;
    }
}
