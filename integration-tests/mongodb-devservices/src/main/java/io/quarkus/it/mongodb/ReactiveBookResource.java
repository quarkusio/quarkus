package io.quarkus.it.mongodb;

import static com.mongodb.client.model.Filters.eq;

import java.util.List;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;

@Path("/reactive-books")
public class ReactiveBookResource {

    @Inject
    ReactiveMongoClient client;

    private ReactiveMongoCollection<Book> getCollection() {
        return client.getDatabase("books").getCollection("my-reactive-collection", Book.class);
    }

    @GET
    public CompletionStage<List<Book>> getBooks() {
        return getCollection().find().collect().asList().subscribeAsCompletionStage();
    }

    @POST
    public CompletionStage<Response> addBook(Book book) {
        return getCollection().insertOne(book)
                .onItem().transform(x -> Response.accepted().build())
                .subscribeAsCompletionStage();
    }

    @GET
    @Path("/{author}")
    public CompletionStage<List<Book>> getBooksByAuthor(@PathParam("author") String author) {
        return getCollection().find(eq("author", author))
                .collect().asList()
                .subscribeAsCompletionStage();
    }

}
