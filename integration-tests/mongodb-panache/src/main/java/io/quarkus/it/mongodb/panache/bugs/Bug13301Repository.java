package io.quarkus.it.mongodb.panache.bugs;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class Bug13301Repository implements PanacheMongoRepository<NeedReflectionChild> {
}
