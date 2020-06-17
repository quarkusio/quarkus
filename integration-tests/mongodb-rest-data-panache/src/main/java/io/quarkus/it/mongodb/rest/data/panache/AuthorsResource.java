package io.quarkus.it.mongodb.rest.data.panache;

import javax.ws.rs.core.Response;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.rest.data.panache.PanacheMongoEntityResource;
import io.quarkus.rest.data.panache.MethodProperties;

public interface AuthorsResource extends PanacheMongoEntityResource<Author, ObjectId> {

    @MethodProperties(exposed = false)
    Response add(Author entity);

    @MethodProperties(exposed = false)
    Response update(ObjectId id, Author entity);

    @MethodProperties(exposed = false)
    void delete(ObjectId id);
}
