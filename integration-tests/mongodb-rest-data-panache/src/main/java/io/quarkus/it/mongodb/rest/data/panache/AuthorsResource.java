package io.quarkus.it.mongodb.rest.data.panache;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.rest.data.panache.PanacheMongoEntityResource;
import io.quarkus.rest.data.panache.MethodProperties;

public interface AuthorsResource extends PanacheMongoEntityResource<Author, ObjectId> {

    @MethodProperties(exposed = false)
    Author add(Author entity);

    @MethodProperties(exposed = false)
    Author update(ObjectId id, Author entity);

    @MethodProperties(exposed = false)
    boolean delete(ObjectId id);
}
