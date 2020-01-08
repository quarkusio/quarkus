package io.quarkus.it.mongodb;

import static com.mongodb.client.model.Filters.eq;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.mongodb.mutiny.ReactiveMongoClient;
import io.quarkus.mongodb.mutiny.ReactiveMongoCollection;
import io.smallrye.mutiny.Uni;

@Path("/reactive-books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReactiveBookResource {

    @Inject
    ReactiveMongoClient client;

    private ReactiveMongoCollection<Book> collection;

    @PostConstruct
    public void init() {
        collection = client.getDatabase("books").getCollection("my-reactive-collection", Book.class);
    }

    @GET
    public Uni<List<Book>> getBooks() {
        return collection.find().collectItems().asList();
    }

    @POST
    public Uni<Response> addBook(Book book) {
        return collection.insertOne(book)
                .onItem().apply(x -> Response.accepted().build());
    }

    @GET
    @Path("/{author}")
    public Uni<List<Book>> getBooksByAuthor(@PathParam("author") String author) {
        return collection.find(eq("author", author))
                .collectItems().asList();
    }

}
