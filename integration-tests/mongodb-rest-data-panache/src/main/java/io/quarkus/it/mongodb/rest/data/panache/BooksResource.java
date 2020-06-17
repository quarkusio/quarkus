package io.quarkus.it.mongodb.rest.data.panache;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.rest.data.panache.PanacheMongoRepositoryResource;
import io.quarkus.rest.data.panache.ResourceProperties;

@ResourceProperties(hal = true)
public interface BooksResource extends PanacheMongoRepositoryResource<BookRepository, Book, ObjectId> {
}
