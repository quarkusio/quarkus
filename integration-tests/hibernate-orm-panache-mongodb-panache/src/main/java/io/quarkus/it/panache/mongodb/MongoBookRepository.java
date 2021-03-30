package io.quarkus.it.panache.mongodb;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class MongoBookRepository implements PanacheMongoRepository<MongoBook> {
}
