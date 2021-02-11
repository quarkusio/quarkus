package io.quarkus.it.mongodb.panache.bugs;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class Bug6324Repository implements PanacheMongoRepository<NeedReflection> {
}
