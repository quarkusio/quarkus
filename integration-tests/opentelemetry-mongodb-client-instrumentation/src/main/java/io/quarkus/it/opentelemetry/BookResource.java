package io.quarkus.it.opentelemetry;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.Document;

import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import io.smallrye.common.annotation.Blocking;

@Path("/books")
@Blocking
public class BookResource {

    @Inject
    MongoClient client;

    private MongoCollection<Book> getCollection() {
        return client.getDatabase("books").getCollection("my-collection", Book.class);
    }

    @DELETE
    public Response clearBooks() {
        getCollection().deleteMany(new Document());
        return Response.ok().build();
    }

    @GET
    public List<Book> getBooks() {
        FindIterable<Book> iterable = getCollection().find();
        return getBooks(iterable);
    }

    private List<Book> getBooks(FindIterable<Book> iterable) {
        List<Book> books = new ArrayList<>();
        WriteConcern writeConcern = client.getDatabase("temp").getWriteConcern();
        // force a test failure if we're not getting the correct, and correctly configured named mongodb client
        if (Boolean.TRUE.equals(writeConcern.getJournal())) {
            for (Book doc : iterable) {
                books.add(doc);
            }
        }
        return books;
    }

    @GET
    @Path("/invalid")
    public List<Book> error() {
        BsonDocument query = new BsonDocument();
        query.put("$invalidop", new BsonDouble(0d));
        FindIterable<Book> iterable = getCollection().find(query);
        return getBooks(iterable);
    }

    @POST
    public Response addBook(Book book) {
        getCollection().insertOne(book);
        return Response.accepted().build();
    }

    @GET
    @Path("/{author}")
    public List<Book> getBooksByAuthor(@PathParam("author") String author) {
        FindIterable<Book> iterable = getCollection().find(eq("author", author));
        List<Book> books = new ArrayList<>();
        for (Book doc : iterable) {
            String title = doc.title();
            books.add(new Book(author, title));
        }
        return books;
    }

}
