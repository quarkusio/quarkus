package io.quarkus.it.mongodb;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;

import io.quarkus.mongodb.MongoClientName;
import io.smallrye.common.annotation.Blocking;

@Path("/books")
@Blocking
public class BookResource {

    @Inject
    @MongoClientName("parameter-injection")
    MongoClient client;

    @Inject
    @MongoClientName("dev-services")
    MongoClient devServiced;
    @Inject
    @MongoClientName("dev-services2")
    MongoClient devServiced2;

    private MongoCollection<Book> getCollection() {
        return client.getDatabase("books").getCollection("my-collection", Book.class);
    }

    private MongoCollection<Book> getServicedCollection() {
        return devServiced.getDatabase("books").getCollection("my-collection", Book.class);
    }

    private MongoCollection<Book> getServiced2Collection() {
        return devServiced.getDatabase("books").getCollection("my-collection", Book.class);
    }

    @GET
    public List<Book> getBooks() {
        FindIterable<Book> iterable = getCollection().find();
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

    @POST
    public Response addBook(Book book) {
        getCollection().insertOne(book);
        getServicedCollection().insertOne(book);
        getServiced2Collection().insertOne(book);
        return Response.accepted().build();
    }

    @GET
    @Path("/{author}")
    public List<Book> getBooksByAuthor(@PathParam("author") String author) {
        getServicedCollection().find(eq("author", author));
        getServiced2Collection().find(eq("author", author));
        FindIterable<Book> iterable = getCollection().find(eq("author", author));
        List<Book> books = new ArrayList<>();
        for (Book doc : iterable) {
            String title = doc.getTitle();
            books.add(new Book().setTitle(title).setAuthor(author));
        }
        return books;
    }

}
