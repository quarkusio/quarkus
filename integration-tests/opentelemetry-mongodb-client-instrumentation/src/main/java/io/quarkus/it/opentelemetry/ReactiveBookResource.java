package io.quarkus.it.opentelemetry;

import static com.mongodb.client.model.Filters.eq;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.Document;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Uni;

@Path("/reactive-books")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReactiveBookResource {

    @Inject
    ReactiveMongoClient client;

    private ReactiveMongoCollection<Book> getCollection() {
        return client.getDatabase("books").getCollection("my-reactive-collection", Book.class);
    }

    @DELETE
    public CompletableFuture<Response> clearCollection() {
        return getCollection()
                .deleteMany(new Document())
                .onItem().transform(x -> Response.ok().build())
                .subscribeAsCompletionStage();
    }

    @GET
    public CompletionStage<List<Book>> getBooks() {
        return getCollection().find().collect().asList().subscribeAsCompletionStage();
    }

    @GET
    @Path("/invalid")
    public CompletionStage<List<Book>> getBooksError() {
        BsonDocument query = new BsonDocument();
        query.put("$invalidop", new BsonDouble(0d));
        return getCollection().find(query).collect().asList().subscribeAsCompletionStage();
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

    @GET
    @Path("/multiple-chain")
    public Uni<Long> helloMultipleUsingChain() {
        return getCountDocuments("Victor Hugo")
                .chain(count1 -> getCountDocuments("Charles Baudelaire").map(count2 -> count1 + count2));
    }

    @GET
    @Path("/multiple-combine")
    public Uni<Long> helloMultipleUsingCombine() {
        return Uni.combine().all().unis(
                getCountDocuments("Victor Hugo"),
                getCountDocuments("Charles Baudelaire"))
                .with(Long::sum);
    }

    private Uni<Long> getCountDocuments(String author) {
        return getCollection().countDocuments(eq("author", author));
    }

}
