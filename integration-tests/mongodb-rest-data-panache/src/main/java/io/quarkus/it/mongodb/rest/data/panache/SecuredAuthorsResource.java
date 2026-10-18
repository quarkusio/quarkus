package io.quarkus.it.mongodb.rest.data.panache;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.rest.data.panache.PanacheMongoEntityResource;
import io.quarkus.security.PermissionsAllowed;

public interface SecuredAuthorsResource extends PanacheMongoEntityResource<Author, ObjectId> {

    @PermissionsAllowed("get-author")
    Author get(ObjectId id);

}
